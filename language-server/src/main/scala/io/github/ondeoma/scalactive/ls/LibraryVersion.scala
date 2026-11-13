package io.github.ondeoma.scalactive.ls

import java.nio.file.Path

/**
 * プロジェクトが使っているScalactive(core)の版の確認.
 *
 * 言語サーバーは.stを、同梱したコンパイラで生成コードに変換し、プロジェクトのScalactiveで型を検査します.
 * 生成コードが呼ぶ関数(StRuntimeの prop・componentOf・each など)がない古い版では、正しい.stにも誤ったエラーが並ぶため、
 * 先に1度だけ警告します.
 */
object LibraryVersion {

  /** この言語サーバーの生成コードに必要なScalactiveの版. 生成コードが新しい関数を使うようになったら上げる */
  val Required = "0.6.0"

  private val JarName = """scalactive_sjs1_[^-/\\]+-([0-9][^/\\]*)\.jar""".r

  /** classpathにあるScalactive(core)のjarの版(ソースから使っている場合などはNone) */
  def of(classpath: Seq[Path]): Option[String] = {
    classpath.iterator.map(_.getFileName).filter(_ != null).map(_.toString).collectFirst {
      case JarName(v) => v
    }
  }

  /** "0.6.0-SNAPSHOT" → List(0, 6, 0) */
  private def numbers(v: String): List[Int] = {
    v.takeWhile(c => c.isDigit || c == '.').split('.').toList.filter(_.nonEmpty).map(_.toInt)
  }

  def older(v: String, than: String): Boolean = {
    val a = numbers(v)
    val b = numbers(than)
    val n = math.max(a.size, b.size)
    val diff = a.padTo(n, 0).zip(b.padTo(n, 0)).find { case (x, y) => x != y }
    diff.exists { case (x, y) => x < y }
  }

  /** 古い版を使っている場合の警告 */
  def warning(classpath: Seq[Path]): Option[String] = {
    of(classpath).filter(older(_, Required)).map { v =>
      s"This Scalactive editor support needs Scalactive $Required or later, but the project uses $v. " +
        s"""Errors shown in .st files may be wrong until you update the library ("io.github.ondeoma" %%% "scalactive" % "$Required")."""
    }
  }

}
