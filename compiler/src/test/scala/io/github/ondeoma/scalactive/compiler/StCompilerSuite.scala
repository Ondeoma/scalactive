package io.github.ondeoma.scalactive.compiler

class StCompilerSuite extends munit.FunSuite {

  private def compile(src: String, name: String = "Foo", pkg: Option[String] = Some("a.b")): StOutput = {
    StCompiler.compile(StInput(src, s"a/b/$name.st", name, pkg)) match {
      case Right(o) => o
      case Left(e) => fail(e.toString)
    }
  }

  private def compileError(src: String): StError = {
    StCompiler.compile(StInput(src, "a/b/Foo.st", "Foo", None)) match {
      case Right(o) => fail(s"expected error but got:\n${o.code}")
      case Left(e) => e
    }
  }

  test("stateless component") {
    val o = compile("<template><p class=\"x\">hi</p></template>")
    assert(o.code.contains("package a.b\n"))
    assert(o.code.contains("object Foo extends StatelessComponent {"))
    assert(o.code.contains("def genHtml(implicit __cm0: __CM): String"))
    assert(o.code.contains("\"<p class=\\\"x\\\">hi</p>\""))
  }

  test("stateful component qualifies state type defined in script") {
    val o = compile(
      """<script lang="scala">
        |case class State(n: RV[Int] = RV(0))
        |def initState() = State()
        |</script>
        |<template><p>{{ s.n }}</p></template>
        |""".stripMargin)
    assert(o.code.contains("object Foo extends StatefulComponent[Foo.State] {"))
    assert(o.code.contains("def genHtml(s: Foo.State)"))
    assert(o.code.contains("__st.text(/* Foo.st:5 */ s.n)"))
  }

  test("explicit initState return type and custom state name") {
    val o = compile(
      """<script>
        |def initState(): my.Model = my.Model()
        |</script>
        |<template state="m"><p>{{ m.x }}</p></template>""".stripMargin)
    assert(o.code.contains("extends StatefulComponent[my.Model]"))
    assert(o.code.contains("def genHtml(m: my.Model)"))
  }

  test("package declaration in script overrides directory package") {
    val o = compile("<script>\npackage x.y\nval a = 1\n</script><template></template>")
    assertEquals(o.packageName, Some("x.y"))
    assert(!o.code.contains("package x.y\n  val"))
    assert(o.code.contains("  val a = 1"))
  }

  test("directives") {
    val o = compile(
      """<template>
        |<div v-if="a">A</div>
        |<div v-else>B</div>
        |<li v-for="(x, i) in xs">{{ x }}</li>
        |<li v-for="y in ys">{{ y }}</li>
        |<p v-show="v" :title="t" :class="c" @click.stop="go(event)">p</p>
        |<Child />
        |</template>""".stripMargin)
    val c = o.code
    assert(c.contains("val __if1 = __st.cond(/* Foo.st:2 */ a)"))
    assert(c.contains("__CM.ifC(__if1) { implicit __cm2 =>"))
    assert(c.contains("__CM.ifC(__st.otherwise(__if1)) { implicit __cm3 =>"))
    assert(c.contains("__CM.forC(/* Foo.st:4 */ xs) { (__cm4, x, i) =>"))
    // 添字の変数がなければforEachC
    assert(c.contains("__CM.forEachC(/* Foo.st:5 */ ys) { (__cm5, y) =>"))
    assert(c.contains("given __CM = __cm4"))
    assert(c.contains("__CM.showIf(__st.cond(/* Foo.st:6 */ v))"))
    assert(c.contains("__st.attr(\"title\", (/* Foo.st:6 */ t))"))
    assert(c.contains("__st.cls(/* Foo.st:6 */ c)"))
    assert(compile("""<template><p :style="m"></p></template>""").code.contains("__st.style(/* Foo.st:1 */ m)"))
    assert(c.contains("event.stopPropagation(); go(event)"))
    assert(c.contains("__st.component(Child())"))
  }

