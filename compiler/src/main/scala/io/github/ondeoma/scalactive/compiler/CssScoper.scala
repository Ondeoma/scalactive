package io.github.ondeoma.scalactive.compiler

/**
 * <style scoped>用に、各セレクタの最後の複合セレクタへ属性セレクタを付与します.
 * 例: `.a .b:hover` → `.a .b[data-st-xxxx]:hover`
 */
object CssScoper {

  private val nestedAtRules = Set("media", "supports", "container", "layer", "document")

  def scope(css: String, attr: String): String = scopeBlock(css, attr)

  private def scopeBlock(css: String, attr: String): String = {
    val out = new StringBuilder
    var i = 0
    while (i < css.length) {
      val stop = findTopLevel(css, i, Set('{', ';', '}'))
      if (stop < 0) {
        out ++= css.substring(i)
        i = css.length
      } else if (css.charAt(stop) == '{') {
        val close = findMatchingBrace(css, stop)
        val prelude = css.substring(i, stop)
        val body = css.substring(stop + 1, close)
        val p = stripComments(prelude).trim
        if (p.startsWith("@")) {
          val keyword = p.drop(1).takeWhile(c => c.isLetter || c == '-').toLowerCase
          val scopedBody = if (nestedAtRules.contains(keyword)) scopeBlock(body, attr) else body
          out ++= prelude + "{" + scopedBody + "}"
        } else {
          out ++= scopeSelectorList(prelude, attr) + "{" + body + "}"
        }
        i = close + 1
      } else {
        out ++= css.substring(i, stop + 1)
        i = stop + 1
      }
    }
    out.toString
  }

  def scopeSelectorList(prelude: String, attr: String): String = {
    val leading = prelude.takeWhile(_.isWhitespace)
    val trailing = if (prelude.trim.isEmpty) "" else prelude.reverse.takeWhile(_.isWhitespace).reverse
    val selectors = splitTopLevel(stripComments(prelude), ',').map(_.trim).filter(_.nonEmpty)
    leading + selectors.map(scopeSelector(_, attr)).mkString(", ") + trailing
  }

  def scopeSelector(sel: String, attr: String): String = {
    // 最後の複合セレクタの開始位置
    var lastStart = 0
    walkTopLevel(sel) { (c, at) =>
      if (c.isWhitespace || c == '>' || c == '+' || c == '~') lastStart = at + 1
    }
    val compound = sel.substring(lastStart)
    // 疑似クラス/疑似要素の手前に挿入
    var insertAt = compound.length
    walkTopLevel(compound) { (c, at) =>
      if (c == ':' && insertAt == compound.length) insertAt = at
    }
    sel.substring(0, lastStart) + compound.substring(0, insertAt) + s"[$attr]" + compound.substring(insertAt)
  }

  /** 括弧・文字列・エスケープの外側にある文字だけを走査します. */
  private def walkTopLevel(s: String)(f: (Char, Int) => Unit): Unit = {
    var depth = 0
    var quote: Char = 0
    var i = 0
    while (i < s.length) {
      val c = s.charAt(i)
      if (c == '\\') {
        i += 1
      } else if (quote != 0) {
        if (c == quote) quote = 0
      } else if (c == '"' || c == '\'') {
        quote = c
      } else if (c == '(' || c == '[') {
        depth += 1
      } else if (c == ')' || c == ']') {
        depth -= 1
      } else if (depth == 0) {
        f(c, i)
      }
      i += 1
    }
  }

  private def splitTopLevel(s: String, sep: Char): List[String] = {
    val cuts = scala.collection.mutable.ListBuffer[Int]()
    walkTopLevel(s)((c, at) => if (c == sep) cuts += at)
    val bounds = (-1 :: cuts.toList) :+ s.length
    bounds.zip(bounds.tail).map { case (a, b) => s.substring(a + 1, b) }
  }

  private def stripComments(s: String): String = s.replaceAll("(?s)/\\*.*?\\*/", "")

  /** コメント・文字列を飛ばして、stopsのいずれかが最初に現れる位置を返します. */
  private def findTopLevel(s: String, from: Int, stops: Set[Char]): Int = {
    var i = from
    var quote: Char = 0
    while (i < s.length) {
      val c = s.charAt(i)
      if (c == '\\') {
        i += 1
      } else if (quote != 0) {
        if (c == quote) quote = 0
      } else if (s.startsWith("/*", i)) {
        val e = s.indexOf("*/", i + 2)
        i = if (e < 0) s.length else e + 1
      } else if (c == '"' || c == '\'') {
        quote = c
      } else if (stops.contains(c)) {
        return i
      }
      i += 1
    }
    -1
  }

  private def findMatchingBrace(s: String, open: Int): Int = {
    var depth = 0
    var i = open
    while (i < s.length) {
      val stop = findTopLevel(s, i, Set('{', '}'))
      if (stop < 0) return s.length
      if (s.charAt(stop) == '{') depth += 1 else depth -= 1
      if (depth == 0) return stop
      i = stop + 1
    }
    s.length
  }

}
