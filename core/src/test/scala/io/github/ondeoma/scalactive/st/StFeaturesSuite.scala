package io.github.ondeoma.scalactive.st

import io.github.ondeoma.scalactive.ScalactiveConfig
import io.github.ondeoma.scalactive.components.*
import io.github.ondeoma.scalactive.components.ComponentManager.{AddMethod, GenComponentF, HTML}
import io.github.ondeoma.scalactive.components.routes.RouterLink
import io.github.ondeoma.scalactive.reactive.*
import org.scalajs.dom.*

import scala.concurrent.Future
import scala.concurrent.duration.*
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

/**
 * propsの値が同じなら子を作り直さないこと・描き直しでフォーカスを保つこと(A-3)、
 * 子のルート要素へのscopedの属性(A-2)、ディレクティブ(D)のテスト.
 */
class StFeaturesSuite extends munit.FunSuite {

  private var saved: (FiniteDuration, Boolean) = null

  override def beforeAll(): Unit = {
    saved = (ScalactiveConfig.reloadTimerLazyDuration, ScalactiveConfig.keepWindowScrollPositionWhenReload)
    ScalactiveConfig.reloadTimerLazyDuration = 0.millis
    ScalactiveConfig.keepWindowScrollPositionWhenReload = false
  }

  override def afterAll(): Unit = {
    ScalactiveConfig.reloadTimerLazyDuration = saved._1
    ScalactiveConfig.keepWindowScrollPositionWhenReload = saved._2
  }

  private def flush(): Future[Unit] = ComponentManager.nextTick()

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

  private def q[E <: Element](root: HTMLElement, selector: String): E = root.querySelector(selector).asInstanceOf[E]

  private object Child extends StatefulPropsComponent[(String, Int), Int] {
    var inits = 0
    def initState(p: (String, Int)): Int = { inits += 1; inits }
    def genHtml(p: (String, Int), n: Int)(implicit cm: ComponentManager): HTML = s"<b>${StRuntime.text(p._1)}${p._2}#$n</b>"
    def of(name: String, count: Int): GenComponentF = (r, am) => apply((name, count))(r, am)
  }

  test("props: 読んだ値が変わっても、propsの値が前と同じなら子を作り直さない") {
    Child.inits = 0
    val items = RV(Map("a" -> 1, "b" -> 2))
    val root = mount { implicit cm =>
      s"<div>${StRuntime.componentOfProps({ c => given ComponentManager = c; Child.of(StRuntime.prop("a"), StRuntime.prop(items.v("a"))) }, 2)}</div>"
    }
    assertEquals(q[HTMLElement](root, "div").textContent, "a1#1")
    items := items.v.updated("b", 3)
    flush().flatMap { _ =>
      assertEquals(Child.inits, 1, "aの値は同じなので作り直さない")
      items := items.v.updated("a", 5)
      flush()
    }.flatMap { _ =>
      assertEquals(q[HTMLElement](root, "div").textContent, "a5#2")
      // 作り直さなかった後も、監視は続いている
      items := items.v.updated("a", 6)
      flush()
    }.map { _ =>
      assertEquals(q[HTMLElement](root, "div").textContent, "a6#3")
    }
  }

  test("props: propsの数が分からない場合(-1)は、これまでどおり作り直す") {
    Child.inits = 0
    val items = RV(Map("a" -> 1, "b" -> 2))
    mount { implicit cm =>
      s"<div>${StRuntime.componentOf { c => given ComponentManager = c; Child.of(StRuntime.prop("a"), StRuntime.prop(items.v("a"))) }}</div>"
    }
    items := items.v.updated("b", 3)
    flush().map(_ => assertEquals(Child.inits, 2))
  }

  private object InputChild extends StatelessPropsComponent[String] {
    def genHtml(v: String)(implicit cm: ComponentManager): HTML = s"<p><label>${StRuntime.text(v)}</label><input class='t' value='${StRuntime.escape(v)}'></p>"
    def of(v: String): GenComponentF = (r, am) => apply(v)(r, am)
  }