  test("v-html") {
    val c = compile("""<template><div class="x" v-html="s.body"></div><p v-html="raw" /></template>""").code
    assert(c.contains("\"<div class=\\\"x\\\">\" +"), c)
    assert(c.contains("__st.html(/* Foo.st:1 */ s.body)"))
    assert(c.contains("__st.html(/* Foo.st:1 */ raw)"))
    assert(!c.contains("v-html"))
    assert(compileError("<template>\n<div v-html=\"x\">child</div></template>").message.contains("must be empty"))
    assertEquals(compileError("<template>\n<br v-html=\"x\"></template>").line, 2)
    assertEquals(compileError("<template>\n<Child v-html=\"x\" /></template>").line, 2)
  }

  test("v-for with :key") {
    val c = compile(
      """<template>
        |<li v-for="x in xs" :key="x.id" class="a">{{ x }}</li>
        |<template v-for="(y, i) in ys" v-bind:key="y.id"><dt>{{ y }}</dt></template>
        |</template>""".stripMargin).code
    assert(c.contains("__CM.forEachC((/* Foo.st:2 */ xs), key = x => (/* Foo.st:2 */ x.id)) { (__cm1, x) =>"), c)
    assert(c.contains("__CM.forC((/* Foo.st:3 */ ys), key = y => (/* Foo.st:3 */ y.id)) { (__cm2, y, i) =>"), c)
    assert(!c.contains("\"key\""), "keyは属性として出力しない")
    assertEquals(compileError("<template>\n<p :key=\"k\"></p></template>").line, 2)
  }

  test("lifecycle hooks: def mounted / def unmounted") {
    val c = compile(
      """<script lang="scala">
        |case class Props(n: Int)
        |case class State(x: RV[Int] = RV(0))
        |def initState() = State()
        |def mounted(s: State, p: Props): Unit = ()
        |def unmounted(): Unit = ()
        |</script>
        |<template><p>{{ s.x }}</p></template>
        |""".stripMargin).code
    assert(c.contains("    __CM.onMounted(mounted(s, props))\n    __CM.onUnmounted(unmounted())\n"), c)
    val c2 = compile("<script>\ndef mounted: Unit = ()\n</script><template><p>a</p></template>").code
    assert(c2.contains("__CM.onMounted(mounted)"))
    assert(compileError("<script>\ndef mounted(x: Int): Unit = ()\n</script><template><p>a</p></template>").message.contains("mounted"))
  }

  test("v-model on component passes the RV as props") {
    val c = compile("""<template><Field v-model="s.name" v-model:first-name="s.first" label="x" /></template>""").code
    assert(c.contains("__st.component(Field(model = (/* Foo.st:1 */ s.name), firstName = (/* Foo.st:1 */ s.first), label = \"x\"))"), c)
    assertEquals(compileError("<template>\n<Field v-model.trim=\"x\" /></template>").line, 2)
    assertEquals(compileError("<template>\n<Field v-model=\"x\" :model=\"y\" /></template>").line, 2)
  }

  test("named slots") {
    val child = compile(
      """<script lang="scala">
        |case class Props(slot: Slot = Slot.empty, header: Slot = Slot.empty, sideNote: Slot = Slot.empty)
        |</script>
        |<template><div><slot name="header">H</slot><slot /><slot name="side-note" /></div></template>
        |""".stripMargin).code
    assert(child.contains("__st.slot(props.header) { implicit"), child)
    assert(child.contains("__st.slot(props.slot) { implicit"))
    assert(child.contains("__st.slot(props.sideNote) { implicit"))
    val parent = compile(
      """<template><Card title="t">
        |  <template #header><b>h</b></template>
        |  <p>body</p>
        |  <template v-slot:side-note>n</template>
        |</Card></template>""".stripMargin).code
    assert(parent.contains("header = __st.slotOf { implicit"), parent)
    assert(parent.contains("sideNote = __st.slotOf { implicit"))
    assert(parent.contains("slot = __st.slotOf { implicit"))
    assert(parent.contains("\"<b>h</b>\""))
    assert(!parent.contains("\"<template") && !parent.contains("#header"), "名前付きスロットの<template>は出力しない")
    val explicitDefault = compile("""<template><Card><template #default>d</template></Card></template>""").code
    assert(explicitDefault.contains("slot = __st.slotOf"))
    assert(compileError("<script>\ncase class Props(slot: Slot = Slot.empty)\n</script><template>\n<slot name=\"x\" /></template>").message.contains("x: Slot"))
    assertEquals(compileError("<template><Card>\n<template #a>1</template><template #a>2</template></Card></template>").line, 2)
    assertEquals(compileError("<template><Card>\n<template #default>1</template><p>2</p></Card></template>").line, 2)
  }

