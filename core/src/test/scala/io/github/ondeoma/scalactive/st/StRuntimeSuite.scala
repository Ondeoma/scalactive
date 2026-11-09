package io.github.ondeoma.scalactive.st

import io.github.ondeoma.scalactive.components.*
import io.github.ondeoma.scalactive.components.ComponentManager.{AddMethod, HTML}
import io.github.ondeoma.scalactive.reactive.*
import org.scalajs.dom.*

import scala.scalajs.js

/**
 * .stの生成コードが使うStRuntimeのテスト.
 * オーバーロードの振り分け(リアクティブ/静的)と、DOMへの反映をjsdom上で確認します.
 */
class StRuntimeSuite extends munit.FunSuite {

  private var savedDelay: scala.concurrent.duration.FiniteDuration = scala.concurrent.duration.Duration.Zero
  private var savedKeepScroll = true

  override def beforeAll(): Unit = {
    savedDelay = io.github.ondeoma.scalactive.ScalactiveConfig.reloadTimerLazyDuration
    savedKeepScroll = io.github.ondeoma.scalactive.ScalactiveConfig.keepWindowScrollPositionWhenReload
    io.github.ondeoma.scalactive.ScalactiveConfig.reloadTimerLazyDuration = scala.concurrent.duration.Duration.Zero
    // jsdomはwindow.scrollToを実装していない
    io.github.ondeoma.scalactive.ScalactiveConfig.keepWindowScrollPositionWhenReload = false
  }

  override def afterAll(): Unit = {
    io.github.ondeoma.scalactive.ScalactiveConfig.reloadTimerLazyDuration = savedDelay
    io.github.ondeoma.scalactive.ScalactiveConfig.keepWindowScrollPositionWhenReload = savedKeepScroll
  }

  /** 予約された描き直しが終わるまで待つ */
  protected def flush(): scala.concurrent.Future[Unit] = {
    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
    js.Promise.resolve[Unit](()).toFuture.flatMap(_ => js.Promise.resolve[Unit](()).toFuture)
  }

  /** genで生成したHTMLをdocument.bodyにマウントし、そのルート要素を返します. */
  private def mount(gen: ComponentManager => HTML): HTMLElement = {
    val root = document.createElement("div").asInstanceOf[HTMLElement]
    document.body.appendChild(root)
    val component = new StatelessComponent {
      def genHtml(implicit cm: ComponentManager): HTML = gen(cm)
    }
    given HTMLElement = root
    given AddMethod = AddMethod.append(root)
    component().init() match {
      case Left(e) => fail(e)
      case Right(_) => root
    }
  }

  private def q[E <: Element](root: HTMLElement, selector: String): E = {
    root.querySelector(selector).asInstanceOf[E]
  }

  private def fire(ele: Element, name: String): Unit = {
    ele.dispatchEvent(new Event(name, new EventInit {
      bubbles = true
    }))
  }

  test("text: 静的な値はエスケープして埋め込み、Reactiveは変更に追従する") {
    val n = RV(1)
    val root = mount { implicit cm =>
      s"<p>${StRuntime.text("<b>&</b>")}</p><span>${StRuntime.text(n)}</span>"
    }
    assertEquals(q[HTMLElement](root, "p").textContent, "<b>&</b>")
    assertEquals(q[HTMLElement](root, "p").innerHTML, "&lt;b&gt;&amp;&lt;/b&gt;")
    assertEquals(q[HTMLElement](root, "span").textContent, "1")
    n := 2
    assertEquals(q[HTMLElement](root, "span").textContent, "2")
  }

  test("attr: Reactive[Boolean]は属性の有無、その他のReactiveは値、静的な値はその場で展開") {
    val b = RV(true)
    val i = RV(3)
    val s = RV("x")
    val root = mount { implicit cm =>
      s"""<button ${StRuntime.attr("disabled", b)} ${StRuntime.attr("data-n", i)} ${StRuntime.attr("title", s)}
         | ${StRuntime.attr("a", true)} ${StRuntime.attr("b", false)} ${StRuntime.attr("c", None)} ${StRuntime.attr("d", Some("\"q\""))}></button>""".stripMargin
    }
    val btn = q[HTMLElement](root, "button")
    assert(btn.hasAttribute("disabled"))
    assertEquals(btn.getAttribute("data-n"), "3")
    assertEquals(btn.getAttribute("title"), "x")
    assert(btn.hasAttribute("a"))
    assert(!btn.hasAttribute("b"))
    assert(!btn.hasAttribute("c"))
    assertEquals(btn.getAttribute("d"), "\"q\"")
    b := false
    i := 4
    s := "y"
    assert(!btn.hasAttribute("disabled"))
    assertEquals(btn.getAttribute("data-n"), "4")
    assertEquals(btn.getAttribute("title"), "y")
  }

