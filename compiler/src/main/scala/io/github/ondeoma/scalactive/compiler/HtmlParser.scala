package io.github.ondeoma.scalactive.compiler

import scala.collection.mutable.ListBuffer

/**
 * @param quote       値の囲み文字. 囲みなし/値なしの場合は0.
 * @param valueOffset 値の先頭のオフセット. 値なしの場合は-1.
 */
final case class Attr(name: String, value: Option[String], quote: Char, offset: Int, valueOffset: Int = -1) {

  /** 値を.st上の位置付きで返します. */
  def located: Option[Located] = value.map(v => Located(v, valueOffset))

}

final case class OpenTag(name: String, attrs: List[Attr], end: Int, selfClosing: Boolean)

sealed trait HtmlNode {
  def offset: Int
}

final case class Element(name: String,
                         attrs: List[Attr],
                         children: List[HtmlNode],
                         selfClosing: Boolean,
                         offset: Int) extends HtmlNode

/** {{ expr }} を含みうるテキスト */
final case class Text(text: String, offset: Int) extends HtmlNode

/** そのまま出力するテキスト(コメント、script/style/textareaの中身) */
final case class RawText(text: String, offset: Int) extends HtmlNode

object TagReader {

  private def isNameChar(c: Char): Boolean = {
    c.isLetterOrDigit || c == '-' || c == '_' || c == ':' || c == '.'
  }

  /**
   * startは'<'の位置.
   */
  def readOpenTag(src: SourceText, start: Int): OpenTag = {
    val s = src.text
    var i = start + 1
    while (i < s.length && isNameChar(s.charAt(i))) i += 1
    if (i == start + 1) src.fail(start, "tag name expected")
    val name = s.substring(start + 1, i)

    val attrs = ListBuffer[Attr]()
    var selfClosing = false
    var done = false
    while (!done) {
      while (i < s.length && s.charAt(i).isWhitespace) i += 1
      if (i >= s.length) src.fail(start, s"unclosed tag <$name>")
      else if (s.charAt(i) == '>') {
        i += 1
        done = true
      } else if (s.startsWith("/>", i)) {
        i += 2
        selfClosing = true
        done = true
      } else {
        val aStart = i
        while (i < s.length && !s.charAt(i).isWhitespace && s.charAt(i) != '=' && s.charAt(i) != '>' && !s.startsWith("/>", i)) i += 1
        if (i == aStart) src.fail(aStart, s"attribute name expected in <$name>")
        val aName = s.substring(aStart, i)
        var j = i
        while (j < s.length && s.charAt(j).isWhitespace) j += 1
        if (j < s.length && s.charAt(j) == '=') {
          j += 1
          while (j < s.length && s.charAt(j).isWhitespace) j += 1
          if (j >= s.length) src.fail(aStart, s"attribute value expected for '$aName'")
          val q = s.charAt(j)
          if (q == '"' || q == '\'') {
            val e = s.indexOf(q.toInt, j + 1)
            if (e < 0) src.fail(aStart, s"unclosed attribute value for '$aName'")
            attrs += Attr(aName, Some(s.substring(j + 1, e)), q, aStart, j + 1)
            i = e + 1
          } else {
            val vs = j
            while (j < s.length && !s.charAt(j).isWhitespace && s.charAt(j) != '>') j += 1
            attrs += Attr(aName, Some(s.substring(vs, j)), 0, aStart, vs)
            i = j
          }
        } else {
          attrs += Attr(aName, None, 0, aStart)
        }
      }
    }
    OpenTag(name, attrs.toList, i, selfClosing)
  }

}

/**
 * <template>の中身を構文木にする最小限のHTMLパーサー.
 * HTMLの省略規則(</p>の省略など)は扱わず、開始・終了タグの対応を厳密に要求します.
 */
object HtmlParser {

  val voidElements: Set[String] = Set(
    "area", "base", "br", "col", "embed", "hr", "img", "input",
    "link", "meta", "param", "source", "track", "wbr"
  )

  val rawTextElements: Set[String] = Set("script", "style", "textarea")

  private final class Frame(val name: String, val attrs: List[Attr], val offset: Int) {
    val children: ListBuffer[HtmlNode] = ListBuffer()
  }

  def parse(src: SourceText, from: Int, until: Int): List[HtmlNode] = {
    val s = src.text
    val root = new Frame("", Nil, from)
    var stack = List(root)
    val text = new StringBuilder
    var textStart = -1
    var i = from

    def appendText(t: String, at: Int): Unit = {
      if (textStart < 0) textStart = at
      text ++= t
    }

    def flushText(): Unit = {
      if (text.nonEmpty) {
        stack.head.children += Text(text.toString, textStart)
        text.clear()
      }
      textStart = -1
    }

    def isTagStart(at: Int): Boolean = at < until && s.charAt(at).isLetter

    while (i < until) {
      if (s.startsWith("{{", i)) {
        val e = s.indexOf("}}", i + 2)
        if (e < 0 || e + 2 > until) src.fail(i, "unclosed '{{'")
        appendText(s.substring(i, e + 2), i)
        i = e + 2
      } else if (s.startsWith("<!--", i)) {
        flushText()
        val e = s.indexOf("-->", i + 4)
        if (e < 0 || e + 3 > until) src.fail(i, "unclosed comment")
        stack.head.children += RawText(s.substring(i, e + 3), i)
        i = e + 3
      } else if (s.startsWith("</", i) && isTagStart(i + 2)) {
        flushText()
        val e = s.indexOf('>', i)
        if (e < 0 || e >= until) src.fail(i, "unclosed end tag")
        val name = s.substring(i + 2, e).trim
        val top = stack.head
        if (top eq root) src.fail(i, s"unexpected end tag </$name>")
        if (top.name != name) {
          src.fail(i, s"expected </${top.name}> (opened at line ${src.lineOf(top.offset)}) but found </$name>")
        }
        stack = stack.tail
        stack.head.children += Element(top.name, top.attrs, top.children.toList, selfClosing = false, top.offset)
        i = e + 1
      } else if (s.charAt(i) == '<' && isTagStart(i + 1)) {
        flushText()
        val t = TagReader.readOpenTag(src, i)
        if (t.end > until) src.fail(i, s"unclosed tag <${t.name}>")
        val lname = t.name.toLowerCase
        if (t.selfClosing || voidElements.contains(lname)) {
          stack.head.children += Element(t.name, t.attrs, Nil, t.selfClosing, i)
          i = t.end
        } else if (rawTextElements.contains(lname)) {
          val close = s.indexOf("</" + t.name, t.end)
          if (close < 0 || close >= until) src.fail(i, s"unclosed tag <${t.name}>")
          val closeEnd = s.indexOf('>', close)
          val children = if (close > t.end) List(RawText(s.substring(t.end, close), t.end)) else Nil
          stack.head.children += Element(t.name, t.attrs, children, selfClosing = false, i)
          i = closeEnd + 1
        } else {
          stack = new Frame(t.name, t.attrs, i) :: stack
          i = t.end
        }
      } else {
        appendText(s.charAt(i).toString, i)
        i += 1
      }
    }
    flushText()
    if (!(stack.head eq root)) src.fail(stack.head.offset, s"unclosed tag <${stack.head.name}>")
    root.children.toList
  }

}
