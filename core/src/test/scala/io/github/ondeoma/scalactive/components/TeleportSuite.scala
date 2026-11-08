package io.github.ondeoma.scalactive.components

import io.github.ondeoma.scalactive.ScalactiveConfig
import io.github.ondeoma.scalactive.components.ComponentManager.{AddMethod, HTML}
import io.github.ondeoma.scalactive.reactive.*
import org.scalajs.dom.*

import scala.concurrent.Future
import scala.concurrent.duration.*
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

class TeleportSuite extends munit.FunSuite {

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

  private def mount(gen: ComponentManager => HTML): Either[String, HTMLElement] = {
    val root = document.createElement("div").asInstanceOf[HTMLElement]
    document.body.appendChild(root)
    val component = new StatelessComponent {
      def genHtml(implicit cm: ComponentManager): HTML = gen(cm)
    }
    given HTMLElement = root
    given AddMethod = AddMethod.append(root)
    component().init().map(_ => root)
  }

  private def flush(): Future[Unit] = {
    js.Promise.resolve[Unit](()).toFuture.flatMap(_ => js.Promise.resolve[Unit](()).toFuture)
  }

  private def target(id: String): HTMLElement = {
    val t = document.createElement("div").asInstanceOf[HTMLElement]
    t.id = id
    document.body.appendChild(t)
    t
  }

  test("中身を描画先に描き、リアクティブな表示とイベントはそのまま動く. 片付けると描画先から取り除く") {
    val modals = target("tp-modals")
    val show = RV(true)
    val n = RV(1)
    var clicked = 0
    val root = mount { implicit cm =>
      s"<section>${ComponentManager.ifC(show) { implicit cm =>
        ComponentManager.teleportC("#tp-modals") { implicit cm =>
          s"""<div class="modal">${text(n)}<button ${ComponentManager.evClick(clicked += 1)}>x</button></div>"""
        }
      }}</section>"
    }.fold(fail(_), identity)
    assertEquals(root.querySelectorAll(".modal").length, 0, "元の位置には描かない")
    assertEquals(modals.querySelector(".modal").textContent, "1x")
    n := 2
    modals.querySelector("button").asInstanceOf[HTMLElement].click()
    assertEquals(modals.querySelector(".modal").textContent, "2x")
    assertEquals(clicked, 1)
    show := false
    flush().flatMap { _ =>
      assertEquals(modals.childNodes.length, 0)
      show := true
      flush()
    }.map(_ => assertEquals(modals.querySelectorAll(".modal").length, 1))
  }

  test("disabledなら元の位置に描く") {
    val root = mount { implicit cm =>
      s"<section>${ComponentManager.teleportC("#none", disabled = true)(_ => "<i>here</i>")}</section>"
    }.fold(fail(_), identity)
    assertEquals(root.querySelector("section").textContent, "here")
  }

  test("描画先が見つからなければエラー") {
    val r = mount(implicit cm => s"<section>${ComponentManager.teleportC("#not-found")(_ => "<i>x</i>")}</section>")
    assert(r.isLeft, r)
  }

  private def text(rv: RV[Int])(using cm: ComponentManager): HTML = {
    io.github.ondeoma.scalactive.st.StRuntime.text(rv)
  }

}
