package io.github.ondeoma.scalactive.compiler

/**
 * .stの整形(エディタの「ドキュメントのフォーマット」).
 *
 * 内容を壊さないよう、次のことだけを行います.
 *  - テンプレート: 要素の入れ子に合わせて2スペースずつインデントし直す.
 *    複数行にまたがる開始タグの続きの行は、タグより4スペース深くする.
 *    <pre>・<textarea>の中身、複数行のコメント、{{ }} の中は変えない
 *  - <script>・<style>: インデントは変えず、前後の空行と行末の空白を整える
 *  - ファイル全体: 行末の空白を取り除き、最後を改行1つにする
 * 構文エラーなどで読めない場合は、そのまま返します.
 */
object StFormatter {

  private val indentUnit = "  "
  private val continuationIndent = "    "

  /** 中身をそのまま残す要素 */
  private val rawElements = Set("pre", "textarea", "script", "style")

  def format(source: String): String = {
    val normalized = source.replace("\r\n", "\n")
    val sfc = try SfcParser.parse(new SourceText(normalized)) catch {
      case _: StCompileException => return source
    }
    val blocks = (sfc.script.toList ++ sfc.template.toList ++ sfc.styles).sortBy(_.contentStart)
    val sb = new StringBuilder
    var last = 0
    blocks.foreach { b =>
      sb ++= normalized.substring(last, b.contentStart)
      val content = normalized.substring(b.contentStart, b.contentEnd)
      sb ++= (if (b.name == "template") formatTemplate(content) else formatBlock(content))
      last = b.contentEnd
    }
    sb ++= normalized.substring(last)
    val lines = sb.toString.split("\n", -1).map(stripTrailing)
    lines.mkString("\n").replaceAll("\\s+$", "") + "\n"
  }

  private def stripTrailing(s: String): String = s.replaceAll("[ \t]+$", "")

  private def isBlank(s: String): Boolean = s.trim.isEmpty

  /** 前後の空行を取り除き、行末の空白を整える(インデントは変えない) */
  private def formatBlock(content: String): String = {
    val lines = content.split("\n", -1).toList.map(stripTrailing)
    val trimmed = lines.dropWhile(isBlank).reverse.dropWhile(isBlank).reverse
    if (trimmed.isEmpty) "\n" else trimmed.mkString("\n", "\n", "\n")
  }

  /** テンプレートの走査の状態(行をまたいで引き継ぐ) */
  private final class State {
    var depth = 0
    var inComment = false
    var inInterpolation = false
    /** 開始タグの途中(属性が次の行に続く)ならタグの行のインデント */
    var inTag: Option[String] = None
    var tagName = ""
    var quote: Char = 0
    /** 中身をそのまま残す要素の閉じタグ名 */
    var rawUntil: Option[String] = None
  }

  private def formatTemplate(content: String): String = {
    val lines = content.split("\n", -1).toList.map(stripTrailing)
    val trimmed = lines.dropWhile(isBlank).reverse.dropWhile(isBlank).reverse
    if (trimmed.isEmpty) return "\n"
    val st = new State
    val out = trimmed.map { line =>
      if (st.rawUntil.isDefined || st.inComment || st.inInterpolation) {
        // 中身をそのまま残す
        scan(line, st, "")
        line
      } else {
        val body = line.dropWhile(c => c == ' ' || c == '\t')
        if (body.isEmpty) ""
        else {
          val indent = st.inTag match {
            case Some(tagIndent) => tagIndent + continuationIndent
            case None =>
              val d = if (body.startsWith("</")) st.depth - 1 else st.depth
              indentUnit + indentUnit * math.max(d, 0)
          }
          scan(body, st, indent)
          indent + body
        }
      }
    }
    out.mkString("\n", "\n", "\n")
  }

  private def isNameChar(c: Char): Boolean = c.isLetterOrDigit || c == '-' || c == '_' || c == ':' || c == '.'

  /** 1行を走査して、入れ子の深さなどの状態を更新します. indentはこの行のインデント(開始タグが行をまたぐ場合に使う) */
  private def scan(line: String, st: State, indent: String): Unit = {
    var i = 0
    while (i < line.length) {
      if (st.inComment) {
        val end = line.indexOf("-->", i)
        if (end < 0) i = line.length
        else {
          st.inComment = false
          i = end + 3
        }
      } else if (st.inInterpolation) {
        val end = line.indexOf("}}", i)
        if (end < 0) i = line.length
        else {
          st.inInterpolation = false
          i = end + 2
        }
      } else if (st.inTag.isDefined) {
        // 開始タグの中(属性)
        val c = line.charAt(i)
        if (st.quote != 0) {
          if (c == st.quote) st.quote = 0
          i += 1
        } else if (c == '"' || c == '\'') {
          st.quote = c
          i += 1
        } else if (line.startsWith("/>", i)) {
          st.inTag = None
          i += 2
        } else if (c == '>') {
          st.inTag = None
          openElement(st)
          i += 1
        } else i += 1
      } else if (st.rawUntil.isDefined) {
        val name = st.rawUntil.get
        val end = line.indexOf("</" + name, i)
        if (end < 0) i = line.length
        else {
          st.rawUntil = None
          st.depth = math.max(st.depth - 1, 0)
          val close = line.indexOf('>', end)
          i = if (close < 0) line.length else close + 1
        }
      } else if (line.startsWith("<!--", i)) {
        st.inComment = true
        i += 4
      } else if (line.startsWith("{{", i)) {
        st.inInterpolation = true
        i += 2
      } else if (line.startsWith("</", i)) {
        st.depth = math.max(st.depth - 1, 0)
        val close = line.indexOf('>', i)
        i = if (close < 0) line.length else close + 1
      } else if (line.charAt(i) == '<' && i + 1 < line.length && line.charAt(i + 1).isLetter) {
        var j = i + 1
        while (j < line.length && isNameChar(line.charAt(j))) j += 1
        st.tagName = line.substring(i + 1, j)
        st.inTag = Some(indent)
        st.quote = 0
        i = j
      } else i += 1
    }
  }

  /** 開始タグが閉じた(>)ときの処理 */
  private def openElement(st: State): Unit = {
    val name = st.tagName.toLowerCase
    if (!HtmlParser.voidElements.contains(name)) {
      st.depth += 1
      if (rawElements.contains(name)) st.rawUntil = Some(st.tagName)
    }
  }

}