  test("cls: String/List[String]/静的な値を静的なclass属性と併用できる") {
    val s = RV("a b")
    val l = RV(List("c"))
    val root = mount { implicit cm =>
      s"""<p class="x" ${StRuntime.cls(s)}></p><i class="y" ${StRuntime.cls(l)}></i><b ${StRuntime.cls(List("d", "e f"))}></b>"""
    }
    assertEquals(q[HTMLElement](root, "p").className, "x a b")
    assertEquals(q[HTMLElement](root, "i").className, "y c")
    assertEquals(q[HTMLElement](root, "b").className, "d e f")
    s := "b"
    l := List("z")
    assertEquals(q[HTMLElement](root, "p").className, "x b")
    assertEquals(q[HTMLElement](root, "i").className, "y z")
  }

  test("cls: クラス名と条件の組・Map(オブジェクト記法)と、文字列との混在") {
    val on = RV(false)
    val flags = RV(Map("m1" -> true, "m2" -> false))
    val root = mount { implicit cm =>
      s"""<p class="x" ${StRuntime.cls("active" -> on)}></p>""" +
        s"""<i ${StRuntime.cls(Map("a" -> true, "b" -> false, "c d" -> on))}></i>""" +
        s"""<b ${StRuntime.cls(List("btn", "on" -> on))}></b>""" +
        s"""<u class="y" ${StRuntime.cls(flags)}></u>"""
    }
    def classes(sel: String) = q[HTMLElement](root, sel).className
    assertEquals(classes("p"), "x")
    assertEquals(classes("i"), "a")
    assertEquals(classes("b"), "btn")
    assertEquals(classes("u"), "y m1")
    on := true
    flags := Map("m1" -> false, "m2" -> true)
    assertEquals(classes("p"), "x active")
    assertEquals(classes("i"), "a c d")
    assertEquals(classes("b"), "btn on")
    assertEquals(classes("u"), "y m2")
  }

  test("style: プロパティごとの組・Map(オブジェクト記法)は静的なstyleと併用でき、Reactiveに追従する") {
    val color = RV("red")
    val size = RV[Option[String]](Some("12px"))
    val whole = RV(Map("color" -> "blue", "margin-top" -> "1px"))
    val root = mount { implicit cm =>
      s"""<p style="padding: 2px" ${StRuntime.style(Map("color" -> color, "fontSize" -> size, "--x" -> 1))}></p>""" +
        s"""<i ${StRuntime.style("display" -> "none")}></i>""" +
        s"""<b ${StRuntime.style(whole)}></b>""" +
        s"""<u ${StRuntime.style("color: green")}></u>"""
    }
    val p = q[HTMLElement](root, "p")
    assertEquals(p.style.getPropertyValue("padding"), "2px")
    assertEquals(p.style.getPropertyValue("color"), "red")
    assertEquals(p.style.getPropertyValue("font-size"), "12px")
    assertEquals(p.style.getPropertyValue("--x"), "1")
    assertEquals(q[HTMLElement](root, "i").style.display, "none")
    assertEquals(q[HTMLElement](root, "u").style.color, "green")
    val b = q[HTMLElement](root, "b")
    assertEquals(b.style.getPropertyValue("margin-top"), "1px")
    color := "blue"
    size := None
    whole := Map("color" -> "black")
    assertEquals(p.style.getPropertyValue("color"), "blue")
    assertEquals(p.style.getPropertyValue("font-size"), "")
    assertEquals(p.style.getPropertyValue("padding"), "2px")
    assertEquals(b.style.getPropertyValue("color"), "black")
    assertEquals(b.style.getPropertyValue("margin-top"), "")
  }

  test("elseIf/otherwise: 先行条件がすべて偽の分岐だけが真になり、変化した時だけ通知する") {
    val a = RV(false)
    val b = RV(true)
    var r: (Reactive[Boolean], Reactive[Boolean], Reactive[Boolean]) = null
    mount { implicit cm =>
      r = (StRuntime.cond(a), StRuntime.elseIf(StRuntime.cond(b), a), StRuntime.otherwise(a, b))
      ""
    }
    val (ifV, elseIfV, elseV) = r
    var notified = 0
    elseV.addWatcher(_ => notified += 1)
    assertEquals((ifV.v, elseIfV.v, elseV.v), (false, true, false))
    a := true
    assertEquals((ifV.v, elseIfV.v, elseV.v), (true, false, false))
    assertEquals(notified, 0)
    a := false
    b := false
    assertEquals((ifV.v, elseIfV.v, elseV.v), (false, false, true))
    assertEquals(notified, 1)
    assert(!StRuntime.cond(false).v)
  }

