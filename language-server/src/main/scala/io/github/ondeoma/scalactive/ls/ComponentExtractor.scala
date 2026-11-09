package io.github.ondeoma.scalactive.ls

import io.github.ondeoma.scalactive.compiler.{Element, HtmlNode, HtmlParser, SfcParser, SourceText, Text}

import java.nio.file.{Files, Path}
import scala.util.control.NonFatal

/**
 * コンポーネントの抽出: テンプレートの選んだ範囲を新しい.stに切り出し、元の場所をそのタグに置き換えます.
 *
 *  - 選んだ範囲の外で定義された値(s・props・外側の v-for の変数など)は、新しいコンポーネントの props にし、
 *    範囲の中の参照を `props.名前` に書き換えます(型はpresentation compilerのホバーから求めます).
 *  - 元のコンポーネントのscriptで定義したもの(def・val など)は `元の名前.名前` で参照します.
 *  - 範囲の中で定義した変数(v-forの変数、ラムダの引数など)やライブラリの名前はそのままです.
 *  - 選んだ範囲で使っているクラスの、<style scoped> のルールを新しいコンポーネントにも写します.
 *
 * Scalaの名前の解決はStLanguageServiceがpresentation compilerに問い合わせ、その結果(Resolution)を受け取ります.
 */
object ComponentExtractor {

  /** 選んだ範囲の中の、Scalaの名前の出現(offsetは.stの中の位置) */
  final case class Ident(name: String, offset: Int, inEventHandler: Boolean)

  /** 名前の解決の結果 */
  sealed trait Resolution
  /** 範囲の外で定義された値. 新しいコンポーネントのpropにする(tpeは型) */
  final case class External(tpe: String) extends Resolution
  /** 元のコンポーネントのscriptで定義したもの */
  case object ScriptMember extends Resolution
  /** 範囲の中で定義したもの・ライブラリなど(そのまま) */
  case object Keep extends Resolution

  /** 抽出の結果 */
  final case class Result(file: Path, content: String, replaceStart: Int, replaceEnd: Int, replacement: String)

  private val keywords = Set(
    "if", "then", "else", "match", "case", "true", "false", "null", "new", "this", "super", "val", "var", "def",
    "for", "yield", "do", "while", "return", "throw", "try", "catch", "finally", "_", "import", "given", "using", "in")

  private val IdentRe = "(?<![\\w.$])[A-Za-z_][A-Za-z0-9_]*".r

  /** 文字列リテラルを同じ長さの空白にする(中の名前を拾わないため) */
  private def maskStrings(s: String): String = {
    val sb = new StringBuilder(s)
    var i = 0
    while (i < s.length) {
      val c = s.charAt(i)
      if (c == '"') {
        var j = i + 1
        while (j < s.length && s.charAt(j) != '"') j += (if (s.charAt(j) == '\\') 2 else 1)
        for (k <- i until math.min(j + 1, s.length)) sb.setCharAt(k, ' ')
        i = j + 1
      } else if (c == '\'' && i + 2 < s.length && (s.charAt(i + 2) == '\'' || s.charAt(i + 1) == '\\')) {
        val e = s.indexOf('\'', i + 2)
        for (k <- i until (if (e < 0) s.length else e + 1)) sb.setCharAt(k, ' ')
        i = if (e < 0) s.length else e + 1
      } else i += 1
    }
    sb.toString
  }

  /**
   * 範囲[start, end)が、テンプレートの中の完結したHTML(タグがすべて閉じている)なら、その前後の空白を除いた範囲を返します.
   */
  def selection(text: String, start: Int, end: Int): Option[(Int, Int)] = {
    try {
      val src = new SourceText(text)
      val t = SfcParser.parse(src).template.getOrElse(return None)
      var s = math.max(start, t.contentStart)
      var e = math.min(end, t.contentEnd)
      while (s < e && text.charAt(s).isWhitespace) s += 1
      while (e > s && text.charAt(e - 1).isWhitespace) e -= 1
      if (s >= e || start < t.contentStart || end > t.contentEnd) return None
      val nodes = HtmlParser.parse(src, s, e)
      // 少なくとも1つの要素を含み、属性値やタグの途中で切れていない
      if (nodes.exists(_.isInstanceOf[Element]) && text.charAt(s) == '<' && text.charAt(e - 1) == '>') Some((s, e)) else None
    } catch {
      case NonFatal(_) => None
    }
  }