  test("描き直しで入力欄が作り直されても、フォーカスとカーソルの位置を戻す") {
    val v = RV("abc")
    val root = mount { implicit cm =>
      s"<section>${StRuntime.componentOfProps({ c => given ComponentManager = c; InputChild.of(StRuntime.prop(v.v)) }, 1)}</section>"
    }
    val input = q[HTMLInputElement](root, "input.t")
    input.focus()
    input.setSelectionRange(1, 2)
    v := "abcd"
    flush().map { _ =>
      val now = q[HTMLInputElement](root, "input.t")
      assert(now ne input, "作り直されている")
      assert(document.activeElement eq now, "フォーカスが戻っている")
      assertEquals((now.selectionStart, now.selectionEnd), (1, 2))
    }
  }

  test("scope: 子のルート要素にscopedの属性を付け、ルート要素のない子では何もしない") {
    val root = mount { implicit cm =>
      s"<nav>${ComponentManager.c(RouterLink("/a"), StRuntime.scope("data-st-test"))}</nav>"
    }
    assert(q[HTMLElement](root, "a").hasAttribute("data-st-test"))
  }

  test("directive: mountedはDOMに挿入された後、updatedは描き直しの後、unmountedは片付けたとき") {
    val show = RV(true)
    val body = RV("<i>1</i>")
    var log = List.empty[String]
    val d = new Directive[String] {
      override def mounted(el: HTMLElement, value: String): Unit = log :+= s"mounted:${el.isConnected}:${el.textContent}"
      override def updated(el: HTMLElement, value: String, oldValue: String): Unit = log :+= s"updated:$oldValue->$value:${el.textContent}"
      override def unmounted(el: HTMLElement): Unit = log :+= "unmounted"
    }
    mount { implicit cm =>
      ComponentManager.ifC(show) { implicit c =>
        s"<div ${StRuntime.directive(d, body)}>${StRuntime.html(body)}</div>"
      }
    }
    flush().flatMap { _ =>
      assertEquals(log, List("mounted:true:1"))
      body := "<i>2</i>"
      flush()
    }.flatMap { _ =>
      assertEquals(log.last, "updated:<i>1</i>-><i>2</i>:2")
      show := false
      flush()
    }.map { _ =>
      assertEquals(log.last, "unmounted")
    }
  }

  test("v-focus: 挿入されたときと、条件が真に変わったときにフォーカスする") {
    val editing = RV(false)
    val root = mount { implicit cm =>
      s"<input class='a' ${StRuntime.directive(Directive.focus(), true)}><input class='b' ${StRuntime.directive(Directive.focus(select = true), editing)}>"
    }
    flush().flatMap { _ =>
      assert(document.activeElement eq q[Element](root, "input.a"))
      editing := true
      flush()
    }.map { _ =>
      assert(document.activeElement eq q[Element](root, "input.b"))
    }
  }

  test("v-click-outside: 要素の外を押したときだけ実行し、片付けると解除する") {
    val show = RV(true)
    var n = 0
    val root = mount { implicit cm =>
      "<p class='out'>out</p>" + ComponentManager.ifC(show) { implicit c =>
        s"<div class='menu' ${StRuntime.directive(Directive.clickOutside(), (_: Event) => n += 1)}><span>in</span></div>"
      }
    }
    def press(e: Element): Unit = e.dispatchEvent(new Event("pointerdown", new EventInit {
      bubbles = true
    }))
    flush().flatMap { _ =>
      press(q[Element](root, ".menu span"))
      assertEquals(n, 0)
      press(q[Element](root, "p.out"))
      assertEquals(n, 1)
      show := false
      flush()
    }.map { _ =>
      press(q[Element](root, "p.out"))
      assertEquals(n, 1)
    }
  }

}
