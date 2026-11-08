package io.github.ondeoma.scalactive.compiler

import java.util.zip.CRC32

/**
 * @param fileName    エラーメッセージや生成コードのコメントに使うファイル名(相対パス)
 * @param objectName  生成するobject名(通常はファイル名から拡張子を除いたもの)
 * @param packageName ディレクトリ構成から決まるパッケージ. <script>内のpackage宣言が優先されます.
 */
final case class StInput(source: String,
                         fileName: String,
                         objectName: String,
                         packageName: Option[String])

/**
 * @param sourceMap 生成コードと.stの位置の対応(エディタ支援用)
 */
final case class StOutput(packageName: Option[String], objectName: String, code: String, sourceMap: SourceMap = SourceMap.empty)

/**
 * .st(単一ファイルコンポーネント)をScalactiveのコンポーネントobjectへ変換します.
 */
object StCompiler {

  private val Identifier = """[A-Za-z_][A-Za-z0-9_]*"""
  private val PackageDecl = """(?s)^\s*package\s+([A-Za-z_][\w.]*)\s*;?[ \t]*(?:\r?\n|$)(.*)""".r
  private val InitState = """def\s+initState\s*(?:\(\s*([^)]*?)\s*\))?\s*(?::\s*([^=]+?))?\s*=""".r
  private val PropsDecl = """\bcase\s+class\s+Props\s*\(""".r
  private val HookDecl = """\bdef\s+(mounted|unmounted)\b\s*(?:\(\s*([^)]*?)\s*\))?""".r
  private val PropParam = """(?s)^\s*(?:@\w+(?:\([^)]*\))?\s+)*(?:(?:private|protected|override|final|val|var)\s+)*([A-Za-z_]\w*)\s*:(.+)$""".r

  def compile(in: StInput): Either[StError, StOutput] = {
    val src = new SourceText(in.source)
    try {
      Right(compileUnsafe(src, in))
    } catch {
      case e: StCompileException => Left(StError(in.fileName, src.lineOf(e.offset), e.getMessage, e.offset))
    }
  }

  /**
   * .stの <script> の case class Props(...) のパラメータ(名前と型). Propsがない・読めない場合は空.
   * エディタの補完(子コンポーネントの属性・スロット名)に使います.
   */
  def propsOf(source: String): List[(String, String)] = {
    try {
      val src = new SourceText(source)
      SfcParser.parse(src).script.toList.flatMap { b =>
        val body = b.content(src)
        PropsDecl.findFirstMatchIn(body).toList.flatMap { m =>
          findClose(body, m.end - 1).toList.flatMap { close =>
            splitTopLevel(body.substring(m.end, close), ',').filter(_.trim.nonEmpty).collect {
              case PropParam(name, rest) => (name, typeOf(rest))
            }
          }
        }
      }
    } catch {
      case _: StCompileException => Nil
    }
  }

  /** "Int = 1" から "Int" を取り出す(デフォルト値を除く). 関数型の => は区切りにしない */
  private def typeOf(rest: String): String = {
    var depth = 0
    var i = 0
    while (i < rest.length) {
      rest.charAt(i) match {
        case '"' | '\'' => i = skipString(rest, i)
        case '(' | '[' | '{' => depth += 1
        case ')' | ']' | '}' => depth -= 1
        case '=' if depth == 0 =>
          val next = if (i + 1 < rest.length) rest.charAt(i + 1) else ' '
          val prev = if (i > 0) rest.charAt(i - 1) else ' '
          if (next != '>' && next != '=' && !"=!<>".contains(prev)) return rest.substring(0, i).trim
        case _ => ()
      }
      i += 1
    }
    rest.trim
  }

  /** scoped CSS用の属性名. ファイルのパスから決まります. */
  def scopeId(fileName: String): String = {
    val crc = new CRC32
    crc.update(fileName.replace('\\', '/').getBytes("UTF-8"))
    "data-st-" + "%08x".format(crc.getValue)
  }

