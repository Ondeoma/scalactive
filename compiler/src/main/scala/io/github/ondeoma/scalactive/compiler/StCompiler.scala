package io.github.ondeoma.scalactive.compiler

import java.util.zip.CRC32
import scala.collection.mutable.ListBuffer

/**
 * @param fileName    エラーメッセージや生成コードのコメントに使うファイル名(相対パス)
 * @param objectName  生成するobject名(通常はファイル名から拡張子を除いたもの)
 * @param packageName ディレクトリ構成から決まるパッケージ. <script>内のpackage宣言が優先されます.
 * @param styleCompiler <style lang="scss"> などをCSSへ変換するもの.
 *                      Noneの場合(言語サーバーなど、CSSを使わない場合)は、それらの<style>を出力しません.
 * @param domNames    `import org.scalajs.dom.*` で使えるようになる名前(DomNames). 指定すると、使っている場合だけimportします.
 *                    Noneの場合は常にimportします.
 * @param siblings    同じディレクトリ(パッケージ)にある.stのobject名. テンプレートのタグからは完全修飾名で参照し、
 *                    組み込みのコンポーネント(RouterLinkなど)と同じ名前でも、利用者のものが使われるようにします.
 */
/**
 * Propsのパラメータ.
 *
 * @param offset .stの中のパラメータ名の位置
 */
final case class PropInfo(name: String, tpe: String, hasDefault: Boolean, offset: Int)

final case class StInput(source: String,
                         fileName: String,
                         objectName: String,
                         packageName: Option[String],
                         styleCompiler: Option[StyleCompiler] = None,
                         siblings: Set[String] = Set.empty,
                         domNames: Option[Set[String]] = None)

/**
 * @param sourceMap 生成コードと.stの位置の対応(エディタ支援用)
 */
final case class StOutput(packageName: Option[String], objectName: String, code: String, sourceMap: SourceMap = SourceMap.empty)

/**
 * .st(単一ファイルコンポーネント)をScalactiveのコンポーネントobjectへ変換します.
 */
object StCompiler {

  private val Identifier = """[A-Za-z_][A-Za-z0-9_]*"""
  /**
   * io.github.ondeoma.scalactive.reactive の公開している型・object. .stの中にどれかがあるときだけ reactive.* をimportします.
   * (値・関数(RV(0)・computedなど)はBaseComponentのexportで使えますが、型(RV[Int]など)にはimportが要ります.
   *  reactiveパッケージに公開の型を追加したら、ここにも追加します)
   */
  private val ReactiveNames = Set("CRV", "RMCompatible", "RMConversion", "RMList", "RV", "RVList", "Reactive", "ReactiveFunctions",
    "ReactiveList", "ReactiveModel", "ReactiveModelMacro", "ReactiveModelOf", "WatchHandle")

  /** 名前だけで参照している識別子(`a.b` の `b` のようなメンバーの選択は除く) */
  private val WordPattern = """(?<![.\w])[A-Za-z_][A-Za-z0-9_]*""".r

  /**
   * import文(`import a.b.{C, D}`・`import a.B, c.D` など. 選択子の{}は複数行でもよい).
   * scriptでimportした名前は、ヘッダーのimport(dom.*・reactive.*)ではなくそちらが使われるので、使っている名前に数えません.
   */
  private val ImportStmt = """\bimport\s+(?:[^\n{;]|\{[^}]*\})*""".r

  /** import文で、名前を指定して使えるようにした名前(`a.B` の B、`a.{B, C as D}` の B・D). ワイルドカード・givenは含めない */
  private[compiler] def importedNames(stmt: String): Set[String] = {
    val body = stmt.trim.stripPrefix("import").trim
    // {}の外のカンマで区切る
    val clauses = ListBuffer.empty[String]
    var depth = 0
    var start = 0
    for (i <- body.indices) body.charAt(i) match {
      case '{' => depth += 1
      case '}' => depth -= 1
      case ',' if depth == 0 => clauses += body.substring(start, i); start = i + 1
      case _ =>
    }
    clauses += body.substring(start)
    def local(sel: String): Option[String] = sel.trim.split("""\s+as\s+|\s*=>\s*""") match {
      case Array(_, to) => Some(to.trim).filter(_ != "_")
      case Array(n) => Some(n.trim).filter(n => n != "*" && n != "_" && n != "given" && !n.startsWith("given "))
      case _ => None
    }
    clauses.toList.flatMap { c =>
      val t = c.trim
      val brace = t.indexOf('{')
      if (brace >= 0) t.substring(brace + 1).stripSuffix("}").split(',').toList.flatMap(local)
      else local(t.substring(t.lastIndexOf('.') + 1)).toList
    }.filter(_.matches(Identifier)).toSet
  }