  test("on: キー修飾子・修飾キー・selfの判定") {
    var log = List.empty[String]
    val root = mount { implicit cm =>
      s"""<div ${StRuntime.on("click", e => if (StRuntime.self(e)) log :+= "self")}>
         |  <input ${StRuntime.on("keydown", e => if (StRuntime.key(e, "Enter") && StRuntime.modifier(e, "ctrl")) log :+= "ctrl+enter")}>
         |</div>""".stripMargin
    }
    val input = q[HTMLInputElement](root, "input")
    def press(k: String, ctrl: Boolean): Unit = {
      input.dispatchEvent(new KeyboardEvent("keydown", new KeyboardEventInit {
        this.key = k
        ctrlKey = ctrl
      }))
    }
    press("Enter", ctrl = false)
    press("a", ctrl = true)
    press("Enter", ctrl = true)
    fire(input, "click")
    fire(q[HTMLElement](root, "div"), "click")
    assertEquals(log, List("ctrl+enter", "self"))
    assert(!StRuntime.key(new Event("click"), "Enter"))
  }

  test("on: once(条件を満たした最初の1回だけ)・capture(子より先)・passive(preventDefaultが効かない)") {
    var log = List.empty[String]
    val enter: Event => Boolean = e => StRuntime.key(e, "Enter")
    val root = mount { implicit cm =>
      s"""<div ${StRuntime.on("click", _ => log :+= "outer-capture", capture = true, once = false, passive = false, guard = _ => true)}>
         |  <input ${StRuntime.on("keydown", _ => log :+= "enter-once", capture = false, once = true, passive = false, guard = enter)}
         |         ${StRuntime.on("click", _ => log :+= "inner")}
         |         ${StRuntime.on("wheel", e => e.preventDefault(), capture = false, once = false, passive = true, guard = _ => true)}>
         |</div>""".stripMargin
    }
    val input = q[HTMLInputElement](root, "input")
    def press(k: String): Unit = input.dispatchEvent(new KeyboardEvent("keydown", new KeyboardEventInit { this.key = k }))
    press("a")
    press("Enter")
    press("Enter")
    fire(input, "click")
    assertEquals(log, List("enter-once", "outer-capture", "inner"))
    val wheel = new Event("wheel", new EventInit { cancelable = true })
    input.dispatchEvent(wheel)
    assert(!wheel.defaultPrevented, "passiveなリスナーのpreventDefaultは無視される")
  }

  test("model: テキスト入力とRV[String]を双方向に同期し、trimは入力中の値を書き換えない") {
    val s = RV("a")
    val t = RV("")
    val root = mount { implicit cm =>
      s"""<input id="s" ${StRuntime.model(s)}><textarea ${StRuntime.model(t, trim = true)}></textarea>"""
    }
    val input = q[HTMLInputElement](root, "#s")
    assertEquals(input.value, "a")
    input.value = "abc"
    fire(input, "input")
    assertEquals(s.v, "abc")
    s := "z"
    assertEquals(input.value, "z")

    val ta = q[HTMLTextAreaElement](root, "textarea")
    ta.value = " hi "
    fire(ta, "input")
    assertEquals(t.v, "hi")
    assertEquals(ta.value, " hi ")
  }

  test("model: lazyはchangeイベントでのみ反映する") {
    val s = RV("")
    val root = mount { implicit cm => s"<input ${StRuntime.model(s, lazyUpdate = true)}>" }
    val input = q[HTMLInputElement](root, "input")
    input.value = "x"
    fire(input, "input")
    assertEquals(s.v, "")
    fire(input, "change")
    assertEquals(s.v, "x")
  }

  test("model: RV[Int]は数値として読めない入力を無視する") {
    val n = RV(1)
    val d = RV(1.5)
    val root = mount { implicit cm => s"""<input id="n" type="number" ${StRuntime.model(n)}><input id="d" ${StRuntime.model(d)}>""" }
    val input = q[HTMLInputElement](root, "#n")
    assertEquals(input.value, "1")
    input.value = "42"
    fire(input, "input")
    assertEquals(n.v, 42)
    input.value = ""
    fire(input, "input")
    assertEquals(n.v, 42)
    val dInput = q[HTMLInputElement](root, "#d")
    dInput.value = "2."
    fire(dInput, "input")
    assertEquals(d.v, 2.0)
    assertEquals(dInput.value, "2.")
  }

