package io.github.ondeoma.scalactive.ls

import com.google.gson.JsonParser

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}
import scala.collection.JavaConverters._

/**
 * sbtプラグインが書き出す target/scalactive-lsp.json の内容.
 *
 * @param file              設定ファイル自身のパス
 * @param sourceRoots       .stを探すディレクトリ(パッケージの起点)
 * @param sourceDirectories 定義ジャンプで探すソースディレクトリ
 */
final case class ProjectConfig(file: Path,
                               scalaVersion: String,
                               classpath: Seq[Path],
                               scalacOptions: Seq[String],
                               sourceRoots: Seq[Path],
                               sourceDirectories: Seq[Path],
                               generatedDirectory: Path) {

  /** コンパイル完了の印. sbtがコンパイルするたびに更新されます. */
  def stampFile: Path = file.resolveSibling(ProjectConfig.StampName)

  def contains(stFile: Path): Boolean = sourceRoots.exists(r => stFile.startsWith(r))

  /**
   * presentation compilerに渡すオプション. コンパイラプラグインや、警告をエラーにする設定は除きます.
   */
  def compilerOptions: Seq[String] = {
    scalacOptions.filterNot { o =>
      o.startsWith("-Xplugin") || o.startsWith("-P:") || o == "-Xfatal-warnings" || o == "-Werror" ||
        o.startsWith("-semanticdb") || o == "-Xsemanticdb"
    }
  }

}

object ProjectConfig {

  val FileName = "scalactive-lsp.json"
  val StampName = "scalactive-lsp.stamp"

  def load(file: Path): Either[String, ProjectConfig] = {
    try {
      val o = JsonParser.parseString(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)).getAsJsonObject
      def str(k: String): String = o.get(k).getAsString
      def strs(k: String): Seq[String] = Option(o.getAsJsonArray(k)).map(_.asScala.map(_.getAsString).toList).getOrElse(Nil)
      def paths(k: String): Seq[Path] = strs(k).map(p => Paths.get(p).toAbsolutePath.normalize)
      val version = Option(o.get("version")).map(_.getAsInt).getOrElse(0)
      if (version != 1) Left(s"$file: unsupported version $version (update the Scalactive editor extension or sbt-scalactive)")
      else Right(ProjectConfig(
        file = file.toAbsolutePath.normalize,
        scalaVersion = str("scalaVersion"),
        classpath = paths("classpath"),
        scalacOptions = strs("scalacOptions"),
        sourceRoots = paths("sourceRoots"),
        sourceDirectories = paths("sourceDirectories"),
        generatedDirectory = Paths.get(str("generatedDirectory")).toAbsolutePath.normalize
      ))
    } catch {
      case e: Exception => Left(s"cannot read $file: $e")
    }
  }

  /**
   * .stファイルの親ディレクトリを遡り、そのファイルを含むプロジェクトの target/scalactive-lsp.json を探します.
   */
  def find(stFile: Path): Option[Path] = {
    val abs = stFile.toAbsolutePath.normalize
    Iterator.iterate(abs.getParent)(_.getParent).takeWhile(_ != null)
      .map(_.resolve("target").resolve(FileName))
      .find(f => Files.isRegularFile(f) && load(f).exists(_.contains(abs)))
  }

}