  private val PackageDecl = """(?s)^\s*package\s+([A-Za-z_][\w.]*)\s*;?[ \t]*(?:\r?\n|$)(.*)""".r
  private val InitState = """def\s+initState\s*(?:\(\s*([^)]*?)\s*\))?\s*(?::\s*([^=]+?))?\s*=""".r
  private val PropsDecl = """\bcase\s+class\s+Props\s*\(""".r
  private val HookDecl = """\bdef\s+(mounted|unmounted)\b\s*(?:\(\s*([^)]*?)\s*\))?""".r
  private val PropParam = """(?s)^\s*(?:@\w+(?:\([^)]*\))?\s+)*(?:(?:private|protected|override|final|val|var)\s+)*([A-Za-z_]\w*)\s*:(.+)$""".r

  /**
   * 文字列リテラル・文字リテラル・コメントの中身を空白にした文字列(長さと改行の位置は同じ).
   * scriptから initState・Props・ライフサイクルフックの定義を探すときに、コードサンプルの文字列などの中を拾わないために使います.
   */
  /**
   * 使っている名前を探すためのコード. コメントと文字列リテラルの中を空白にしますが、
   * 埋め込みのある文字列(s"...${x}"など. 直前が識別子の文字)の中はコードがあり得るので残します.
   */
  private[compiler] def codeForWords(s: String): String = codeOnly(s, keepInterpolated = true)

  private[compiler] def codeOnly(s: String): String = codeOnly(s, keepInterpolated = false)

  private def codeOnly(s: String, keepInterpolated: Boolean): String = {
    val sb = new StringBuilder(s)
    def interpolated(quote: Int): Boolean = quote > 0 && (s.charAt(quote - 1).isLetterOrDigit || s.charAt(quote - 1) == '_')
    def blank(from: Int, until: Int): Unit = for (k <- from until math.min(until, s.length)) if (s.charAt(k) != '\n') sb.setCharAt(k, ' ')
    var i = 0
    while (i < s.length) {
      if (s.startsWith("//", i)) {
        val e = s.indexOf('\n', i)
        val end = if (e < 0) s.length else e
        blank(i, end)
        i = end
      } else if (s.startsWith("/*", i)) {
        var depth = 0
        var j = i
        var done = false
        while (j < s.length && !done) {
          if (s.startsWith("/*", j)) { depth += 1; j += 2 }
          else if (s.startsWith("*/", j)) { depth -= 1; j += 2; if (depth == 0) done = true }
          else j += 1
        }
        blank(i, j)
        i = j
      } else if (s.startsWith("\"\"\"", i)) {
        val e = s.indexOf("\"\"\"", i + 3)
        var end = if (e < 0) s.length else e + 3
        while (end < s.length && s.charAt(end) == '"') end += 1
        if (!(keepInterpolated && interpolated(i))) blank(i + 3, end - 3)
        i = end
      } else if (s.charAt(i) == '"') {
        var j = i + 1
        while (j < s.length && s.charAt(j) != '"' && s.charAt(j) != '\n') j += (if (s.charAt(j) == '\\') 2 else 1)
        if (!(keepInterpolated && interpolated(i))) blank(i + 1, j)
        i = math.min(j + 1, s.length)
      } else if (s.charAt(i) == '\'' && i + 2 < s.length && (s.charAt(i + 2) == '\'' || s.charAt(i + 1) == '\\')) {
        val e = s.indexOf('\'', i + 2)
        val end = if (e < 0) s.length else e + 1
        blank(i + 1, end - 1)
        i = end
      } else i += 1
    }
    sb.toString
  }

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
  def propsOf(source: String): List[(String, String)] = propsInfo(source).map(p => (p.name, p.tpe))