  test("model: チェックボックス(Boolean/List[String])とラジオボタン・select") {
    val agree = RV(false)
    val tags = RV(List("a"))
    val color = RV("red")
    val root = mount { implicit cm =>
      s"""<input id="agree" type="checkbox" ${StRuntime.model(agree)}>
         |<input id="ta" type="checkbox" value="a" ${StRuntime.model(tags)}>
         |<input id="tb" type="checkbox" value="b" ${StRuntime.model(tags)}>
         |<input id="red" type="radio" name="c" value="red" ${StRuntime.model(color)}>
         |<input id="blue" type="radio" name="c" value="blue" ${StRuntime.model(color)}>
         |<select ${StRuntime.model(color)}><option value="red">r</option><option value="blue">b</option></select>
         |""".stripMargin
    }
    val agreeE = q[HTMLInputElement](root, "#agree")
    agreeE.click()
    assertEquals(agree.v, true)
    agree := false
    assertEquals(agreeE.checked, false)

    assertEquals(q[HTMLInputElement](root, "#ta").checked, true)
    q[HTMLInputElement](root, "#tb").click()
    assertEquals(tags.v, List("a", "b"))
    q[HTMLInputElement](root, "#ta").click()
    assertEquals(tags.v, List("b"))

    assertEquals(q[HTMLInputElement](root, "#red").checked, true)
    q[HTMLInputElement](root, "#blue").click()
    assertEquals(color.v, "blue")
    val select = q[HTMLSelectElement](root, "select")
    assertEquals(select.value, "blue")
    select.value = "red"
    fire(select, "change")
    assertEquals(color.v, "red")
    assertEquals(q[HTMLInputElement](root, "#red").checked, true)
  }

  test("model: IME変換中の入力は確定まで反映しない") {
    val s = RV("")
    val root = mount { implicit cm => s"<input ${StRuntime.model(s)}>" }
    val input = q[HTMLInputElement](root, "input")
    input.value = "にほ"
    input.dispatchEvent(new InputEvent("input", new InputEventInit {
      isComposing = true
    }))
    assertEquals(s.v, "")
    input.value = "日本"
    input.dispatchEvent(new CompositionEvent("compositionend", new CompositionEventInit {}))
    assertEquals(s.v, "日本")
  }

  test("slot: 空ならfallbackを、そうでなければ渡された内容を子のComponentManagerで展開する") {
    val n = RV(1)
    val filled = StRuntime.slotOf { implicit cm => s"<b>${StRuntime.text(n)}</b>" }
    val root = mount { implicit cm =>
      s"""<p>${StRuntime.slot(Slot.empty)(_ => "fallback")}</p><div>${StRuntime.slot(filled)(_ => "fallback")}</div>"""
    }
    assertEquals(q[HTMLElement](root, "p").textContent, "fallback")
    assertEquals(q[HTMLElement](root, "div").textContent, "1")
    n := 2
    assertEquals(q[HTMLElement](root, "div").textContent, "2")
  }

  test("html: 静的な値はそのまま、ReactiveはHTMLとして展開し直す(エスケープしない)") {
    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
    val body = RV("<b>a</b>")
    val root = mount { implicit cm =>
      s"""<p>${StRuntime.html("<i>x</i>")}</p><div>${StRuntime.html(body)}</div><span>${StRuntime.html(RV(1))}</span>"""
    }
    assertEquals(q[HTMLElement](root, "p").innerHTML, "<i>x</i>")
    assertEquals(q[HTMLElement](root, "div b").textContent, "a")
    assertEquals(q[HTMLElement](root, "span").textContent, "1")
    body := "<u>c</u><u>d</u>"
    flush().map { _ =>
      val div = q[HTMLElement](root, "div")
      assertEquals(div.querySelectorAll("u").length, 2)
      assertEquals(div.querySelectorAll("b").length, 0)
      assertEquals(div.textContent, "cd")
    }
  }

  test("scopedSlot: 値を渡して子のComponentManagerで展開し、空ならfallback") {
    val n = RV(1)
    val filled = StRuntime.scopedSlotOf[String] { label => cm =>
      given ComponentManager = cm
      s"<b>$label:${StRuntime.text(n)}</b>"
    }
    val root = mount { implicit cm =>
      s"""<p>${StRuntime.scopedSlot(ScopedSlot.empty, "x")(_ => "fallback")}</p><div>${StRuntime.scopedSlot(filled, "a")(_ => "fallback")}</div>"""
    }
    assertEquals(q[HTMLElement](root, "p").textContent, "fallback")
    assertEquals(q[HTMLElement](root, "div").textContent, "a:1")
    n := 2
    assertEquals(q[HTMLElement](root, "div").textContent, "a:2")
  }

  test("injectStyle: 同じidのスタイルは1度だけ挿入する") {
    StRuntime.injectStyle("data-st-test", "p { color: red; }")
    StRuntime.injectStyle("data-st-test", "p { color: red; }")
    assertEquals(document.head.querySelectorAll("style[data-st=\"data-st-test\"]").length, 1)
  }

}
