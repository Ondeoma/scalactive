package io.github.ondeoma.scalactive.ls

import io.github.ondeoma.scalactive.compiler.{SfcParser, SourceText, StCompileException}
import org.scalafmt.interfaces.{Scalafmt, ScalafmtReporter}

import java.io.{OutputStreamWriter, PrintWriter, Writer}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.util.control.NonFatal

/**
 * <script>(Scala)をscalafmtで整形します.
 *
 *  - .stのディレクトリから上へ .scalafmt.conf を探し、あればその設定(バージョン)を使います.
 *    `runner.dialect` を指定していなければ scala3 を補います.
 *  - なければ、組み込みの設定(DefaultConfig)を使います.
 *  - scalafmt本体は、設定のバージョンのものを初回に取得します(coursierのキャッシュを使います).
 *  - scriptの中身は文(if など)を含むことがあり、ファイルの最上位には書けないため、`object` の本体として整形し、取り出します.
 *  - 構文エラーなどで整形できないときは None(呼び出し側は空白の整理だけを行います).
 */
final class ScriptFormatter(log: String => Unit) {

  private val wrapperName = "ScalactiveScript__"

  /** 整形中にscalafmtがエラーを報告したか(scalafmtは整形できないとき入力をそのまま返すため) */
  @volatile private var failed = false

  private val reporter: ScalafmtReporter = new ScalafmtReporter {
    override def error(file: Path, message: String): Unit = { failed = true; log(s"scalafmt: $file: $message") }
    override def error(file: Path, e: Throwable): Unit = { failed = true; log(s"scalafmt: $file: $e") }
    override def excluded(file: Path): Unit = ()
    override def parsedConfig(config: Path, scalafmtVersion: String): Unit = log(s"scalafmt $scalafmtVersion: $config")
    // 標準出力はLSPの通信に使うため、ダウンロードの進捗はログへ
    override def downloadWriter(): PrintWriter = new PrintWriter(new Writer {
      private val sb = new StringBuilder
      override def write(cbuf: Array[Char], off: Int, len: Int): Unit = sb.appendAll(cbuf, off, len)
      override def flush(): Unit = if (sb.nonEmpty) { log("scalafmt: " + sb.toString.trim); sb.clear() }
      override def close(): Unit = flush()
    })
    override def downloadOutputStreamWriter(): OutputStreamWriter = new OutputStreamWriter(System.err, StandardCharsets.UTF_8)
  }

  private lazy val scalafmt: Scalafmt =
    Scalafmt.create(getClass.getClassLoader)
      .withReporter(reporter)
      .withRespectProjectFilters(false)

  private lazy val defaultConfig: Path = {
    val f = Files.createTempFile("scalactive-scalafmt", ".conf")
    f.toFile.deleteOnExit()
    Files.write(f, ScriptFormatter.DefaultConfig.getBytes(StandardCharsets.UTF_8))
    f
  }

  /** stFileに使う設定ファイル(.scalafmt.conf がなければ組み込みの設定) */
  private def configFor(stFile: Path): Path = {
    val found = Iterator.iterate(stFile.toAbsolutePath.normalize.getParent)(_.getParent).takeWhile(_ != null)
      .map(_.resolve(".scalafmt.conf")).find(Files.isRegularFile(_))
    found match {
      case Some(conf) =>
        val text = new String(Files.readAllBytes(conf), StandardCharsets.UTF_8)
        val extra =
          (if (text.contains("runner.dialect")) "" else "\nrunner.dialect = scala3") +
            (if ("(?m)^\\s*version\\s*=".r.findFirstIn(text).isDefined) "" else "\nversion = \"" + ScriptFormatter.DefaultVersion + "\"")
        if (extra.isEmpty) conf
        else {
          // scriptはScala 3なので方言の指定がなければ補い、バージョンの指定がなければ既定のものを補う(元の設定ファイルは変えない)
          val f = Files.createTempFile("scalactive-scalafmt", ".conf")
          f.toFile.deleteOnExit()
          Files.write(f, (text + extra + "\n").getBytes(StandardCharsets.UTF_8))
          f
        }
      case None => defaultConfig
    }
  }

  /** scriptの中身(インデントは0の前提)を整形します */
  def formatScript(content: String, stFile: Path): Option[String] = synchronized {
    if (content.trim.isEmpty) return None
    val body = content.split("\n", -1).map(l => if (l.trim.isEmpty) "" else "  " + l).mkString("\n")
    val wrapped = s"object $wrapperName {\n$body\n}\n"
    try {
      val file = stFile.resolveSibling(stFile.getFileName.toString.stripSuffix(".st") + ".scala")
      failed = false
      val out = scalafmt.format(configFor(stFile), file, wrapped)
      if (failed) None else unwrap(out)
    } catch {
      case NonFatal(e) =>
        log(s"scalafmt failed: $e")
        None
    }
  }

  private def unwrap(out: String): Option[String] = {
    val lines = out.split("\n", -1).toList.reverse.dropWhile(_.trim.isEmpty).reverse
    if (lines.size < 2 || lines.head.trim != s"object $wrapperName {" || lines.last.trim != "}") None
    else {
      val inner = lines.tail.init
      if (inner.exists(l => l.nonEmpty && !l.startsWith("  "))) None
      else Some(inner.map(_.drop(2)).mkString("\n"))
    }
  }

  /**
   * .st全体のうち<script>の中身をscalafmtで整形したものを返します. 整形できなければ None.
   */
  def format(source: String, stFile: Path): Option[String] = {
    val sfc = try SfcParser.parse(new SourceText(source)) catch {
      case _: StCompileException => return None
    }
    sfc.script.filter(b => b.attr("lang").flatMap(_.value).forall(_ == "scala")).flatMap { b =>
      val content = source.substring(b.contentStart, b.contentEnd)
      // 前後の空行は除いて整形し、StFormatterが整える
      val trimmed = content.split("\n", -1).toList.dropWhile(_.trim.isEmpty).reverse.dropWhile(_.trim.isEmpty).reverse
      // scriptの中身全体のインデント(タグより字下げして書く人もいる)は保つ
      val base = trimmed.filter(_.trim.nonEmpty).map(_.takeWhile(_ == ' ').length).reduceOption(_ min _).getOrElse(0)
      formatScript(trimmed.map(_.drop(base)).mkString("\n"), stFile).map { formatted =>
        val indented = formatted.split("\n", -1).map(l => if (l.isEmpty) l else " " * base + l).mkString("\n")
        source.substring(0, b.contentStart) + "\n" + indented + "\n" + source.substring(b.contentEnd)
      }
    }
  }

}

object ScriptFormatter {

  /** .scalafmt.conf がないときに使うscalafmtのバージョン */
  val DefaultVersion = "3.10.7"

  /**
   * .scalafmt.conf がないときの設定. 書いた人の改行の位置は保ち(newlines.source = keep)、
   * 定義の引数の続きの行は開き括弧の直後にそろえ、閉じ括弧は最後の引数の行に置いたまま、空白とインデントを整えます.
   */
  val DefaultConfig: String =
    s"""version = "$DefaultVersion"
       |runner.dialect = scala3
       |maxColumn = 120
       |newlines.source = keep
       |align.openParenDefnSite = true
       |danglingParentheses.preset = false
       |""".stripMargin

}