  test("template ref") {
    val c = compile("""<template><input ref="s.input"><p :ref="s.p"></p></template>""").code
    assert(c.contains("__CM.ref(/* Foo.st:1 */ s.input)"), c)
    assert(c.contains("__CM.ref(/* Foo.st:1 */ s.p)"))
    assertEquals(compileError("<template>\n<Child ref=\"x\" /></template>").line, 2)
  }

  test("class/style/id/data-*/aria-* on component fall through to its root element") {
    val c = compile("""<template><Btn label="ok" class="primary" :style="s.st" id="b1" :data-n="s.n" aria-label="a" :aria-busy="s.busy" /></template>""").code
    assert(c.contains("""__st.component(Btn(label = "ok"), __st.rootCls("primary"), __st.rootStyle((/* Foo.st:1 */ s.st)), __st.rootAttr("id", "b1"), __st.rootAttr("data-n", (/* Foo.st:1 */ s.n)), __st.rootAttr("aria-label", "a"), __st.rootAttr("aria-busy", (/* Foo.st:1 */ s.busy)))"""), c)
  }

  test("scoped slots") {
    val child = compile(
      """<script lang="scala">
        |case class Props(items: List[String], item: ScopedSlot[String] = ScopedSlot.empty)
        |</script>
        |<template><ul><li v-for="x in props.items"><slot name="item" :value="x">{{ x }}</slot></li></ul></template>
        |""".stripMargin).code
    assert(child.contains("__st.scopedSlot(props.item, (/* Foo.st:4 */ x)) { implicit"), child)
    val parent = compile(
      """<template><List>
        |  <template #item="x"><b>{{ x }}</b></template>
        |  <template #row="r: Row">{{ r }}</template>
        |  <template #pair="(a, i)">{{ i }}</template>
        |</List></template>""".stripMargin).code
    assert(parent.contains("item = __st.scopedSlotOf { (x) => (__cm"), parent)
    assert(parent.contains("row = __st.scopedSlotOf { (r: Row) => (__cm"))
    assert(parent.contains("pair = __st.scopedSlotOf { case (a, i) => (__cm"))
    assert(compileError("<template><List>\n<template #item=\"1 + 2\">x</template></List></template>").message.contains("slot parameter"))
  }

  test("propsOf: Propsのパラメータの名前と型") {
    val ps = StCompiler.propsOf(
      """<script lang="scala">
        |case class Props(title: String,
        |                 count: Option[Reactive[Int]] = None,
        |                 onClose: Unit => Unit = _ => (),
        |                 item: ScopedSlot[(String, Int)] = ScopedSlot.empty)
        |</script>
        |<template><p>a</p></template>""".stripMargin)
    assertEquals(ps, List(
      ("title", "String"), ("count", "Option[Reactive[Int]]"), ("onClose", "Unit => Unit"), ("item", "ScopedSlot[(String, Int)]")))
    assertEquals(StCompiler.propsOf("<template><p>a</p></template>"), Nil)
  }

  test("propsInfo: デフォルト値の有無と、.stの中のパラメータ名の位置") {
    val src =
      """<script lang="scala">
        |case class Props(title: String,
        |                 count: Int = 1,
        |                 onClose: Unit => Unit)
        |</script>
        |<template><p>a</p></template>""".stripMargin
    val ps = StCompiler.propsInfo(src)
    assertEquals(ps.map(p => (p.name, p.hasDefault)), List(("title", false), ("count", true), ("onClose", false)))
    ps.foreach(p => assertEquals(src.substring(p.offset, p.offset + p.name.length), p.name))
  }

