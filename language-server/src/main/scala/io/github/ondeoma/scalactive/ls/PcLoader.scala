package io.github.ondeoma.scalactive.ls

import coursierapi.{Dependency, Fetch}
import scala.meta.pc.PresentationCompiler

import java.net.{URL, URLClassLoader}
import java.nio.file.{Path, Paths}
import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConverters._

/**
 * Scala 3のpresentation compiler(Metalsと同じ、補完・ホバーなどを提供するコンパイラ)を読み込みます.
 *
 * 言語サーバー自身はScala 2.12なので、presentation compilerとそのScalaライブラリは別のクラスローダーで動かし、
 * インターフェース(scala.meta.pc)とLSPの型(lsp4j)だけを共有します.
 */
object PcLoader {

  /** presentation compilerを提供している最初のバージョン */
  private val minimumVersion = List(3, 3, 2)

  private val classLoaders = new ConcurrentHashMap[String, ClassLoader]()

  /**
   * 指定したScalaバージョンのpresentation compilerを返します. 初回はMaven Centralから取得します(coursierのキャッシュを使います).
   * 環境変数 SCALACTIVE_PC_CLASSPATH にjarのパスを(パス区切り文字で)指定すると、取得せずにそれを使います.
   */
  def load(scalaVersion: String): PresentationCompiler = {
    classLoader(scalaVersion).loadClass("dotty.tools.pc.ScalaPresentationCompiler")
      .getDeclaredConstructor().newInstance().asInstanceOf[PresentationCompiler]
  }

  /** presentation compilerとScala 3コンパイラを含むクラスローダー */
  def classLoader(scalaVersion: String): ClassLoader = {
    val numbers = scalaVersion.takeWhile(c => c.isDigit || c == '.').split('.').toList.flatMap(s => scala.util.Try(s.toInt).toOption)
    if (numbers.headOption.forall(_ != 3)) {
      throw new IllegalArgumentException(s"Scala $scalaVersion is not supported (Scala 3 is required)")
    }
    if (numbers.padTo(3, 0).zip(minimumVersion).find { case (a, b) => a != b }.exists { case (a, b) => a < b }) {
      throw new IllegalArgumentException(s"Scala $scalaVersion is not supported by the language server (3.3.2 or later is required)")
    }
    classLoaders.computeIfAbsent(scalaVersion, v => newClassLoader(jars(v)))
  }

  private def jars(scalaVersion: String): Seq[Path] = {
    sys.env.get("SCALACTIVE_PC_CLASSPATH").filter(_.nonEmpty) match {
      case Some(cp) => cp.split(java.io.File.pathSeparator).toList.map(Paths.get(_))
      case None =>
        Fetch.create()
          .addDependencies(Dependency.of("org.scala-lang", "scala3-presentation-compiler_3", scalaVersion))
          .fetch().asScala.map(_.toPath).toList
    }
  }

  private def newClassLoader(jars: Seq[Path]): ClassLoader = {
    new URLClassLoader(jars.map(_.toUri.toURL).toArray[URL], new SharedClassLoader(getClass.getClassLoader))
  }

  /**
   * JDKのクラスに加えて、共有するパッケージだけを言語サーバーのクラスローダーから読み込むクラスローダー.
   */
  private final class SharedClassLoader(server: ClassLoader) extends ClassLoader(ClassLoader.getPlatformClassLoader) {

    private val sharedPrefixes = List("scala.meta.pc.", "org.eclipse.lsp4j.", "com.google.gson.")

    override def findClass(name: String): Class[_] = {
      if (sharedPrefixes.exists(name.startsWith)) server.loadClass(name)
      else throw new ClassNotFoundException(name)
    }

  }

}
