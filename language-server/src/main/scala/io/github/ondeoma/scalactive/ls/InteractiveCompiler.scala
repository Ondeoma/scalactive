package io.github.ondeoma.scalactive.ls

import java.lang.reflect.Method
import java.net.URI
import java.util.Optional
import scala.collection.JavaConverters._

/**
 * Scala 3コンパイラの対話用ドライバ(InteractiveDriver)で型検査し、エラー・警告を返します.
 *
 * presentation compilerのdidChangeは、Scalaのバージョンによっては診断を返さないため、
 * 同じクラスローダー上のInteractiveDriverをリフレクションで直接使います.
 * ドライバは前回の結果(クラスパスの読み込みなど)を再利用するため、2回目以降は速く動きます.
 */
final class InteractiveCompiler(loader: ClassLoader, classpath: Seq[String], options: Seq[String]) {

  import InteractiveCompiler._

  private def cls(name: String): Class[_] = loader.loadClass(name)

  private val converters = cls("scala.jdk.javaapi.CollectionConverters")
  private val scalaList = cls("scala.collection.immutable.List")
  private val scalaIterable = cls("scala.collection.Iterable")

  private val driverClass = cls("dotty.tools.dotc.interactive.InteractiveDriver")
  private val runMethod: Method = driverClass.getMethod("run", classOf[URI], classOf[String])

  private val diagnosticClass = cls("dotty.tools.dotc.interfaces.Diagnostic")
  private val levelMethod = diagnosticClass.getMethod("level")
  private val messageMethod = diagnosticClass.getMethod("message")
  private val positionMethod = diagnosticClass.getMethod("position")
  private val positionClass = cls("dotty.tools.dotc.interfaces.SourcePosition")
  private val startMethod = positionClass.getMethod("start")
  private val endMethod = positionClass.getMethod("end")

  private val driver: AnyRef = {
    val settings = List("-classpath", classpath.mkString(java.io.File.pathSeparator)) ++ options
    val buffer = converters.getMethod("asScala", classOf[java.util.List[_]]).invoke(null, settings.asJava)
    val list = buffer.getClass.getMethod("toList").invoke(buffer)
    driverClass.getConstructor(scalaList).newInstance(list).asInstanceOf[AnyRef]
  }

  /** codeを型検査し、生成コード上の位置で診断を返します. */
  def diagnostics(uri: URI, code: String): List[CompilerDiagnostic] = synchronized {
    val result = runMethod.invoke(driver, uri, code)
    val javaList = converters.getMethod("asJava", scalaIterable).invoke(null, result).asInstanceOf[java.util.Collection[AnyRef]]
    javaList.asScala.toList.map { d =>
      val pos = positionMethod.invoke(d).asInstanceOf[Optional[AnyRef]]
      val (start, end) =
        if (pos.isPresent) (startMethod.invoke(pos.get).asInstanceOf[Int], endMethod.invoke(pos.get).asInstanceOf[Int])
        else (0, 0)
      CompilerDiagnostic(levelMethod.invoke(d).asInstanceOf[Int], messageMethod.invoke(d).asInstanceOf[String], start, end)
    }
  }

}

object InteractiveCompiler {

  /** @param level 0: 情報, 1: 警告, 2: エラー(dotty.tools.dotc.interfaces.Diagnosticの定数) */
  final case class CompilerDiagnostic(level: Int, message: String, start: Int, end: Int)

}