  test("v-else-if chain evaluates each condition once") {
    val o = compile(
      """<template>
        |<p v-if="a">A</p>
        |<!-- c -->
        |<p v-else-if="b">B</p>
        |<p v-else-if="c">C</p>
        |<p v-else>D</p>
        |<p v-if="d">E</p>
        |</template>""".stripMargin)
    val c = o.code
    assert(c.contains("val __if1 = __st.cond(/* Foo.st:2 */ a)"))
    assert(c.contains("val __if2 = __st.cond(/* Foo.st:4 */ b)"))
    assert(c.contains("val __if3 = __st.cond(/* Foo.st:5 */ c)"))
    assert(c.contains("__CM.ifC(__if1)"))
    assert(c.contains("<!-- c -->"))
    assert(c.contains("__CM.ifC(__st.elseIf(__if2, __if1))"))
    assert(c.contains("__CM.ifC(__st.elseIf(__if3, __if1, __if2))"))
    assert(c.contains("__CM.ifC(__st.otherwise(__if1, __if2, __if3))"))
    // 単独のv-ifはvalを使わない
    assert(c.contains("__CM.ifC(__st.cond(/* Foo.st:7 */ d))"))
  }

  test("template wrapper renders only its children") {
    val o = compile(
      """<template>
        |<template v-if="a"><p>1</p><p>2</p></template>
        |<template v-for="x in xs"><dt>{{ x }}</dt><dd>-</dd></template>
        |</template>""".stripMargin)
    val c = o.code
    assert(c.contains("\"<p>1</p><p>2</p>\""))
    assert(c.contains("\"<dt>\""))
    assert(!c.contains("\"<template"))
  }

  test("key and system event modifiers") {
    val o = compile("""<template><input @keydown.enter.ctrl.prevent="go()" @keyup.esc.delete="x()" @click.self="y()"></template>""")
    val c = o.code
    assert(c.contains("""if (__st.key(event, "Enter") && __st.modifier(event, "ctrl")) { event.preventDefault(); go() }"""))
    assert(c.contains("""__st.key(event, "Escape", "Delete", "Backspace")"""))
    assert(c.contains("""if (__st.self(event)) { y() }"""))
  }

  test("once/capture/passive event modifiers") {
    val c = compile("""<template><div @click.capture.stop="a()" @keydown.enter.once="b()" @scroll.passive="c()"></div></template>""").code
    assert(c.contains("""event.stopPropagation(); a() }, capture = true, once = false, passive = false, guard = _ => true)"""), c)
    assert(c.contains("""b() }, capture = false, once = true, passive = false, guard = (event: org.scalajs.dom.Event) => __st.key(event, "Enter"))"""), c)
    assert(c.contains("""c() }, capture = false, once = false, passive = true, guard = _ => true)"""), c)
    assert(compileError("<template>\n<div @wheel.passive.prevent=\"x\"></div></template>").message.contains("passive"))
  }

  test("v-model") {
    val o = compile("""<template><input v-model="s.name"><textarea v-model.lazy.trim="s.memo"></textarea></template>""")
    assert(o.code.contains("__st.model((/* Foo.st:1 */ s.name), lazyUpdate = false, trim = false)"))
    assert(o.code.contains("__st.model((/* Foo.st:1 */ s.memo), lazyUpdate = true, trim = true)"))
  }

