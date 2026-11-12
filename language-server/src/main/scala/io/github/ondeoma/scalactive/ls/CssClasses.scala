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

  /** SCSS・Sassの、親のセレクタにつなげる & (例: .card { &-title {} } の &-title は .card-title) */
  private val ParentSuffix = """&([\w-]+)""".r

  /**
   * <style>の中身からクラスのセレクタを探します. 宣言の中(url(a.png) など)、コメント、文字列は除きます.
   * @param base 中身の先頭の、.st の中の位置
   * @param lang <style>のlang(css・scss・sass)
   */
  private[ls] def selectors(css: String, base: Int, lang: String = "css"): List[(String, Int)] = lang match {
    case "scss" => scssSelectors(mask(css, sass = true), base)
    case "sass" => sassSelectors(mask(css, sass = true), base)
    case _ => cssSelectors(mask(css, sass = false), base)
  }

  /**
   * コメントと文字列(Sassでは // のコメントと #{...} も)を同じ長さの空白にして、位置を保ったまま除きます.
   */
  private def mask(css: String, sass: Boolean): String = {
    val masked = new StringBuilder(css)
    def blank(from: Int, to: Int): Unit = for (j <- from until to) if (masked.charAt(j) != '\n') masked.setCharAt(j, ' ')
    var i = 0
    while (i < css.length) {
      if (sass && css.startsWith("//", i)) {
        val e = css.indexOf('\n', i)
        val end = if (e < 0) css.length else e
        blank(i, end)
        i = end
      } else if (sass && css.startsWith("#{", i)) {
        val e = css.indexOf('}', i + 2)
        val end = if (e < 0) css.length else e + 1
        blank(i, end)
        i = end
      } else if (css.startsWith("/*", i)) {
        val e = css.indexOf("*/", i + 2)
        val end = if (e < 0) css.length else e + 2
        blank(i, end)
        i = end
      } else if (css.charAt(i) == '"' || css.charAt(i) == '\'') {
        val q = css.charAt(i)
        var j = i + 1
        while (j < css.length && css.charAt(j) != q && css.charAt(j) != '\n') j += (if (css.charAt(j) == '\\') 2 else 1)
        val end = math.min(j + 1, css.length)
        blank(i, end)
        i = end
      } else i += 1
    }
    masked.toString
  }

  /**
   * セレクタ(prelude)のクラスと、子のルールの & が指す名前(カンマで区切った各セレクタの最後のクラス).
   * @param parents 親のルールの、& が指す名前
   * @param at      preludeの先頭の、.st の中の位置
   */
  private def ruleClasses(prelude: String, at: Int, parents: List[String]): (List[(String, Int)], List[String]) = {
    val found = ClassSelector.findAllMatchIn(prelude).map(m => (m.group(1), at + m.start + 1, m.start)) ++
      ParentSuffix.findAllMatchIn(prelude).flatMap(m => parents.map(p => (p + m.group(1), at + m.start + 1, m.start)))
    val all = found.toList.sortBy(_._3)
    var from = 0
    val lasts = prelude.split(",", -1).toList.flatMap { part =>
      val range = from until from + part.length
      from += part.length + 1
      val inPart = all.filter(c => range.contains(c._3))
      inPart.lastOption.toList.flatMap { last =>
        // .card { &-a, &-b {} } のように、親が複数の名前を指す場合は、同じ位置の名前をすべて
        all.filter(_._3 == last._3).map(_._1)
      }
    }
    (all.map(c => (c._1, c._2)), lasts.distinct)
  }

  /** SCSS: 入れ子のルールも読む */
  private def scssSelectors(text: String, base: Int): List[(String, Int)] = {
    val out = scala.collection.mutable.ListBuffer[(String, Int)]()
    // 外側から順の、& が指す名前
    var stack: List[List[String]] = List(Nil)
    var preludeStart = 0
    var k = 0
    while (k < text.length) {
      text.charAt(k) match {
        case '{' =>
          val prelude = text.substring(preludeStart, k)
          if (prelude.trim.startsWith("@")) {
            // @media・@include などの中は、親と同じ & のまま読む
            stack = stack.head :: stack
          } else {
            val (classes, lasts) = ruleClasses(prelude, base + preludeStart, stack.head)
            out ++= classes
            stack = lasts :: stack
          }
          k += 1
          preludeStart = k
        case '}' =>
          if (stack.tail.nonEmpty) stack = stack.tail
          k += 1
          preludeStart = k
        case ';' =>
          k += 1
          preludeStart = k
        case _ => k += 1
      }
    }
    out.toList
  }

  /** 宣言の行(color: red). a:hover のように、: の直後が空白でないものはセレクタとみなす */
  private val SassDeclaration = """^[\w-]+\s*:(\s|$).*""".r

  /** Sass(インデント記法): インデントで入れ子を読む */
  private def sassSelectors(text: String, base: Int): List[(String, Int)] = {
    val out = scala.collection.mutable.ListBuffer[(String, Int)]()
    // (インデント, & が指す名前)
    var stack: List[(Int, List[String])] = Nil
    var lineStart = 0
    text.split("\n", -1).foreach { line =>
      val trimmed = line.trim
      val indent = line.indexWhere(c => !c.isWhitespace)
      if (trimmed.nonEmpty) {
        stack = stack.dropWhile(_._1 >= indent)
        val parents = stack.headOption.map(_._2).getOrElse(Nil)
        if (trimmed.startsWith("@")) stack = (indent, parents) :: stack
        else if (!trimmed.startsWith("$") && !trimmed.startsWith("+") && !trimmed.startsWith("=") &&
          !SassDeclaration.pattern.matcher(trimmed).matches()) {
          val (classes, lasts) = ruleClasses(line, base + lineStart, parents)
          out ++= classes
          stack = (indent, lasts) :: stack
        }
      }
      lineStart += line.length + 1
    }
    out.toList
  }

  /** CSS: { の前(セレクタ・@mediaなどの条件)だけを見る. 宣言のブロックは { から対応する } まで */
  private def cssSelectors(text: String, base: Int): List[(String, Int)] = {
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
        val lang = b.attr("lang").flatMap(_.value).getOrElse("css")
        selectors(text.substring(b.contentStart, b.contentEnd), b.contentStart, lang).map { case (n, o) => Definition(n, o, scoped) }
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