  /** 範囲の中のScalaの名前と、範囲の中で導入される変数(v-for・スロットの変数) */
  def identifiers(text: String, start: Int, end: Int): (List[Ident], Set[String]) = {
    val src = new SourceText(text)
    val nodes = HtmlParser.parse(src, start, end)
    val idents = scala.collection.mutable.ListBuffer[Ident]()
    val introduced = scala.collection.mutable.Set[String]()
    def scan(code: String, base: Int, event: Boolean): Unit = {
      IdentRe.findAllMatchIn(maskStrings(code)).foreach { m =>
        // 名前付き引数(f(x = 1) の x)は除く
        val after = code.substring(m.end).dropWhile(_ == ' ')
        val namedArg = after.startsWith("=") && !after.startsWith("==") && !after.startsWith("=>")
        if (!keywords.contains(m.matched) && !namedArg) idents += Ident(m.matched, base + m.start, event)
      }
    }
    def walk(ns: List[HtmlNode]): Unit = ns.foreach {
      case Text(t, offset) =>
        "\\{\\{(.*?)\\}\\}".r.findAllMatchIn(t).foreach(m => scan(m.group(1), offset + m.start(1), event = false))
      case e: Element =>
        e.attrs.foreach { a =>
          a.value.foreach { v =>
            val base = a.valueOffset
            if (a.name == "v-for") {
              val in = v.indexOf(" in ")
              if (in >= 0) {
                IdentRe.findAllMatchIn(v.substring(0, in)).foreach(m => introduced += m.matched)
                scan(v.substring(in + 4), base + in + 4, event = false)
              }
            } else if (a.name.startsWith("#") || a.name.startsWith("v-slot")) {
              IdentRe.findAllMatchIn(v).foreach(m => introduced += m.matched)
            } else if (a.name.startsWith("@") || a.name.startsWith("v-on:")) scan(v, base, event = true)
            else if (a.name.startsWith(":") || a.name.startsWith("v-") || a.name == "ref") scan(v, base, event = false)
          }
        }
        walk(e.children)
      case _ =>
    }
    walk(nodes)
    (idents.toList, introduced.toSet)
  }

  /** 同じディレクトリで使われていない名前(Extracted, Extracted2, ...) */
  def freshName(dir: Path): String = {
    Iterator.from(1).map(i => if (i == 1) "Extracted" else s"Extracted$i").find(n => !Files.exists(dir.resolve(n + ".st"))).get
  }

  /** 型の中の、元のコンポーネントのscriptで定義した型の名前を `元の名前.型` にする */
  def qualify(tpe: String, scriptTypes: Set[String], parent: String): String = {
    "(?<![\\w.])[A-Z][A-Za-z0-9_]*".r.replaceAllIn(tpe, m =>
      java.util.regex.Matcher.quoteReplacement(if (scriptTypes.contains(m.matched)) s"$parent.${m.matched}" else m.matched))
  }

  /** scriptで定義した型の名前 */
  def scriptTypes(script: String): Set[String] = {
    "\\b(?:case\\s+class|class|trait|object|enum|type)\\s+([A-Z][A-Za-z0-9_]*)".r.findAllMatchIn(script).map(_.group(1)).toSet
  }

