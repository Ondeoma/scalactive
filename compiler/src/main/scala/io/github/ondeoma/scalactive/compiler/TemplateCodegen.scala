package io.github.ondeoma.scalactive.compiler

import scala.collection.mutable.ListBuffer

/**
 * <template>の構文木から、HTML文字列を組み立てるScala式を生成します.
 * 生成コード中では以下のエイリアスが使える前提です.
 *  - `__CM` : io.github.ondeoma.scalactive.components.ComponentManager
 *  - `__st` : io.github.ondeoma.scalactive.st.StRuntime
 *
 * @param scopeAttr scoped CSS用に各要素へ付与する属性名
 * @param props     propsの変数名とPropsのパラメータ名(Propsがある場合のみ). <slot>で展開するSlotを探すのに使う
 * @param marks     ユーザーのコードの位置を記録する目印(LSPで使うSourceMapのため)
 */
final class TemplateCodegen(src: SourceText,
                            fileName: String,
                            scopeAttr: Option[String],
                            props: Option[(String, Set[String])] = None,
                            marks: Marks = new Marks) {

  import TemplateCodegen._

  private val shortName = fileName.split('/').last

  private def lineComment(offset: Int): String = s"/* $shortName:${src.lineOf(offset)} */"

  private var lambdaCounter = 0

  private def nextId(): Int = {
    lambdaCounter += 1
    lambdaCounter
  }

  def generate(nodes: List[HtmlNode]): String = render(genNodes(nodes), "    ")

  // ---- 出力 ----

  private def render(parts: List[Part], indent: String): String = {
    val merged = parts.foldLeft(List.empty[Part]) {
      case (Static(a) :: rest, Static(b)) => Static(a + b) :: rest
      case (acc, p) => p :: acc
    }.reverse.filter {
      case Static(s) => s.nonEmpty
      case _ => true
    }
    if (merged.isEmpty) "\"\""
    else merged.map {
      case Static(s) => StringLiteral(s)
      case Code(c) => c
    }.mkString(s" +\n$indent")
  }

  // ---- 式 ----

  private def decodedValue(a: Attr): Option[Located] = a.located.map(Located.decodeEntities)

  /** ユーザーの式を、元ファイルの行番号コメント付きで埋め込みます. */
  private def userExpr(code: Located, offset: Int): String = {
    val c = code.trim
    if (c.isEmpty) src.fail(offset, "empty expression")
    // 式末尾の行コメントで閉じ括弧が消えないよう、その場合のみ改行する
    val close = if (c.text.contains("//")) "\n)" else ")"
    s"(${lineComment(offset)} ${marks.mark(c)}$close"
  }

  private def attrExpr(a: Attr): String = {
    decodedValue(a) match {
      case Some(v) => userExpr(v, a.offset)
      case None => src.fail(a.offset, s"'${a.name}' requires a value")
    }
  }

  /** イベントハンドラの文. 末尾の行コメントで閉じ括弧が消えないよう、その場合は改行を付けます. */
  private def userStmt(a: Attr): String = {
    val body = decodedValue(a).map(_.trim).filter(!_.isEmpty)
      .getOrElse(src.fail(a.offset, s"'${a.name}' requires a value"))
    marks.mark(body) + (if (body.text.contains("//")) "\n" else " ")
  }

  // ---- ノード ----

  private val conditionalDirectives = Set("v-if", "v-else-if", "v-else")

  private val structuralDirectives = conditionalDirectives + "v-for"

  private def genNodes(nodes: List[HtmlNode]): List[Part] = {
    val out = ListBuffer[Part]()
    val chain = ListBuffer[Branch]()
    // 条件の連なりの途中にある空白・コメント. 連なりが続くかどうかで出力先が決まる.
    val pending = ListBuffer[Part]()

    def flush(): Unit = {
      if (chain.nonEmpty) out += genChain(chain.toList)
      chain.clear()
      out ++= pending
      pending.clear()
    }

    nodes.foreach {
      case t: Text if t.text.trim.isEmpty =>
        if (chain.nonEmpty) pending += Static(t.text) else out += Static(t.text)
      case r: RawText if chain.nonEmpty =>
        pending += Static(r.text)
      case e: Element =>
        e.attrs.filter(a => conditionalDirectives.contains(a.name)) match {
          case Nil =>
            flush()
            out ++= genElement(e)
          case List(a) if a.name == "v-if" =>
            flush()
            chain += Branch(e, a.name, a, Nil)
          case List(a) =>
            if (chain.isEmpty) src.fail(a.offset, s"${a.name} must follow an element with v-if or v-else-if")
            if (a.name == "v-else" && a.value.exists(_.trim.nonEmpty)) src.fail(a.offset, "v-else does not take a value")
            chain += Branch(e, a.name, a, pending.toList)
            pending.clear()
            if (a.name == "v-else") flush()
          case as =>
            src.fail(as(1).offset, s"${as.map(_.name).mkString(" and ")} cannot be used on the same element")
        }
      case n =>
        flush()
        out ++= genNode(n)
    }
    flush()
    out.toList
  }

  /**
   * v-if / v-else-if / v-else の連なりを生成します.
   * 各条件は1度だけ評価してvalに束縛し、後続の分岐は先行する条件がすべて偽の場合に表示します.
   */
  private def genChain(chain: List[Branch]): Part = chain match {
    case List(b) => genIf(b.e, s"__st.cond${attrExpr(b.attr)}")
    case _ =>
      val conds = chain.filter(_.kind != "v-else").map(b => (s"__if${nextId()}", b))
      val names = conds.map(_._1)
      val defs = conds.map { case (n, b) => s"      val $n = __st.cond${attrExpr(b.attr)}\n" }.mkString
      val parts = chain.zipWithIndex.flatMap { case (b, i) =>
        val cond = b.kind match {
          case "v-if" => names.head
          case "v-else-if" => s"__st.elseIf(${names(i)}, ${names.take(i).mkString(", ")})"
          case _ => s"__st.otherwise(${names.mkString(", ")})"
        }
        b.wsBefore :+ genIf(b.e, cond)
      }
      Code(s"({\n$defs      ${render(parts, "      ")}\n    })")
  }

  private def genNode(n: HtmlNode): List[Part] = n match {
    case e: Element => genElement(e)
    case t: Text => genText(t)
    case r: RawText => List(Static(r.text))
  }

  private def genIf(e: Element, cond: String): Part = {
    val id = nextId()
    val body = render(genElement(e), "      ")
    Code(s"__CM.ifC($cond) { implicit __cm$id =>\n      $body\n    }")
  }

  private val ForPattern = """(?s)^\s*(?:\(\s*([A-Za-z_]\w*)\s*(?:,\s*([A-Za-z_]\w*)\s*)?\)|([A-Za-z_]\w*))\s+in\s+(.+)$""".r

  private def isKeyAttr(a: Attr): Boolean = a.name == ":key" || a.name == "v-bind:key"

  private def genElement(e: Element): List[Part] = {
    e.attrs.find(_.name == "v-for") match {
      case None =>
        e.attrs.find(isKeyAttr).foreach(a => src.fail(a.offset, ":key can be used only on an element with v-for"))
        genElementCore(e)
      case Some(a) =>
        if (e.attrs.exists(x => conditionalDirectives.contains(x.name))) {
          src.fail(a.offset, "v-for cannot be combined with v-if/v-else-if/v-else on the same element; wrap it with <template>")
        }
        val v = decodedValue(a).getOrElse(Located("", 0))
        val (item, itemName, idx, listExpr) = ForPattern.findFirstMatchIn(v.text) match {
          case Some(m) =>
            def group(i: Int): Option[Located] = Option(m.group(i)).map(_ => v.substring(m.start(i), m.end(i)))
            val it = group(1).orElse(group(3)).get
            (marks.mark(it), it.text, group(2).map(marks.mark), group(4).get)
          case None => src.fail(a.offset, "v-for must be in the form \"item in expr\" or \"(item, index) in expr\"")
        }
        val id = nextId()
        // 添字の変数がなければforEachC(位置が変わった行を作り直さないので、削除や並べ替えが速い)
        val (fn, params) = idx match {
          case Some(i) => ("forC", s"__cm$id, $item, $i")
          case None => ("forEachC", s"__cm$id, $item")
        }
        // :key="expr" は行を対応付けるキー関数になる(要素の属性としては出力しない)
        val keyAttr = e.attrs.find(isKeyAttr)
        val source = keyAttr match {
          // (ラムダを括弧で囲むと、オーバーロードの解決で引数の型が推論できないため囲まない)
          case Some(k) => s"(${userExpr(listExpr, a.offset)}, key = $itemName => ${attrExpr(k)})"
          case None => userExpr(listExpr, a.offset)
        }
        val body = render(genElementCore(e.copy(attrs = e.attrs.filterNot(isKeyAttr))), "      ")
        List(Code(
          s"__CM.$fn$source { ($params) =>\n" +
            s"      given __CM = __cm$id\n" +
            s"      $body\n" +
            "    }"
        ))
    }
  }

  private def isComponentTag(name: String): Boolean = name.head.isUpper

  private def genElementCore(e: Element): List[Part] = {
    val vHtml = e.attrs.find(_.name == "v-html")
    if (isComponentTag(e.name) || e.name == "template" || e.name == "slot") {
      vHtml.foreach(a => src.fail(a.offset, s"v-html cannot be used on <${e.name}>"))
    }
    if (isComponentTag(e.name)) genComponent(e)
    else if (e.name == "template") genTemplateWrapper(e)
    else if (e.name == "slot") genSlot(e)
    else {
      val out = ListBuffer[Part]()
      out += Static("<" + e.name)
      e.attrs.foreach { a =>
        if (structuralDirectives.contains(a.name) || a.name == "v-html") ()
        else out ++= genAttr(e, a)
      }
      scopeAttr.foreach(s => out += Static(" " + s))
      out += Static(">")
      if (!HtmlParser.voidElements.contains(e.name.toLowerCase)) {
        vHtml match {
          case Some(a) =>
            // v-html: 子要素の代わりに、式の値をHTMLとして展開する(エスケープしない)
            if (e.children.exists { case Text(t, _) => t.trim.nonEmpty; case _ => true }) {
              src.fail(a.offset, "an element with v-html must be empty (its children are replaced)")
            }
            out += Code(s"__st.html${attrExpr(a)}")
          case None =>
            out ++= genNodes(e.children)
        }
        out += Static(s"</${e.name}>")
      } else {
        vHtml.foreach(a => src.fail(a.offset, s"v-html cannot be used on void element <${e.name}>"))
      }
      out.toList
    }
  }

  /** <template v-if/v-for>: 要素自体は出力せず、子要素だけを展開します. */
  private def genTemplateWrapper(e: Element): List[Part] = {
    e.attrs.find(a => !structuralDirectives.contains(a.name) && !isKeyAttr(a)).foreach { a =>
      src.fail(a.offset, s"'${a.name}' cannot be used on <template>; only v-if/v-else-if/v-else/v-for/:key are allowed")
    }
    genNodes(e.children)
  }

  /**
   * <slot>: 親から渡された子要素を展開します. <slot>の中身は子要素が渡されなかった場合の既定の内容です.
   * <slot name="header"> はPropsの `header: Slot` を展開します(名前付きスロット).
   */
  private def genSlot(e: Element): List[Part] = {
    val nameAttr = e.attrs.find(_.name == "name")
    val propName0 = nameAttr.map(a => propName(a.value.getOrElse(src.fail(a.offset, "<slot name> requires a value")))).getOrElse("slot")
    val slot = props.filter(_._2.contains(propName0)).map(p => s"${p._1}.$propName0").getOrElse {
      if (nameAttr.isEmpty) src.fail(e.offset, "<slot> requires 'slot: Slot' in case class Props (e.g. case class Props(slot: Slot = Slot.empty))")
      else src.fail(e.offset, s"<slot name='${nameAttr.get.value.get}'> requires '$propName0: Slot' in case class Props (e.g. case class Props($propName0: Slot = Slot.empty))")
    }
    e.attrs.find(a => !structuralDirectives.contains(a.name) && a.name != "name").foreach { a =>
      src.fail(a.offset, s"'${a.name}' on <slot> is not supported (scoped slots are not supported)")
    }
    val id = nextId()
    val fallback = render(genNodes(e.children), "      ")
    List(Code(s"__st.slot($slot) { implicit __cm$id =>\n      $fallback\n    }"))
  }

  /**
   * 子コンポーネントの属性は、applyの名前付き引数(props)として渡します.
   *  - `name="text"` : 文字列リテラル
   *  - `name` (値なし) : true
   *  - `:name="expr"` / `v-bind:name="expr"` : Scala式
   *  - `@name="stmt"` / `v-on:name="stmt"` : `onName = (event => { stmt })`
   *    (子は `onName: A => Unit` を定義し、`props.onName(a)` で通知します)
   *  - `v-model="rv"` / `v-model:name="rv"` : `model = rv` / `name = rv` (子はRVを受け取り、双方向に使います)
   * ケバブケースの名前はキャメルケースに変換します(`on-close` → `onClose`).
   * 子要素は `slot = __st.slotOf { ... }` として渡します.
   */
  /** 子コンポーネントのルート要素に引き継ぐ属性か(propsにはしない) */
  private def isFallthrough(raw: String): Boolean = {
    raw == "class" || raw == "style" || raw == "id" || raw == "role" || raw.startsWith("data-") || raw.startsWith("aria-")
  }

  /** 属性名から先頭の : / v-bind: を除いたもの(束縛でなければNone) */
  private def boundName(n: String): Option[String] = {
    if (n.startsWith(":")) Some(n.drop(1))
    else if (n.startsWith("v-bind:")) Some(n.drop("v-bind:".length))
    else None
  }

  /** 子コンポーネントのルート要素に付け加える属性(class・style・id・role・data-*・aria-*) */
  private def genFallthrough(a: Attr): Option[String] = {
    val bound = boundName(a.name)
    val raw = bound.getOrElse(a.name)
    if (!isFallthrough(raw)) None
    else {
      val value = bound match {
        case Some(_) => attrExpr(a)
        case None => decodedValue(a).map(v => StringLiteral(v.text)).getOrElse("true")
      }
      Some(raw match {
        case "class" => s"__st.rootCls($value)"
        case "style" => s"__st.rootStyle($value)"
        case _ => s"__st.rootAttr(${StringLiteral(raw)}, $value)"
      })
    }
  }

  private def genComponent(e: Element): List[Part] = {
    val fallthrough = e.attrs.filterNot(a => structuralDirectives.contains(a.name)).flatMap(genFallthrough)
    val attrArgs = e.attrs.filterNot(a => structuralDirectives.contains(a.name) || isFallthrough(boundName(a.name).getOrElse(a.name))).map { a =>
      val n = a.name
      val (raw, value) =
        if (n.startsWith(":") || n.startsWith("v-bind:")) {
          (if (n.startsWith(":")) n.drop(1) else n.drop("v-bind:".length), attrExpr(a))
        } else if (n.startsWith("@") || n.startsWith("v-on:")) {
          n match {
            case EventPattern(event, mods) =>
              if (mods.nonEmpty) src.fail(a.offset, s"event modifiers are not supported on component <${e.name}>")
              ("on-" + event, s"(event => { ${lineComment(a.offset)} ${userStmt(a)}})")
            case _ => src.fail(a.offset, s"invalid event binding '$n'")
          }
        } else if (n == "v-model" || n.startsWith("v-model:") || n.startsWith("v-model.")) {
          // v-model="rv" はpropsのmodelに、v-model:name="rv" はpropsのnameにRVを渡す(RVを共有するので双方向になる)
          if (n.contains(".")) src.fail(a.offset, s"v-model modifiers are not supported on component <${e.name}>")
          val target = if (n == "v-model") "model" else n.drop("v-model:".length)
          if (target.isEmpty) src.fail(a.offset, s"prop name expected in '$n'")
          (target, attrExpr(a))
        } else if (n.startsWith("v-")) {
          src.fail(a.offset, s"directive '$n' on component <${e.name}> is not supported")
        } else {
          (n, decodedValue(a).map(v => StringLiteral(v.text)).getOrElse("true"))
        }
      val name = propName(raw)
      if (name == "ref") src.fail(a.offset, s"'ref' on component <${e.name}> is not supported; set it on an element")
      if (!name.matches("""[A-Za-z_][A-Za-z0-9_]*""")) src.fail(a.offset, s"invalid prop name '$raw'")
      // 名前を変換していない場合は、引数名を.stの属性名に対応付ける(ホバーや定義ジャンプのため)
      val prefix = if (n.startsWith(":")) 1 else if (n.startsWith("v-bind:")) "v-bind:".length else 0
      val nameCode = if (name == raw && n.length == prefix + raw.length) marks.mark(name, a.offset + prefix) else name
      (name, nameCode, value, a.offset)
    }
    // <template #name> / <template v-slot:name> は名前付きスロット、それ以外の子要素はデフォルトスロット(slot)
    def slotName(c: HtmlNode): Option[(String, Attr)] = c match {
      case t: Element if t.name == "template" =>
        t.attrs.find(a => a.name.startsWith("#") || a.name == "v-slot" || a.name.startsWith("v-slot:")).map { a =>
          val raw = if (a.name.startsWith("#")) a.name.drop(1) else a.name.drop("v-slot".length).stripPrefix(":")
          if (a.value.exists(_.trim.nonEmpty)) src.fail(a.offset, s"'${a.name}' does not take a value (scoped slots are not supported)")
          t.attrs.find(_ ne a).foreach(x => src.fail(x.offset, s"'${x.name}' cannot be used on <template ${a.name}>"))
          (if (raw.isEmpty || raw == "default") "slot" else propName(raw), a)
        }
      case _ => None
    }
    val (named, rest) = e.children.partition(c => slotName(c).isDefined)
    def slotOf(nodes: List[HtmlNode]): String = {
      val id = nextId()
      val body = render(genNodes(nodes), "      ")
      s"__st.slotOf { implicit __cm$id =>\n      $body\n    }"
    }
    val namedArgs = named.map { c =>
      val (name, a) = slotName(c).get
      if (!name.matches("""[A-Za-z_][A-Za-z0-9_]*""")) src.fail(a.offset, s"invalid slot name '${a.name}'")
      (name, name, slotOf(c.asInstanceOf[Element].children), a.offset)
    }
    val hasChildren = rest.exists {
      case Text(t, _) => t.trim.nonEmpty
      case _ => true
    }
    val slotArg = if (!hasChildren) Nil else List(("slot", "slot", slotOf(rest), e.offset))
    val args = attrArgs ++ namedArgs ++ slotArg
    args.groupBy(_._1).collectFirst { case (n, xs) if xs.size > 1 => (n, xs.map(_._4).max) }.foreach {
      case (n, offset) => src.fail(offset, s"duplicate prop or slot '$n' on component <${e.name}>")
    }
    val argList = args.map { case (_, n, v, _) => s"$n = $v" }.mkString(", ")
    val extra = fallthrough.map(", " + _).mkString
    List(Code(s"__st.component(${marks.mark(e.name, e.offset + 1)}($argList)$extra)"))
  }

  private def propName(s: String): String = {
    val parts = s.split('-')
    (parts.head :: parts.tail.toList.filter(_.nonEmpty).map(p => p.head.toUpper.toString + p.tail)).mkString
  }

  private val EventPattern = """^(?:@|v-on:)([\w-]+)((?:\.[\w-]+)*)$""".r

  private val actionModifiers = Map(
    "prevent" -> "event.preventDefault(); ",
    "stop" -> "event.stopPropagation(); "
  )

  /** キー修飾子 → KeyboardEvent.key の値 */
  private val keyModifiers = Map(
    "enter" -> List("Enter"),
    "tab" -> List("Tab"),
    "delete" -> List("Delete", "Backspace"),
    "esc" -> List("Escape"),
    "space" -> List(" "),
    "up" -> List("ArrowUp"),
    "down" -> List("ArrowDown"),
    "left" -> List("ArrowLeft"),
    "right" -> List("ArrowRight")
  )

  private val systemModifiers = Set("ctrl", "shift", "alt", "meta")

  /** addEventListenerのオプションになる修飾子 */
  private val listenerModifiers = Set("once", "capture", "passive")

  private val allModifiers = List("prevent", "stop", "self", "once", "capture", "passive") ++ keyModifiers.keys.toList.sorted ++ List("ctrl", "shift", "alt", "meta")

  private def genAttr(e: Element, a: Attr): List[Part] = {
    val n = a.name
    if (n == "ref" || n == ":ref" || n == "v-bind:ref") {
      // テンプレート参照: 値はElementRefの式
      List(Code(s"__CM.ref${attrExpr(a)}"))
    } else if (n == "v-show") {
      List(Code(s"__CM.showIf(__st.cond${attrExpr(a)})"))
    } else if (n == "v-model" || n.startsWith("v-model.")) {
      val tag = e.name.toLowerCase
      if (tag != "input" && tag != "textarea" && tag != "select") {
        src.fail(a.offset, s"v-model can be used only on <input>, <textarea> and <select>")
      }
      val mods = n.split('.').toList.drop(1)
      mods.foreach {
        case "lazy" | "trim" => ()
        case "number" => src.fail(a.offset, "v-model.number is not needed; bind an RV[Int] or RV[Double] instead")
        case m => src.fail(a.offset, s"unknown v-model modifier '.$m' (supported: .lazy, .trim)")
      }
      List(Code(s"__st.model(${attrExpr(a)}, lazyUpdate = ${mods.contains("lazy")}, trim = ${mods.contains("trim")})"))
    } else if (n.startsWith(":") || n.startsWith("v-bind:")) {
      val name = if (n.startsWith(":")) n.drop(1) else n.drop("v-bind:".length)
      if (name.isEmpty) src.fail(a.offset, s"attribute name expected in '$n'")
      if (name == "class") List(Code(s"__st.cls${attrExpr(a)}"))
      else if (name == "style") List(Code(s"__st.style${attrExpr(a)}"))
      else List(Code(s"__st.attr(${StringLiteral(name)}, ${attrExpr(a)})"))
    } else if (n.startsWith("@") || n.startsWith("v-on:")) {
      n match {
        case EventPattern(event, mods) =>
          val ms = mods.split('.').filter(_.nonEmpty).toList
          ms.foreach { m =>
            if (!actionModifiers.contains(m) && !keyModifiers.contains(m) && !systemModifiers.contains(m) &&
              !listenerModifiers.contains(m) && m != "self") {
              src.fail(a.offset, s"unknown event modifier '.$m' (supported: ${allModifiers.mkString(", ")})")
            }
          }
          if (ms.contains("passive") && ms.contains("prevent")) {
            src.fail(a.offset, ".passive and .prevent cannot be used together (preventDefault is ignored in passive listeners)")
          }
          val keys = ms.flatMap(m => keyModifiers.getOrElse(m, Nil)).distinct
          val guards =
            (if (keys.isEmpty) Nil else List(s"__st.key(event, ${keys.map(StringLiteral(_)).mkString(", ")})")) :::
              ms.filter(systemModifiers.contains).distinct.map(m => s"__st.modifier(event, ${StringLiteral(m)})") :::
              (if (ms.contains("self")) List("__st.self(event)") else Nil)
          val pre = ms.filter(actionModifiers.contains).distinct.map(actionModifiers).mkString
          val body = userStmt(a)
          if (ms.exists(listenerModifiers.contains)) {
            // .once/.capture/.passive: 条件(キーなど)は別の関数にし、.onceは条件を満たした最初の1回だけ実行する
            val guard = if (guards.isEmpty) "_ => true" else s"(event: org.scalajs.dom.Event) => ${guards.mkString(" && ")}"
            List(Code(
              s"__st.on(${StringLiteral(event)}, (event: org.scalajs.dom.Event) => { ${lineComment(a.offset)} $pre$body}, " +
                s"capture = ${ms.contains("capture")}, once = ${ms.contains("once")}, passive = ${ms.contains("passive")}, guard = $guard)"
            ))
          } else {
            val stmt =
              if (guards.isEmpty) s"$pre$body"
              else s"if (${guards.mkString(" && ")}) { $pre$body} "
            List(Code(
              s"__st.on(${StringLiteral(event)}, (event: org.scalajs.dom.Event) => { ${lineComment(a.offset)} $stmt})"
            ))
          }
        case _ => src.fail(a.offset, s"invalid event binding '$n'")
      }
    } else if (n.startsWith("v-")) {
      src.fail(a.offset, s"unknown directive '$n'")
    } else {
      val v = a.value match {
        case None => ""
        case Some(x) if a.quote == 0 => "=" + x
        case Some(x) => "=" + a.quote + x + a.quote
      }
      List(Static(" " + n + v))
    }
  }

  private val InterpolationPattern = """(?s)\{\{(.*?)\}\}""".r

  private def genText(t: Text): List[Part] = {
    val out = ListBuffer[Part]()
    var last = 0
    val located = Located(t.text, t.offset)
    InterpolationPattern.findAllMatchIn(t.text).foreach { m =>
      out += Static(t.text.substring(last, m.start))
      out += Code(s"__st.text${userExpr(located.substring(m.start(1), m.end(1)), t.offset + m.start)}")
      last = m.end
    }
    out += Static(t.text.substring(last))
    out.toList
  }

}

object TemplateCodegen {

  private sealed trait Part

  private final case class Static(s: String) extends Part

  private final case class Code(c: String) extends Part

  /** v-if / v-else-if / v-else の連なりの1要素. wsBeforeは直前の要素との間の空白・コメント. */
  private final case class Branch(e: Element, kind: String, attr: Attr, wsBefore: List[Part])

}

object StringLiteral {

  def apply(s: String): String = {
    val b = new StringBuilder("\"")
    s.foreach {
      case '"' => b ++= "\\\""
      case '\\' => b ++= "\\\\"
      case '\n' => b ++= "\\n"
      case '\r' => b ++= "\\r"
      case '\t' => b ++= "\\t"
      case c if c < ' ' => b ++= "\\u%04x".format(c.toInt)
      case c => b += c
    }
    b += '"'
    b.toString
  }

}
