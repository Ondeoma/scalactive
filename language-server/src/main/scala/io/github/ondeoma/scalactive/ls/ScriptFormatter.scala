package io.github.ondeoma.scalactive.ls

import io.github.ondeoma.scalactive.compiler.{Element, HtmlNode, HtmlParser, SfcParser, SourceText, StCompileException, Text}
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

  private val tempConfigs = scala.collection.mutable.Map[String, Path]()

  /** 内容が同じ設定は同じ一時ファイルを使う */
  private def tempConfig(text: String): Path = tempConfigs.getOrElseUpdate(text, {
    val f = Files.createTempFile("scalactive-scalafmt", ".conf")
    f.toFile.deleteOnExit()
    Files.write(f, text.getBytes(StandardCharsets.UTF_8))
    f
  })

  /**
   * stFileに使う設定ファイル(.scalafmt.conf がなければ組み込みの設定).
   * overridesは設定の最後に足す行(後に書いた設定が優先されます).
   */
  private def configFor(stFile: Path, overrides: String = ""): Path = {
    val found = Iterator.iterate(stFile.toAbsolutePath.normalize.getParent)(_.getParent).takeWhile(_ != null)
      .map(_.resolve(".scalafmt.conf")).find(Files.isRegularFile(_))
    found match {
      case Some(conf) =>
        val text = new String(Files.readAllBytes(conf), StandardCharsets.UTF_8)
        // scriptはScala 3なので方言の指定がなければ補い、バージョンの指定がなければ既定のものを補う(元の設定ファイルは変えない)
        val extra =
          (if (text.contains("runner.dialect")) "" else "\nrunner.dialect = scala3") +
            (if ("(?m)^\\s*version\\s*=".r.findFirstIn(text).isDefined) "" else "\nversion = \"" + ScriptFormatter.DefaultVersion + "\"") +
            (if (overrides.isEmpty) "" else "\n" + overrides)
        if (extra.isEmpty) conf else tempConfig(text + extra + "\n")
      case None =>
        if (overrides.isEmpty) defaultConfig else tempConfig(ScriptFormatter.DefaultConfig + overrides + "\n")
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
   * 式をまとめて整形します(1つの object に def __eN = 式 として並べ、1回で整形する).
   * 1行に収まったものだけを返します(整形できなかった式は含みません).
   */
  private def formatExpressions(exprs: Vector[String], stFile: Path): Map[Int, String] = synchronized {
    if (exprs.isEmpty) return Map.empty
    val config = configFor(stFile, "maxColumn = 10000")
    val file = stFile.resolveSibling(stFile.getFileName.toString.stripSuffix(".st") + ".scala")
    val Def = "^\\s*def __e(\\d+) = (.*)$".r
    def run(indices: Seq[Int]): Option[Map[Int, String]] = {
      val code = indices.map(i => s"  def __e$i = ${exprs(i)}").mkString(s"object $wrapperName {\n", "\n", "\n}\n")
      failed = false
      val out = try scalafmt.format(config, file, code) catch {
        case NonFatal(e) =>
          log(s"scalafmt failed: $e")
          failed = true
          code
      }
      if (failed) None
      else {
        val lines = out.split("\n").toVector
        Some(lines.indices.flatMap { n =>
          lines(n) match {
            // 次の行が別の def か } なら、1行に収まっている
            case Def(i, body) if n + 1 < lines.length && (lines(n + 1).trim.startsWith("def __e") || lines(n + 1).trim == "}") =>
              Some(i.toInt -> body)
            case _ => None
          }
        }.toMap)
      }
    }
    run(exprs.indices).getOrElse {
      // どれかの式が読めない(書きかけなど)ときは、1つずつ整形する
      exprs.indices.flatMap(i => run(Seq(i)).getOrElse(Map.empty)).toMap
    }
  }

  /** テンプレートのScalaの部分 */
  private final case class Part(start: Int, end: Int, text: String, statements: Boolean, interpolation: Boolean)

  private def isExpressionAttr(name: String): Boolean = {
    name.startsWith(":") || name.startsWith("v-bind:") || name.startsWith("v-model") || name == "ref" ||
      Set("v-if", "v-else-if", "v-show", "v-html").contains(name)
  }

  /** ; で区切られた文(括弧・文字列の外の ;)に分ける */
  private def splitStatements(s: String): List[String] = {
    val parts = scala.collection.mutable.ListBuffer[String]()
    var depth = 0
    var quote: Char = 0
    var start = 0
    var i = 0
    while (i < s.length) {
      val c = s.charAt(i)
      if (quote != 0) {
        if (c == '\\') i += 1 else if (c == quote) quote = 0
      } else if (c == '"' || c == '\'') quote = c
      else if ("([{".indexOf(c) >= 0) depth += 1
      else if (")]}".indexOf(c) >= 0) depth -= 1
      else if (c == ';' && depth == 0) {
        parts += s.substring(start, i)
        start = i + 1
      }
      i += 1
    }
    parts += s.substring(start)
    parts.toList.map(_.trim)
  }

  /**
   * テンプレートのScalaの部分({{ }}、:attr・v-if・v-model・ref などの属性値、v-for の in の右側、@event の文)を
   * scalafmtで整形し、{{ }} の内側の空白を {{ x }} にそろえます. 整形できない部分はそのままです.
   * (HTMLの文字参照(&amp;quot; など)を含む値と、複数行の値は対象外)
   */
  def formatTemplate(source: String, stFile: Path): String = {
    val src = new SourceText(source)
    val template = try SfcParser.parse(src).template catch {
      case _: StCompileException => None
    }
    val parts: List[Part] = template.toList.flatMap { t =>
      val nodes = try HtmlParser.parse(src, t.contentStart, t.contentEnd) catch {
        case _: StCompileException => Nil
      }
      def walk(ns: List[HtmlNode]): List[Part] = ns.flatMap {
        case Text(text, offset) =>
          "\\{\\{(.*?)\\}\\}".r.findAllMatchIn(text).toList.collect {
            case m if !m.group(1).contains("\n") && m.group(1).trim.nonEmpty && !m.group(1).contains("&") =>
              Part(offset + m.start, offset + m.end, m.group(1).trim, statements = false, interpolation = true)
          }
        case e: Element =>
          val own = e.attrs.flatMap { a =>
            a.value.filter(v => a.quote != 0 && !v.contains("\n") && !v.contains("&") && v.trim.nonEmpty).flatMap { v =>
              if (isExpressionAttr(a.name)) Some(Part(a.valueOffset, a.valueOffset + v.length, v, statements = false, interpolation = false))
              else if (a.name.startsWith("@") || a.name.startsWith("v-on:")) Some(Part(a.valueOffset, a.valueOffset + v.length, v, statements = true, interpolation = false))
              else if (a.name == "v-for") {
                val in = v.indexOf(" in ")
                if (in < 0) None
                else {
                  val start = in + 4
                  val expr = v.substring(start)
                  val lead = expr.length - expr.dropWhile(_ == ' ').length
                  Some(Part(a.valueOffset + start + lead, a.valueOffset + v.length, expr.trim, statements = false, interpolation = false))
                }
              } else None
            }
          }
          own ++ walk(e.children)
        case _ => Nil
      }
      walk(nodes)
    }
    if (parts.isEmpty) return source
    // 文はひとつずつ式として整形する
    val pieces: Vector[String] = parts.flatMap(p => if (p.statements) splitStatements(p.text) else List(p.text)).toVector
    val formatted = formatExpressions(pieces, stFile)
    var index = 0
    val replacements = parts.flatMap { p =>
      val n = if (p.statements) splitStatements(p.text).size else 1
      val mine = (index until index + n).map(i => formatted.get(i))
      index += n
      if (mine.exists(_.isEmpty)) {
        // 整形できなくても、{{ }} の内側の空白はそろえる
        if (p.interpolation) Some((p.start, p.end, "{{ " + p.text + " }}")) else None
      } else {
        val body = mine.flatten.mkString("; ")
        val quoteChars = source.charAt(p.start - 1)
        if (!p.interpolation && (body.contains(quoteChars.toString) && (quoteChars == '"' || quoteChars == '\''))) None
        else Some((p.start, p.end, if (p.interpolation) "{{ " + body + " }}" else body))
      }
    }
    replacements.sortBy(-_._1).foldLeft(source) { case (s, (start, end, text)) =>
      if (s.substring(start, end) == text) s else s.substring(0, start) + text + s.substring(end)
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
