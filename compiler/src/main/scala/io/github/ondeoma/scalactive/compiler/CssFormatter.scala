package io.github.ondeoma.scalactive.compiler

import scala.collection.mutable.ListBuffer

/**
 * <style>のCSSの整形(StFormatterから使う).
 *
 *  - 1行で書いたルールは1行のまま、空白だけ整えます: `.a{color:red}` → `.a { color: red; }`
 *  - 複数行にまたがるルールは、宣言を1行ずつ2スペースでインデントします(@mediaなどの入れ子も同じ)
 *  - `プロパティ: 値` の空白、セレクタ・値の中の連続した空白を整え、ルールの最後の宣言に ; を補います
 *  - 空行は1行まで残し、コメントは書いた位置(前の行の末尾か、単独の行)に残します
 *  - 文字列・コメント・括弧の中は変えません
 * 文字列やコメントが閉じていない、波括弧の対応が取れないなど、読めないときは None を返します.
 */
object CssFormatter {

  private val indentUnit = "  "

  private sealed trait Node {
    def start: Int
    def end: Int
  }
  /** 宣言、または @import などの ; で終わる文. textは ; を含まない */
  private final case class Stmt(text: String, start: Int, end: Int) extends Node
  private final case class Comment(text: String, start: Int, end: Int) extends Node
  private final case class Rule(selector: String, start: Int, end: Int, children: List[Node]) extends Node

  private final class Fail extends RuntimeException(null, null, false, false)

  def format(content: String): Option[String] = {
    try {
      val nodes = new Parser(content).parseAll()
      val out = ListBuffer[String]()
      emit(content, nodes, 0, out)
      Some(if (out.isEmpty) "\n" else out.mkString("\n", "\n", "\n"))
    } catch {
      case _: Fail => None
    }
  }

  private final class Parser(s: String) {
    private var i = 0

    def parseAll(): List[Node] = {
      val nodes = parseItems(inRule = false)
      if (i < s.length) throw new Fail // 対応しない }
      nodes
    }

    /** } (inRuleのとき)か末尾までの要素を読む. 戻ったとき、iは } の位置 */
    private def parseItems(inRule: Boolean): List[Node] = {
      val nodes = ListBuffer[Node]()
      val pending = new StringBuilder
      var pendingStart = -1
      var parens = 0

      def add(str: String, at: Int): Unit = {
        if (pendingStart < 0 && str.trim.nonEmpty) pendingStart = at + (str.length - str.dropWhile(_.isWhitespace).length)
        pending ++= str
      }
      def flushStmt(end: Int): Unit = {
        if (pending.toString.trim.nonEmpty) nodes += Stmt(pending.toString, pendingStart, end)
        pending.clear()
        pendingStart = -1
      }

      while (i < s.length) {
        val c = s.charAt(i)
        if (c == '"' || c == '\'') {
          val end = stringEnd(i)
          add(s.substring(i, end), i)
          i = end
        } else if (s.startsWith("/*", i)) {
          val close = s.indexOf("*/", i + 2)
          if (close < 0) throw new Fail
          val text = s.substring(i, close + 2)
          // 宣言・セレクタの途中のコメントは、その一部として残す
          if (pending.toString.trim.nonEmpty) add(text, i) else nodes += Comment(text, i, close + 2)
          i = close + 2
        } else if (parens > 0) {
          if (c == '(') parens += 1 else if (c == ')') parens -= 1
          add(c.toString, i)
          i += 1
        } else if (c == '(') {
          parens += 1
          add(c.toString, i)
          i += 1
        } else if (c == ';') {
          flushStmt(i + 1)
          i += 1
        } else if (c == '{') {
          if (pending.toString.trim.isEmpty) throw new Fail // セレクタのないブロック
          val selector = pending.toString
          val start = pendingStart
          pending.clear()
          pendingStart = -1
          i += 1
          val children = parseItems(inRule = true)
          if (i >= s.length) throw new Fail // 閉じていないブロック
          i += 1
          nodes += Rule(selector, start, i, children)
        } else if (c == '}') {
          if (!inRule) throw new Fail
          flushStmt(i)
          return nodes.toList
        } else {
          add(c.toString, i)
          i += 1
        }
      }
      if (parens > 0) throw new Fail
      flushStmt(s.length)
      nodes.toList
    }

    private def stringEnd(from: Int): Int = {
      val q = s.charAt(from)
      var j = from + 1
      while (j < s.length && s.charAt(j) != q) {
        if (s.charAt(j) == '\\') j += 1
        else if (s.charAt(j) == '\n') throw new Fail
        j += 1
      }
      if (j >= s.length) throw new Fail
      j + 1
    }
  }

  /** 文字列の外の連続した空白を1つにする */
  private def collapse(text: String): String = {
    val sb = new StringBuilder
    var i = 0
    var space = false
    while (i < text.length) {
      val c = text.charAt(i)
      if (c == '"' || c == '\'') {
        var j = i + 1
        while (j < text.length && text.charAt(j) != c) {
          if (text.charAt(j) == '\\') j += 1
          j += 1
        }
        val end = math.min(j + 1, text.length)
        if (space && sb.nonEmpty) sb += ' '
        space = false
        sb ++= text.substring(i, end)
        i = end
      } else if (c.isWhitespace) {
        space = true
        i += 1
      } else {
        if (space && sb.nonEmpty) sb += ' '
        space = false
        sb += c
        i += 1
      }
    }
    sb.toString
  }

  private val declaration = "^(--[\\w-]+|-?[A-Za-z_][\\w-]*)\\s*:\\s*(.*)$".r

  /** 宣言の `プロパティ: 値` の空白を整える(@ではじまる文はそのまま) */
  private def statement(text: String): String = {
    val t = collapse(text)
    if (t.startsWith("@")) t
    else t match {
      case declaration(prop, value) => if (value.isEmpty) prop + ":" else prop + ": " + value
      case _ => t
    }
  }

  private def render(n: Node): String = n match {
    case Stmt(t, _, _) => statement(t) + ";"
    case Comment(t, _, _) => t
    case r: Rule => collapse(r.selector) + " { " + r.children.map(render).mkString(" ") + " }"
  }

  private def emit(src: String, nodes: List[Node], depth: Int, out: ListBuffer[String]): Unit = {
    val indent = indentUnit * depth
    var prev: Option[Node] = None
    nodes.foreach { n =>
      val gap = prev.map(p => src.substring(p.end, n.start)).getOrElse("")
      val sameLine = prev.isDefined && !gap.contains('\n')
      if (prev.isDefined && gap.count(_ == '\n') >= 2) out += ""
      n match {
        case c: Comment if sameLine && out.nonEmpty =>
          // 前の行の末尾のコメント
          out(out.length - 1) = out.last + " " + c.text
        case c: Comment =>
          out += indent + c.text
        case st: Stmt =>
          out += indent + statement(st.text) + ";"
        case r: Rule =>
          val selector = collapse(r.selector)
          val oneLine = !src.substring(r.start, r.end).contains('\n') && r.children.forall(!_.isInstanceOf[Rule])
          if (r.children.isEmpty) out += indent + selector + " {}"
          else if (oneLine) out += indent + render(r)
          else {
            out += indent + selector + " {"
            emit(src, r.children, depth + 1, out)
            out += indent + "}"
          }
      }
      prev = Some(n)
    }
  }

}