  /**
   * .stの case class Props(...) のパラメータ(名前・型・デフォルト値の有無・名前の位置).
   * エディタの補完・ホバー・propsの検査・名前の変更に使います.
   */
  def propsInfo(source: String): List[PropInfo] = {
    try {
      val src = new SourceText(source)
      SfcParser.parse(src).script.toList.flatMap { b =>
        val body = b.content(src)
        val code = codeOnly(body)
        PropsDecl.findFirstMatchIn(code).toList.flatMap { m =>
          findClose(code, m.end - 1).toList.flatMap { close =>
            var cursor = m.end
            splitTopLevel(body.substring(m.end, close), ',').flatMap { piece =>
              val pieceStart = cursor
              cursor += piece.length + 1
              PropParam.findFirstMatchIn(piece).filter(_ => piece.trim.nonEmpty).map { pm =>
                val (tpe, hasDefault) = typeAndDefault(pm.group(2))
                PropInfo(pm.group(1), tpe, hasDefault, b.contentStart + pieceStart + pm.start(1))
              }
            }
          }
        }
      }
    } catch {
      case _: StCompileException => Nil
    }
  }

  /** "Int = 1" から "Int" を取り出す(デフォルト値を除く). 関数型の => は区切りにしない */
  private def typeAndDefault(rest: String): (String, Boolean) = {
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
          if (next != '>' && next != '=' && !"=!<>".contains(prev)) return (rest.substring(0, i).trim, true)
        case _ => ()
      }
      i += 1
    }
    (rest.trim, false)
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

    // 定義を探すときは、文字列・コメントの中を除く(コードサンプルの文字列の中の def initState などを拾わない)
    val scriptCode = codeOnly(scriptBody)

    // 自分自身のobject. コンポーネントが継承する名前(RouterLink・Routeなど. BaseComponentがexportしている)と
    // 同じ名前でも自分を指すよう、パッケージがあれば完全修飾名にする
    val selfRef = packageName.fold(in.objectName)(p => s"_root_.$p.${in.objectName}")

    // object内で定義された型は object名.型 で参照する
    def qualify(t: String): String = {
      val definedInside = s"""\\b(?:class|trait|object|enum|type)\\s+${java.util.regex.Pattern.quote(t)}\\b""".r
        .findFirstIn(scriptCode).isDefined
      if (t.matches(Identifier) && definedInside) s"$selfRef.$t" else t
    }

    val initState = InitState.findFirstMatchIn(scriptCode)
    val stateType: Option[String] = initState.map { m =>
      qualify(Option(m.group(2)).map(_.trim).filter(_.nonEmpty).getOrElse("State"))
    }
    val initStateTakesProps = initState.exists(m => Option(m.group(1)).exists(_.trim.nonEmpty))

    // case class Props(...) を定義すると、親から値を受け取るコンポーネントになる
    val props: Option[List[(String, String)]] = PropsDecl.findFirstMatchIn(scriptCode).map { m =>
      val scriptOffset = sfc.script.map(_.offset).getOrElse(0)
      val close = findClose(scriptCode, m.end - 1)
        .getOrElse(src.fail(scriptOffset, "unclosed parameter list of 'case class Props'"))
      splitTopLevel(scriptBody.substring(m.end, close), ',').filter(_.trim.nonEmpty).map {
        case PropParam(name, rest) => (name, s"$name: ${rest.trim}")
        case p => src.fail(scriptOffset, s"cannot parse parameter '${p.trim}' of 'case class Props'")
      }
    }
    if (props.isEmpty && initStateTakesProps) {
      src.fail(sfc.script.map(_.offset).getOrElse(0), "initState can take a parameter only when 'case class Props' is defined")
    }
    val propsType = props.map(_ => s"$selfRef.Props")

    // ---- template ----
    val template = sfc.template.getOrElse(src.fail(0, "<template> block is required"))
    val stateName0 = template.attr("state").flatMap(_.value).getOrElse("s")
    val propsName0 = template.attr("props").flatMap(_.value).getOrElse("props")

    // def mounted / def unmounted: ライフサイクルフック. 引数はなし、またはProps・Stateを受け取れる
    val hookCalls: List[String] = HookDecl.findAllMatchIn(scriptCode).toList.groupBy(_.group(1)).toList.sortBy(_._1).map {
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
            val stateTypes = stateType.toList.flatMap(t => List(t, t.stripPrefix(s"$selfRef.")))
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
    // 同じディレクトリの.stは、ディレクトリから決まるパッケージで完全修飾する
    val componentRefs: Map[String, String] = in.packageName.filter(_.nonEmpty).fold(Map.empty[String, String]) { p =>
      in.siblings.map(n => n -> s"_root_.$p.$n").toMap
    }
    val htmlExpr = new TemplateCodegen(src, in.fileName, scopeAttr, slotProps, marks, componentRefs).generate(trimmed)

    // ---- style ----
    val css = sfc.styles.flatMap { b =>
      val lang = b.attr("lang").flatMap(_.value).getOrElse("css")
      if (lang != "css" && !StyleCompiler.Langs.contains(lang)) {
        src.fail(b.offset, s"unsupported style lang '$lang' (supported: css, scss, sass)")
      }
      val content = b.content(src)
      val compiled =
        if (lang == "css") Some(content)
        else in.styleCompiler.map { sc =>
          sc.compile(lang, content) match {
            case Right(c) => c
            case Left(e) =>
              // 失敗した行の先頭(別のファイルで失敗した場合は<style>の位置)
              val at = e.line.fold(b.offset) { l =>
                Iterator.iterate(b.contentStart)(i => src.text.indexOf('\n', i) + 1).drop(l).next()
              }
              src.fail(at, s"$lang: ${e.message}")
          }
        }
      compiled.map(c => if (b.has("scoped")) CssScoper.scope(c, scopeId(in.fileName)) else c)
    }.mkString("\n").trim

    // ---- 出力 ----
    // 本体(object)を先に作り、使っているものだけをimportする(-Wunused:importsの警告を出さないため)
    val sb = new StringBuilder
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
    val body = sb.toString
    // 使っている名前. コメントと、埋め込みのない文字列リテラル(テンプレートのHTML・コード例など)の中は除く
    val bodyCode = codeForWords(marks.strip(body))
    // object直下(script の一番外側)のimportだけ. defの中などのimportは、その外では使えないため
    val imports = ImportStmt.findAllMatchIn(bodyCode).filter { m =>
      val before = bodyCode.substring(0, m.start)
      before.count(_ == '{') - before.count(_ == '}') == 1
    }.map(_.matched).toList
    // scriptで名前を指定してimportしたものは除く(ヘッダーのimportは使われないため)
    val words = WordPattern.findAllIn(ImportStmt.replaceAllIn(bodyCode, " ")).toSet -- imports.flatMap(importedNames)
    // scriptで同じパッケージを丸ごとimportしている場合も、ヘッダーではimportしない
    def importsAll(pkg: String): Boolean = {
      val all = s"""(?s)import\\s+(?:_root_\\.)?${java.util.regex.Pattern.quote(pkg)}\\.[*_]\\s*""".r
      imports.exists(i => all.pattern.matcher(i).matches())
    }
    val header = new StringBuilder
    header ++= s"// Generated by sbt-scalactive from ${in.fileName}. DO NOT EDIT.\n"
    packageName.foreach(p => header ++= s"package $p\n")
    // components の下のパッケージ(utils・routes など)は短い名前で見えないようにする.
    // 利用者のトップレベルのパッケージ(utils など)と名前が衝突するため.
    // computed・watchなどの関数はimportしない. コンポーネントが継承するBaseComponentが ComponentManager.* をexportしており、
    // objectの中ではそちらが使われるため. 型(RV[Int]など)は継承では使えないので、使っている場合だけ reactive.* をimportする
    header ++= "\nimport io.github.ondeoma.scalactive.components.{fors as _, htmls as _, ifs as _, inputs as _, routes as _, texts as _, utils as _, *}\n"
    if (ReactiveNames.exists(words.contains) && !importsAll("io.github.ondeoma.scalactive.reactive")) header ++= "import io.github.ondeoma.scalactive.reactive.*\n"
    if (in.domNames.forall(_.exists(words.contains)) && !importsAll("org.scalajs.dom")) header ++= "import org.scalajs.dom.*\n"
    header ++= "import io.github.ondeoma.scalactive.components.ComponentManager as __CM\n"
    if (body.contains("__st.")) header ++= "import io.github.ondeoma.scalactive.st.StRuntime as __st\n"
    header ++= "\n"
    val (code, sourceMap) = marks.extract(header.toString + body)
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