  private def compileUnsafe(src: SourceText, in: StInput): StOutput = {
    if (!in.objectName.matches(Identifier)) {
      src.fail(0, s"file name '${in.objectName}' is not a valid Scala identifier")
    }
    val sfc = SfcParser.parse(src)
    val marks = new Marks

    // ---- script ----
    // scriptStartは本体(package宣言を除く)の先頭の.st上のオフセット
    val (scriptPackage, scriptBody, scriptStart, scriptLine) = sfc.script match {
      case None => (None, "", 0, 0)
      case Some(b) =>
        b.attr("lang").flatMap(_.value).foreach { l =>
          if (l != "scala") src.fail(b.offset, s"unsupported script lang '$l' (only 'scala' is supported)")
        }
        val content = b.content(src)
        PackageDecl.findFirstMatchIn(content) match {
          case Some(m) => (Some(m.group(1)), m.group(2), b.contentStart + m.start(2), src.lineOf(b.offset))
          case None => (None, content, b.contentStart, src.lineOf(b.offset))
        }
    }
    val packageName = scriptPackage.orElse(in.packageName).filter(_.nonEmpty)

    // object内で定義された型は object名.型 で参照する
    def qualify(t: String): String = {
      val definedInside = s"""\\b(?:class|trait|object|enum|type)\\s+${java.util.regex.Pattern.quote(t)}\\b""".r
        .findFirstIn(scriptBody).isDefined
      if (t.matches(Identifier) && definedInside) s"${in.objectName}.$t" else t
    }

    val initState = InitState.findFirstMatchIn(scriptBody)
    val stateType: Option[String] = initState.map { m =>
      qualify(Option(m.group(2)).map(_.trim).filter(_.nonEmpty).getOrElse("State"))
    }
    val initStateTakesProps = initState.exists(m => Option(m.group(1)).exists(_.trim.nonEmpty))

    // case class Props(...) を定義すると、親から値を受け取るコンポーネントになる
    val props: Option[List[(String, String)]] = PropsDecl.findFirstMatchIn(scriptBody).map { m =>
      val scriptOffset = sfc.script.map(_.offset).getOrElse(0)
      val close = findClose(scriptBody, m.end - 1)
        .getOrElse(src.fail(scriptOffset, "unclosed parameter list of 'case class Props'"))
      splitTopLevel(scriptBody.substring(m.end, close), ',').filter(_.trim.nonEmpty).map {
        case PropParam(name, rest) => (name, s"$name: ${rest.trim}")
        case p => src.fail(scriptOffset, s"cannot parse parameter '${p.trim}' of 'case class Props'")
      }
    }
    if (props.isEmpty && initStateTakesProps) {
      src.fail(sfc.script.map(_.offset).getOrElse(0), "initState can take a parameter only when 'case class Props' is defined")
    }
    val propsType = props.map(_ => s"${in.objectName}.Props")

    // ---- template ----
    val template = sfc.template.getOrElse(src.fail(0, "<template> block is required"))
    val stateName0 = template.attr("state").flatMap(_.value).getOrElse("s")
    val propsName0 = template.attr("props").flatMap(_.value).getOrElse("props")

    // def mounted / def unmounted: ライフサイクルフック. 引数はなし、またはProps・Stateを受け取れる
    val hookCalls: List[String] = HookDecl.findAllMatchIn(scriptBody).toList.groupBy(_.group(1)).toList.sortBy(_._1).map {
      case (name, ms) =>
        val m = ms.head
        val scriptOffset = sfc.script.map(_.offset).getOrElse(0)
        val args = Option(m.group(2)) match {
          case None => ""
          case Some(ps) =>
            val types = splitTopLevel(ps, ',').filter(_.trim.nonEmpty).map {
              case PropParam(_, t) => t.trim
              case p => src.fail(scriptOffset, s"cannot parse parameter '${p.trim}' of 'def $name'")
            }
            val stateTypes = stateType.toList.flatMap(t => List(t, t.stripPrefix(s"${in.objectName}.")))
            val propsTypes = propsType.toList.flatMap(t => List(t, "Props"))
            types.map {
              case t if stateTypes.contains(t) => stateName0
              case t if propsTypes.contains(t) => propsName0
              case t => src.fail(scriptOffset, s"'def $name' can take only Props and/or the state (found '$t')")
            }.mkString("(", ", ", ")")
        }
        val fn = if (name == "mounted") "onMounted" else "onUnmounted"
        s"__CM.$fn($name$args)"
    }

    val stateName = stateName0
    if (!stateName.matches(Identifier)) src.fail(template.offset, s"invalid state name '$stateName'")
    val propsName = propsName0
    if (!propsName.matches(Identifier)) src.fail(template.offset, s"invalid props name '$propsName'")
    if (props.isDefined && stateType.isDefined && propsName == stateName) {
      src.fail(template.offset, s"props name and state name must differ ('$propsName')")
    }
    val scoped = sfc.styles.exists(_.has("scoped"))
    val scopeAttr = if (scoped) Some(scopeId(in.fileName)) else None
    val nodes = HtmlParser.parse(src, template.contentStart, template.contentEnd)
    val trimmed = trimEdges(nodes)
    val slotProps = props.map(ps => (propsName, ps.map(_._1).toSet))
    val htmlExpr = new TemplateCodegen(src, in.fileName, scopeAttr, slotProps, marks).generate(trimmed)

    // ---- style ----
    val css = sfc.styles.map { b =>
      b.attr("lang").flatMap(_.value).foreach { l =>
        if (l != "css") src.fail(b.offset, s"unsupported style lang '$l' (only 'css' is supported)")
      }
      val c = b.content(src)
      if (b.has("scoped")) CssScoper.scope(c, scopeId(in.fileName)) else c
    }.mkString("\n").trim

    // ---- 出力 ----
    val sb = new StringBuilder
    sb ++= s"// Generated by sbt-scalactive from ${in.fileName}. DO NOT EDIT.\n"
    packageName.foreach(p => sb ++= s"package $p\n")
    sb ++=
      """
        |import io.github.ondeoma.scalactive.components.*
        |import io.github.ondeoma.scalactive.reactive.*
        |import org.scalajs.dom.*
        |import io.github.ondeoma.scalactive.components.ComponentManager as __CM
        |import io.github.ondeoma.scalactive.st.StRuntime as __st
        |
        |""".stripMargin
    (propsType, stateType) match {
      case (Some(p), Some(t)) => sb ++= s"object ${in.objectName} extends StatefulPropsComponent[$p, $t] {\n"
      case (Some(p), None) => sb ++= s"object ${in.objectName} extends StatelessPropsComponent[$p] {\n"
      case (None, Some(t)) => sb ++= s"object ${in.objectName} extends StatefulComponent[$t] {\n"
      case (None, None) => sb ++= s"object ${in.objectName} extends StatelessComponent {\n"
    }
    if (scriptBody.trim.nonEmpty) {
      sb ++= s"\n  // ---- <script> ${in.fileName}:$scriptLine ----\n"
      sb ++= dedent(Located(scriptBody, scriptStart)).map(l => if (l.isEmpty) "" else "  " + marks.mark(l)).mkString("\n")
      sb ++= "\n"
    }
    if (css.nonEmpty) {
      sb ++= s"\n  // ---- <style> ----\n"
      sb ++= s"  __st.injectStyle(${StringLiteral(scopeId(in.fileName))}, ${StringLiteral(css + "\n")})\n"
    }
    (props, propsType) match {
      case (Some(ps), Some(p)) =>
        sb ++= "\n  // ---- props ----\n"
        sb ++= s"  def apply(${ps.map(_._2).mkString(", ")})\n"
        sb ++= "           (implicit __parent: HTMLElement, __am: AddMethod): NodesComponentController = {\n"
        sb ++= s"    apply(Props(${ps.map { case (n, _) => s"$n = $n" }.mkString(", ")}))\n"
        sb ++= "  }\n"
        (stateType, initStateTakesProps) match {
          case (Some(t), false) => sb ++= s"\n  def initState(props: $p): $t = initState()\n"
          case _ => ()
        }
      case _ => ()
    }
    sb ++= s"\n  // ---- <template> ${in.fileName}:${src.lineOf(template.offset)} ----\n"
    (propsType, stateType) match {
      case (Some(p), Some(t)) => sb ++= s"  def genHtml($propsName: $p, $stateName: $t)(implicit __cm0: __CM): String = {\n"
      case (Some(p), None) => sb ++= s"  def genHtml($propsName: $p)(implicit __cm0: __CM): String = {\n"
      case (None, Some(t)) => sb ++= s"  def genHtml($stateName: $t)(implicit __cm0: __CM): String = {\n"
      case (None, None) => sb ++= s"  def genHtml(implicit __cm0: __CM): String = {\n"
    }
    hookCalls.foreach(h => sb ++= s"    $h\n")
    sb ++= s"    $htmlExpr\n"
    sb ++= "  }\n"
    sb ++= "\n}\n"
    val (code, sourceMap) = marks.extract(sb.toString)
    StOutput(packageName, in.objectName, code, sourceMap)
  }