  test("component children are passed as slot and rendered by <slot>") {
    val parent = compile(
      """<template>
        |<Card title="t" @close="s.open := false">
        |  <p>{{ s.body }}</p>
        |</Card>
        |</template>""".stripMargin)
    assert(parent.code.contains("__st.component(Card(title = \"t\", onClose = (event => { /* Foo.st:2 */ s.open := false }), slot = __st.slotOf { implicit __cm1 =>"))
    assert(parent.code.contains("__st.text(/* Foo.st:3 */ s.body)"))

    val child = compile(
      """<script>
        |case class Props(title: String, onClose: Unit => Unit = _ => (), slot: Slot = Slot.empty)
        |</script>
        |<template props="p"><div><slot>empty</slot></div></template>""".stripMargin)
    assert(child.code.contains("__st.slot(p.slot) { implicit __cm1 =>\n      \"empty\""))
  }

  test("long form directives") {
    val o = compile("""<template><p v-bind:title="t" v-on:click="go()"></p></template>""")
    assert(o.code.contains("__st.attr(\"title\", (/* Foo.st:1 */ t))"))
    assert(o.code.contains("__st.on(\"click\","))
  }

  test("props passed to child component") {
    val o = compile(
      """<template>
        |<Child label="a &amp; b" :count="s.n" on-close="x" :on-select="i => go(i)" disabled v-if="ok" />
        |</template>""".stripMargin)
    assert(o.code.contains(
      "__st.component(Child(label = \"a & b\", count = (/* Foo.st:2 */ s.n), onClose = \"x\", onSelect = (/* Foo.st:2 */ i => go(i)), disabled = true))"
    ))
    assert(o.code.contains("__CM.ifC(__st.cond(/* Foo.st:2 */ ok))"))
  }

  test("stateless component with props") {
    val o = compile(
      """<script lang="scala">
        |case class Props(label: String,
        |                 count: Reactive[Int] = RV(0),
        |                 @deprecated("x") val onClick: (Int, String) => Unit = (_, _) => (),
        |                 sep: String = ", ")
        |</script>
        |<template><p>{{ props.label }}</p></template>""".stripMargin)
    val c = o.code
    assert(c.contains("object Foo extends StatelessPropsComponent[Foo.Props] {"))
    assert(c.contains("def apply(label: String, count: Reactive[Int] = RV(0), onClick: (Int, String) => Unit = (_, _) => (), sep: String = \", \")"))
    assert(c.contains("apply(Props(label = label, count = count, onClick = onClick, sep = sep))"))
    assert(c.contains("def genHtml(props: Foo.Props)(implicit __cm0: __CM): String"))
  }

  test("stateful component with props") {
    val o = compile(
      """<script>
        |case class Props(n: Int)
        |case class State(x: RV[Int])
        |def initState() = State(RV(1))
        |</script>
        |<template props="p" state="st"><p>{{ p.n }}{{ st.x }}</p></template>""".stripMargin)
    val c = o.code
    assert(c.contains("object Foo extends StatefulPropsComponent[Foo.Props, Foo.State] {"))
    assert(c.contains("def initState(props: Foo.Props): Foo.State = initState()"))
    assert(c.contains("def genHtml(p: Foo.Props, st: Foo.State)(implicit __cm0: __CM): String"))
  }

  test("initState taking props") {
    val o = compile(
      """<script>
        |case class Props(n: Int)
        |case class State(x: RV[Int])
        |def initState(props: Props): State = State(RV(props.n))
        |</script>
        |<template><p>{{ s.x }}</p></template>""".stripMargin)
    assert(o.code.contains("extends StatefulPropsComponent[Foo.Props, Foo.State]"))
    assert(!o.code.contains("= initState()"))
  }

