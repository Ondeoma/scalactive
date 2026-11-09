package io.github.ondeoma.scalactive.components

import io.github.ondeoma.scalactive.ScalactiveConfig
import io.github.ondeoma.scalactive.components.ComponentManager.{AddMethod, HTML}
import io.github.ondeoma.scalactive.controllers.NodesComponentController
import io.github.ondeoma.scalactive.reactive.*
import io.github.ondeoma.scalactive.st.StRuntime
import io.github.ondeoma.scalactive.utils.TypeAlias.GenComponentF
import org.scalajs.dom.*

import scala.concurrent.Future
import scala.concurrent.duration.*
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

/**
 * 子コンポーネントのルート要素への属性の引き継ぎ(rootCls/rootStyle/rootAttr)のテスト.
 */
class FallthroughSuite extends munit.FunSuite {

  private var savedDelay: FiniteDuration = 0.millis
  private var savedKeepScroll = true

  override def beforeAll(): Unit = {
    savedDelay = ScalactiveConfig.reloadTimerLazyDuration
    savedKeepScroll = ScalactiveConfig.keepWindowScrollPositionWhenReload
    ScalactiveConfig.reloadTimerLazyDuration = 0.millis
    // jsdomはwindow.scrollToを実装していない
    ScalactiveConfig.keepWindowScrollPositionWhenReload = false
  }

  override def afterAll(): Unit = {
    ScalactiveConfig.reloadTimerLazyDuration = savedDelay
    ScalactiveConfig.keepWindowScrollPositionWhenReload = savedKeepScroll
  }

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

  private def flush(): Future[Unit] = {
    js.Promise.resolve[Unit](()).toFuture.flatMap(_ => js.Promise.resolve[Unit](()).toFuture)
  }

  test("class/styleは子のものと合わせ、その他の属性は設定する. Reactiveに追従し、子の描き直し後も付け直す") {
    val on = RV(false)
    val color = RV("red")
    val busy = RV(true)
    var child: NodesComponentController = null
    val f: GenComponentF = (p, am) => {
      val c = new StatelessComponent {
        def genHtml(implicit cm: ComponentManager): HTML = """<button class="base" style="padding: 1px">x</button>"""
      }.apply()(p, am)
      child = c
      c
    }
    val root = mount { implicit cm =>
      s"<div>${ComponentManager.c(f,
        StRuntime.rootCls(List("primary", "on" -> on)),
        StRuntime.rootStyle(Map("color" -> color)),
        StRuntime.rootStyle("margin: 2px"),
        StRuntime.rootAttr("id", "b1"),
        StRuntime.rootAttr("aria-busy", busy))}</div>"
    }
    def btn = root.querySelector("button").asInstanceOf[HTMLElement]
    def check(onClass: Boolean, c: String, busyAttr: Boolean)(implicit loc: munit.Location): Unit = {
      assertEquals(btn.className, if (onClass) "base primary on" else "base primary")
      assertEquals(btn.style.getPropertyValue("padding"), "1px")
      assertEquals(btn.style.getPropertyValue("margin"), "2px")
      assertEquals(btn.style.getPropertyValue("color"), c)
      assertEquals(btn.id, "b1")
      assertEquals(btn.hasAttribute("aria-busy"), busyAttr)
    }
    check(onClass = false, "red", busyAttr = true)
    on := true
    color := "blue"
    busy := false
    check(onClass = true, "blue", busyAttr = false)
    val before = btn
    child.reload()
    flush().map { _ =>
      assert(!(btn eq before), "子が描き直された")
      check(onClass = true, "blue", busyAttr = false)
      on := false
      check(onClass = false, "blue", busyAttr = false)
    }
  }

}