  /**
   * openの位置の開き括弧に対応する閉じ括弧の位置を返します.
   * 文字列リテラル内の括弧は無視します.
   */
  private def findClose(s: String, open: Int): Option[Int] = {
    var depth = 0
    var i = open
    while (i < s.length) {
      s.charAt(i) match {
        case '"' | '\'' =>
          i = skipString(s, i)
        case '(' | '[' | '{' =>
          depth += 1
        case ')' | ']' | '}' =>
          depth -= 1
          if (depth == 0) return Some(i)
        case _ => ()
      }
      i += 1
    }
    None
  }

  /** startの引用符で始まる文字列/文字リテラルの閉じ引用符の位置を返します. */
  private def skipString(s: String, start: Int): Int = {
    val q = s.charAt(start)
    var i = start + 1
    while (i < s.length && s.charAt(i) != q) {
      if (s.charAt(i) == '\\') i += 1
      i += 1
    }
    i
  }

  /** 括弧・文字列の外側にあるsepで分割します. */
  private def splitTopLevel(s: String, sep: Char): List[String] = {
    val out = scala.collection.mutable.ListBuffer[String]()
    var depth = 0
    var last = 0
    var i = 0
    while (i < s.length) {
      s.charAt(i) match {
        case '"' | '\'' => i = skipString(s, i)
        case '(' | '[' | '{' => depth += 1
        case ')' | ']' | '}' => depth -= 1
        case c if c == sep && depth == 0 =>
          out += s.substring(last, i)
          last = i + 1
        case _ => ()
      }
      i += 1
    }
    out += s.substring(last)
    out.toList
  }