  test("entities in directive values are decoded") {
    val o = compile("""<template><p :title="if (a) &quot;x&quot; else &quot;y&quot;"></p></template>""")
    assert(o.code.contains("""if (a) "x" else "y""""))
  }

  test("expression ending with line comment keeps closing paren") {
    val o = compile("<template><p>{{ a // note }}</p></template>")
    assert(o.code.contains("a // note\n)"))
  }

  test("void and self-closing elements") {
    val o = compile("""<template><br><input type="text"/><div/></template>""")
    assert(o.code.contains("\"<br><input type=\\\"text\\\"><div></div>\""))
  }

  test("raw text elements are not interpolated") {
    val o = compile("<template><textarea>{{ x }}</textarea></template>")
    assert(o.code.contains("<textarea>{{ x }}</textarea>"))
  }

  test("scoped style") {
    val o = compile(
      """<template><p>x</p></template>
        |<style scoped>.a .b:hover, p::before { color: red; }</style>
        |""".stripMargin)
    val id = StCompiler.scopeId("a/b/Foo.st")
    assert(o.code.contains(s"<p $id>x</p>"))
    assert(o.code.contains(s".a .b[$id]:hover, p[$id]::before"))
    assert(o.code.contains(s"""__st.injectStyle("$id","""))
  }

  test("global style is not scoped") {
    val o = compile("<template><p>x</p></template><style>p { color: red; }</style>")
    assert(o.code.contains("\"<p>x</p>\""))
    assert(o.code.contains("p { color: red; }"))
  }

  test("errors carry line numbers") {
    assertEquals(compileError("<template>\n<div>\n<span></div>\n</template>").line, 3)
    assertEquals(compileError("<template>\n\n<p v-else>x</p></template>").line, 3)
    assertEquals(compileError("<template>\n<p v-for=\"bad\"></p></template>").line, 2)
    assertEquals(compileError("<template>\n<p @click.foo=\"x\"></p></template>").line, 2)
    assertEquals(compileError("<template>\n<p v-model=\"x\"></p></template>").line, 2)
    assertEquals(compileError("<script>\n</script>\n<foo></foo>").line, 3)
    assert(compileError("<script></script>").message.contains("<template> block is required"))
    assertEquals(compileError("<template>\n<Child @click.stop=\"x\" /></template>").line, 2)
    assertEquals(compileError("<template>\n<p v-if=\"a\">a</p>\n<b>x</b>\n<p v-else-if=\"b\">b</p></template>").line, 4)
    assertEquals(compileError("<template>\n<p v-if=\"a\" v-else-if=\"b\">a</p></template>").line, 2)
    assertEquals(compileError("<template>\n<template class=\"x\">a</template></template>").line, 2)
    assertEquals(compileError("<template>\n<slot /></template>").line, 2)
    assertEquals(compileError("<template>\n<div v-model=\"x\"></div></template>").line, 2)
    assertEquals(compileError("<template>\n<input v-model.number=\"x\"></template>").line, 2)
    assertEquals(compileError("<template>\n<input @keyup.foo=\"x\"></template>").line, 2)
    assertEquals(compileError("<template>\n<Child a=\"1\" :a=\"2\" /></template>").line, 2)
    assert(compileError("<script>\ndef initState(p: Int) = 1\n</script><template></template>").message.contains("Props"))
  }

}

class CssScoperSuite extends munit.FunSuite {

  private val A = "data-st-1"

  test("selectors") {
    assertEquals(CssScoper.scopeSelector(".a", A), ".a[data-st-1]")
    assertEquals(CssScoper.scopeSelector(".a > .b + li", A), ".a > .b + li[data-st-1]")
    assertEquals(CssScoper.scopeSelector("a:not(.x .y)", A), "a[data-st-1]:not(.x .y)")
    assertEquals(CssScoper.scopeSelector("input[type=\"a b\"]", A), "input[type=\"a b\"][data-st-1]")
    assertEquals(CssScoper.scopeSelector(".md\\:flex", A), ".md\\:flex[data-st-1]")
  }

  test("at-rules") {
    val css = "@media (max-width: 600px) { .a { x: y } }\n@keyframes k { from { a: b } to { a: c } }\n@import url(x.css);"
    assertEquals(
      CssScoper.scope(css, A),
      "@media (max-width: 600px) { .a[data-st-1] { x: y } }\n@keyframes k { from { a: b } to { a: c } }\n@import url(x.css);"
    )
  }

  test("comments and strings") {
    // セレクタ部分のコメントは除去される
    assertEquals(
      CssScoper.scope("/* c { } */ .a /* d */ { content: \"}\"; }", A),
      ".a[data-st-1] { content: \"}\"; }"
    )
  }

}
