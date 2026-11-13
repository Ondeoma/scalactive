package io.github.ondeoma.scalactive.sbtplugin

import io.github.ondeoma.scalactive.compiler.{DomNames, StCompiler, StSources}
import sbt.Keys._
import sbt._
import sbt.nio.Keys._
import sbt.nio.file.{**, Glob}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

/**
 * .st(単一ファイルコンポーネント)をScalaソースへ変換するsbtプラグイン.
 *
 * {{{
 * lazy val app = project.enablePlugins(ScalaJSPlugin, ScalactivePlugin)
 * }}}
 *
 * src/main/scalactive と src/main/scala 配下の *.st を対象に、
 * ディレクトリ構成をパッケージとしたobjectを src_managed に生成します.
 * <style lang="scss"> / <style lang="sass"> は、同梱のDart SassでCSSへ変換します.
 *
 * コンパイル時には、言語サーバー(エディタでの補完・型チェック)向けに
 * クラスパスなどを target/scalactive-lsp.json に、コンパイル完了の印を target/scalactive-lsp.stamp に書き出します.
 */
object ScalactivePlugin extends AutoPlugin {

  override def trigger = noTrigger

  override def requires = plugins.JvmPlugin

  object autoImport {
    val scalactiveSourceDirectories = settingKey[Seq[File]]("Directories scanned for .st single file components")
    val scalactiveGenerate = taskKey[Seq[File]]("Generate Scala sources from .st single file components")
    val scalactiveLspConfig = taskKey[File]("Write target/scalactive-lsp.json for the Scalactive language server")
  }

  import autoImport._

  override def projectSettings: Seq[Setting[_]] = inConfig(Compile)(Seq(
    scalactiveSourceDirectories := Seq(sourceDirectory.value / "scalactive", scalaSource.value),
    scalactiveGenerate / fileInputs := scalactiveSourceDirectories.value.map(d => Glob(d.toPath, ** / "*.st")),
    scalactiveGenerate := {
      val roots = scalactiveSourceDirectories.value.map(_.toPath.toAbsolutePath.normalize)
      val inputs = scalactiveGenerate.inputFiles
      val outDir = (sourceManaged.value / "scalactive").toPath
      // dom.* を使っている場合だけimportするため、scalajs-domのjarから名前を読む(外部の依存だけで足りる)
      val classpath = externalDependencyClasspath.value.map(_.data.toPath)
      generate(roots, inputs, outDir, classpath, streams.value.log)
    },
    sourceGenerators += scalactiveGenerate.taskValue,
    scalactiveLspConfig := {
      // 自身のクラスディレクトリも含める(他の.stやScalaソースの定義を、最後にコンパイルした時点の内容で参照する)
      val classpath = dependencyClasspath.value.map(_.data) :+ classDirectory.value
      val json = LspConfig(
        scalaVersion = scalaVersion.value,
        classpath = classpath.map(_.getAbsolutePath),
        scalacOptions = scalacOptions.value,
        sourceRoots = scalactiveSourceDirectories.value.map(_.getAbsolutePath),
        sourceDirectories = (unmanagedSourceDirectories.value ++ scalactiveSourceDirectories.value).distinct.map(_.getAbsolutePath),
        generatedDirectory = (sourceManaged.value / "scalactive").getAbsolutePath
      ).toJson
      val file = target.value / "scalactive-lsp.json"
      if (!file.exists || IO.read(file) != json) IO.write(file, json)
      file
    },
    compile := {
      val analysis = compile.dependsOn(scalactiveLspConfig).value
      // コンパイルが終わったことを言語サーバーに知らせる(クラスディレクトリの内容を読み直すため)
      IO.touch(target.value / "scalactive-lsp.stamp")
      analysis
    }
  ))

  /** 言語サーバーが読む設定. 形式を変える場合は言語サーバー側(ProjectConfig)も合わせて変更します. */
  private final case class LspConfig(scalaVersion: String,
                                     classpath: Seq[String],
                                     scalacOptions: Seq[String],
                                     sourceRoots: Seq[String],
                                     sourceDirectories: Seq[String],
                                     generatedDirectory: String) {

    def toJson: String = {
      def str(s: String) = "\"" + s.flatMap {
        case '"' => "\\\""
        case '\\' => "\\\\"
        case c if c < ' ' => "\\u%04x".format(c.toInt)
        case c => c.toString
      } + "\""
      def arr(xs: Seq[String]) = xs.map(x => "\n    " + str(x)).mkString("[", ",", "\n  ]")
      s"""{
         |  "version": 1,
         |  "scalaVersion": ${str(scalaVersion)},
         |  "classpath": ${arr(classpath)},
         |  "scalacOptions": ${arr(scalacOptions)},
         |  "sourceRoots": ${arr(sourceRoots)},
         |  "sourceDirectories": ${arr(sourceDirectories)},
         |  "generatedDirectory": ${str(generatedDirectory)}
         |}
         |""".stripMargin
    }

  }

  private def generate(roots: Seq[Path], inputs: Seq[Path], outDir: Path, classpath: Seq[Path], log: Logger): Seq[File] = {
    val sass = new SassStyles(roots.map(_.toFile))
    val domNames = DomNames.fromClasspath(classpath)
    val results = try {
      inputs.sortBy(_.toString).map { file =>
        val abs = file.toAbsolutePath.normalize
        val root = StSources.rootOf(roots, abs).getOrElse(sys.error(s"$abs is not under scalactiveSourceDirectories"))
        val in = StSources.input(root, abs, new String(Files.readAllBytes(abs), StandardCharsets.UTF_8))
          .copy(styleCompiler = Some(sass.forFile(abs)), domNames = domNames)
        StCompiler.compile(in).left.map(e => s"$abs:${e.line}: ${e.message}")
      }
    } finally sass.close()

    val errors = results.collect { case Left(e) => e }
    if (errors.nonEmpty) {
      errors.foreach(e => log.error(e))
      throw new MessageOnlyException(s"${errors.size} error(s) in .st files")
    }

    val written = results.collect { case Right(out) =>
      val target = StSources.outputFile(outDir, out)
      val bytes = out.code.getBytes(StandardCharsets.UTF_8)
      // 内容が変わらない場合は書き込まず、不要な再コンパイルを避ける
      if (!Files.exists(target) || !java.util.Arrays.equals(Files.readAllBytes(target), bytes)) {
        Files.createDirectories(target.getParent)
        Files.write(target, bytes)
      }
      target.toFile
    }

    // 削除・改名された.stに対応する古い生成ファイルを掃除
    if (Files.exists(outDir)) {
      val keep = written.map(_.getAbsoluteFile).toSet
      (outDir.toFile ** "*.scala").get.filterNot(f => keep.contains(f.getAbsoluteFile)).foreach(IO.delete)
    }
    written
  }

}