  /** 選んだ範囲で使っているクラスの、<style scoped> のトップレベルのルール */
  def styleRules(text: String, usedText: String): List[String] = {
    try {
      val sfc = SfcParser.parse(new SourceText(text))
      sfc.styles.filter(_.attr("scoped").isDefined).flatMap { b =>
        val css = text.substring(b.contentStart, b.contentEnd)
        val rules = scala.collection.mutable.ListBuffer[String]()
        var start = 0
        var depth = 0
        for (i <- css.indices) css.charAt(i) match {
          case '{' => depth += 1
          case '}' =>
            depth -= 1
            if (depth == 0) {
              rules += css.substring(start, i + 1).trim
              start = i + 1
            }
          case _ =>
        }
        rules.toList.filter { r =>
          val prelude = r.takeWhile(_ != '{')
          "\\.(-?[_a-zA-Z][\\w-]*)".r.findAllMatchIn(prelude).exists { m =>
            ("(?<![\\w-])" + java.util.regex.Pattern.quote(m.group(1)) + "(?![\\w-])").r.findFirstIn(usedText).isDefined
          }
        }
      }
    } catch {
      case NonFatal(_) => Nil
    }
  }

  /**
   * 抽出の結果を作ります.
   *
   * @param resolve 名前の出現 → 解決の結果(presentation compilerへの問い合わせ)
   */
  def extract(text: String, stFile: Path, start: Int, end: Int, resolve: Ident => Resolution): Either[String, Result] = {
    val (s, e) = selection(text, start, end).getOrElse(return Left("Select complete elements in the template to extract a component"))
    val parent = stFile.getFileName.toString.stripSuffix(".st")
    val sfc = SfcParser.parse(new SourceText(text))
    val script = sfc.script.map(b => text.substring(b.contentStart, b.contentEnd)).getOrElse("")
    val types = scriptTypes(script)
    val (idents, introduced) = identifiers(text, s, e)

    // 名前ごとに解決する(同じ名前は同じ扱い. 範囲の中で導入した変数・イベントハンドラーの event はそのまま)
    val decisions = scala.collection.mutable.LinkedHashMap[String, Resolution]()
    idents.foreach { id =>
      if (!decisions.contains(id.name)) {
        val r =
          if (introduced.contains(id.name) || (id.inEventHandler && id.name == "event")) Keep
          else resolve(id)
        decisions(id.name) = r
      }
    }
    val props = decisions.toList.collect { case (n, External(t)) => (n, if (n == "props") "parentProps" else n, qualify(t, types, parent)) }
    val members = decisions.collect { case (n, ScriptMember) => n }.toSet

    // 範囲の中の参照を書き換える(後ろから)
    val propNames = props.map(p => p._1 -> p._2).toMap
    val edits = idents.flatMap { id =>
      decisions.get(id.name) match {
        case Some(External(_)) => Some((id.offset, id.name.length, "props." + propNames(id.name)))
        case Some(ScriptMember) => Some((id.offset, id.name.length, s"$parent.${id.name}"))
        case _ => None
      }
    }.sortBy(-_._1)
    val body = edits.foldLeft(text.substring(s, e)) { case (acc, (offset, len, repl)) =>
      val o = offset - s
      acc.substring(0, o) + repl + acc.substring(o + len)
    }
    // 元のインデントをそろえ、<template>の中で2スペースにする
    val firstIndent = {
      val lineStart = text.lastIndexOf('\n', s - 1) + 1
      s - lineStart
    }
    val lines = body.split("\n", -1).toList
    val dedented = lines.head :: lines.tail.map(l => if (l.take(firstIndent).forall(_ == ' ')) l.drop(firstIndent) else l.dropWhile(_ == ' '))
    val templateBody = dedented.map(l => if (l.isEmpty) l else "  " + l).mkString("\n")

    val name = freshName(stFile.getParent)
    val propsDecl =
      if (props.isEmpty) ""
      else {
        val pad = " " * "case class Props(".length
        props.map { case (_, n, t) => s"$n: $t" }.mkString("case class Props(", ",\n" + pad, ")") + "\n"
      }
    val rules = styleRules(text, body)
    val content =
      s"""<script lang="scala">
         |$propsDecl</script>
         |
         |<template>
         |$templateBody
         |</template>
         |""".stripMargin + (if (rules.isEmpty) "" else rules.mkString("\n<style scoped>\n", "\n", "\n</style>\n"))
    val attrs = props.map { case (orig, n, _) => s""" :$n="$orig"""" }.mkString
    Right(Result(stFile.resolveSibling(name + ".st"), content, s, e, s"<$name$attrs />"))
  }

}
