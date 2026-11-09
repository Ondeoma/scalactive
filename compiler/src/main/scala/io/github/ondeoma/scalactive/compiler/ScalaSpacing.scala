package io.github.ondeoma.scalactive.compiler

/**
 * <script>(Scala)の空白の整理(StFormatterから使う). インデントと改行は変えず、行の途中の空白だけを整えます.
 *
 *  - 連続した空白を1つにする: `val a   = 1` → `val a = 1`
 *  - `(` `[` の直後と、`)` `]` `,` の直前の空白を取り除く: `A( 10   )` → `A(10)`
 *  - 行頭のインデント、文字列(`"..."`・`"""..."""`・補間文字列)、文字リテラル、コメントの中は変えない
 *
 * 文字列やコメントが閉じていないなど読めないときは None を返します.
 */
object ScalaSpacing {

  def format(content: String): Option[String] = {
    val sb = new StringBuilder
    val n = content.length
    var i = 0
    var lineStart = true // 行頭のインデントの中

    def isSpace(c: Char) = c == ' ' || c == '\t'

    while (i < n) {
      val c = content.charAt(i)
      if (c == '\n') {
        sb += c
        lineStart = true
        i += 1
      } else if (isSpace(c)) {
        var j = i
        while (j < n && isSpace(content.charAt(j))) j += 1
        if (lineStart) sb ++= content.substring(i, j)
        else {
          val next = if (j < n) content.charAt(j) else '\n'
          val prev = if (sb.nonEmpty) sb.last else '\n'
          val drop = next == '\n' || next == ')' || next == ']' || next == ',' || prev == '(' || prev == '['
          // 行末のコメントの前の空白は、位置をそろえていることがあるので残す
          if (content.startsWith("//", j)) sb ++= content.substring(i, j)
          else if (!drop) sb += ' '
        }
        i = j
      } else {
        lineStart = false
        val end = opaqueEnd(content, i)
        end match {
          case Some(e) =>
            if (e < 0) return None
            sb ++= content.substring(i, e)
            // 複数行の文字列・コメントの後は、行頭ではない
            i = e
          case None =>
            sb += c
            i += 1
        }
      }
    }
    Some(sb.toString)
  }

  /**
   * iから始まる、中身を変えない部分(文字列・文字リテラル・コメント)の終わり. そうでなければ None、閉じていなければ Some(-1)
   */
  private def opaqueEnd(s: String, i: Int): Option[Int] = {
    val c = s.charAt(i)
    if (s.startsWith("//", i)) {
      val e = s.indexOf('\n', i)
      Some(if (e < 0) s.length else e)
    } else if (s.startsWith("/*", i)) {
      // Scalaのブロックコメントは入れ子にできる
      var depth = 0
      var j = i
      while (j < s.length) {
        if (s.startsWith("/*", j)) { depth += 1; j += 2 }
        else if (s.startsWith("*/", j)) {
          depth -= 1
          j += 2
          if (depth == 0) return Some(j)
        } else j += 1
      }
      Some(-1)
    } else if (s.startsWith("\"\"\"", i)) {
      val e = s.indexOf("\"\"\"", i + 3)
      if (e < 0) Some(-1)
      else {
        // """...""""" のように、閉じる引用符の後に続く " も文字列に含む
        var j = e + 3
        while (j < s.length && s.charAt(j) == '"') j += 1
        Some(j)
      }
    } else if (c == '"') {
      var j = i + 1
      while (j < s.length && s.charAt(j) != '"' && s.charAt(j) != '\n') {
        if (s.charAt(j) == '\\') j += 1
        j += 1
      }
      if (j >= s.length || s.charAt(j) == '\n') Some(-1) else Some(j + 1)
    } else if (c == '\'') {
      // 文字リテラル('a'、'\n'、'A')だけを扱い、シンボルや引用('x、'{ ... })はそのまま
      val m = "^'(\\\\u[0-9a-fA-F]{4}|\\\\.|[^'\\\\\\n])'".r.findPrefixOf(s.substring(i, math.min(s.length, i + 8)))
      m.map(t => i + t.length)
    } else None
  }

}