  /** テンプレート先頭と末尾の空白テキストを除去します. */
  private def trimEdges(nodes: List[HtmlNode]): List[HtmlNode] = {
    def blank(n: HtmlNode) = n match {
      case Text(t, _) => t.trim.isEmpty
      case _ => false
    }
    nodes.dropWhile(blank).reverse.dropWhile(blank).reverse
  }

  /** 前後の空行を除き、共通のインデントを取り除いた行のリストを返します(空白だけの行は空になります). */
  private def dedent(s: Located): List[Located] = {
    val lines = {
      val b = List.newBuilder[Located]
      var start = 0
      var i = 0
      while (i <= s.text.length) {
        if (i == s.text.length || s.text.charAt(i) == '\n') {
          val end = if (i > start && s.text.charAt(i - 1) == '\r') i - 1 else i
          b += s.substring(start, end)
          start = i + 1
        }
        i += 1
      }
      b.result()
    }
    def blank(l: Located) = l.text.trim.isEmpty
    val trimmed = lines.dropWhile(blank).reverse.dropWhile(blank).reverse
    val margin = trimmed.filterNot(blank).map(_.text.takeWhile(c => c == ' ' || c == '\t').length) match {
      case Nil => 0
      case ms => ms.min
    }
    trimmed.map(l => if (blank(l)) Located("", 0) else l.substring(margin))
  }

}
