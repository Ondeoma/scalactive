package io.github.ondeoma.scalactive.ls

import io.github.ondeoma.scalactive.compiler.{SfcParser, SourceText}

import scala.util.control.NonFatal

/**
 * .stの<style>で定義したCSSのクラス(テンプレートの class="..." の補完・定義ジャンプと、使われていないクラスの検査に使う).
 */
object CssClasses {

  /** クラスの定義. offsetは .st の中の `.name` の name の位置、scopedは <style scoped> の中か */
  final case class Definition(name: String, offset: Int, scoped: Boolean)

  private val ClassSelector = """\.(-?[_a-zA-Z][\w-]*)""".r

  /**
   * CSSの中身からクラスのセレクタを探します. 宣言の中(url(a.png) など)、コメント、文字列は除きます.
   * @param base 中身の先頭の、.st の中の位置
   */
  private[ls] def selectors(css: String, base: Int): List[(String, Int)] = {
    // コメントと文字列を同じ長さの空白にして、位置を保ったまま除く
    val masked = new StringBuilder(css)
    var i = 0
    while (i < css.length) {
      if (css.startsWith("/*", i)) {
        val e = css.indexOf("*/", i + 2)
        val end = if (e < 0) css.length else e + 2
        for (j <- i until end) masked.setCharAt(j, ' ')
        i = end
      } else if (css.charAt(i) == '"' || css.charAt(i) == '\'') {
        val q = css.charAt(i)
        var j = i + 1
        while (j < css.length && css.charAt(j) != q && css.charAt(j) != '\n') j += (if (css.charAt(j) == '\\') 2 else 1)
        val end = math.min(j + 1, css.length)
        for (k <- i until end) masked.setCharAt(k, ' ')
        i = end
      } else i += 1
    }
    // { の前(セレクタ・@mediaなどの条件)だけを見る. 宣言のブロックは { から対応する } まで
    val text = masked.toString
    val out = scala.collection.mutable.ListBuffer[(String, Int)]()
    var preludeStart = 0
    var k = 0
    while (k < text.length) {
      text.charAt(k) match {
        case '{' =>
          val prelude = text.substring(preludeStart, k)
          if (!prelude.trim.startsWith("@")) {
            ClassSelector.findAllMatchIn(prelude).foreach(m => out += ((m.group(1), base + preludeStart + m.start + 1)))
          }
          // 宣言だけのブロックか(入れ子のルールを含む @media などは中を続けて読む)
          val close = matching(text, k)
          val inner = text.substring(k + 1, math.max(k + 1, close))
          if (prelude.trim.startsWith("@") && inner.contains("{")) {
            preludeStart = k + 1
            k += 1
          } else {
            k = if (close < 0) text.length else close + 1
            preludeStart = k
          }
        case '}' | ';' =>
          k += 1
          preludeStart = k
        case _ => k += 1
      }
    }
    out.toList
  }

  private def matching(text: String, open: Int): Int = {
    var depth = 0
    var i = open
    while (i < text.length) {
      if (text.charAt(i) == '{') depth += 1
      else if (text.charAt(i) == '}') {
        depth -= 1
        if (depth == 0) return i
      }
      i += 1
    }
    -1
  }

  /** .stのすべての<style>のクラスの定義 */
  def definitions(text: String): List[Definition] = {
    try {
      SfcParser.parse(new SourceText(text)).styles.flatMap { b =>
        val scoped = b.attr("scoped").isDefined
        selectors(text.substring(b.contentStart, b.contentEnd), b.contentStart).map { case (n, o) => Definition(n, o, scoped) }
      }
    } catch {
      case NonFatal(_) => Nil
    }
  }

  /**
   * <style scoped> で定義したのに、テンプレートとscriptのどこにも名前が出てこないクラス.
   * (:class の文字列や、scriptで組み立てたクラス名も「使っている」とみなすため、名前が単語として現れるかで判断します)
   */
  def unused(text: String): List[Definition] = {
    val sfc = try SfcParser.parse(new SourceText(text)) catch {
      case NonFatal(_) => return Nil
    }
    val uses = (sfc.template.toList ++ sfc.script.toList).map(b => text.substring(b.contentStart, b.contentEnd)).mkString("\n")
    definitions(text).filter(_.scoped).groupBy(_.name).toList.flatMap { case (name, defs) =>
      val word = ("(?<![\\w-])" + java.util.regex.Pattern.quote(name) + "(?![\\w-])").r
      if (word.findFirstIn(uses).isDefined) Nil else defs
    }.sortBy(_.offset)
  }

}
