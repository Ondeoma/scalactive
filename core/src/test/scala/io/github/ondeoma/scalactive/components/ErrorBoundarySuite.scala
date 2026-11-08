package io.github.ondeoma.scalactive.components

import io.github.ondeoma.scalactive.ScalactiveConfig
import io.github.ondeoma.scalactive.components.ComponentManager.{AddMethod, HTML}
import io.github.ondeoma.scalactive.reactive.*
import org.scalajs.dom.*

import scala.concurrent.Future
import scala.concurrent.duration.*
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

class ErrorBoundarySuite extends munit.FunSuite {

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

  /** 監視の数を数えられるRV */
  private final class Counted[A](a: A) extends RV[A](a) {
    def watcherCount: Int = watchers.size
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

  private val fallback: ErrorInfo => ComponentManager => HTML = err => _ => s"""<p class="fb">${err.message}</p>"""

  test("描画中の例外を受け止めてfallbackを表示し、途中で作った監視は片付ける") {
    val src = new Counted(1)
    var errors = List.empty[String]
    val root = mount { implicit cm =>
      s"<div>${ComponentManager.errorBoundaryC(fallback, e => errors :+= e.getMessage) { implicit cm =>
        val label = src.mapC(_ + 1)
        if (label.v > 0) throw new RuntimeException("boom")
        "<p>ok</p>"
      }}</div>"
    }
    assertEquals(root.querySelector(".fb").textContent, "boom")
    assertEquals(errors, List("boom"))
    assertEquals(src.watcherCount, 0, "例外の前に作ったmapCの監視は解除される")
  }

  test("後から起きる子孫の描き直しの例外を受け止め、retryで中身を描き直す") {
    val flag = RV(false)
    var broken = true
    val root = mount { implicit cm =>
      s"<div>${ComponentManager.errorBoundaryC(err => implicit cm => s"""<p class="fb">${err.message}</p><button ${ComponentManager.evClick(err.retry())}>retry</button>""", _ => ()) { implicit cm =>
        s"<section>${ComponentManager.ifC(flag) { _ => if (broken) throw new RuntimeException("later") else "<b>shown</b>" }}</section>"
      }}</div>"
    }
    assertEquals(root.querySelector("section").textContent, "")
    flag := true
    flush().flatMap { _ =>
      flush()
    }.flatMap { _ =>
      assertEquals(root.querySelector(".fb").textContent, "later")
      broken = false
      root.querySelector("button").asInstanceOf[HTMLElement].click()
      flush()
    }.map { _ =>
      assertEquals(root.querySelector("section b").textContent, "shown")
      assertEquals(root.querySelector(".fb"), null)
    }
  }

  test("onMountedの中の例外も受け止める") {
    val root = mount { implicit cm =>
      s"<div>${ComponentManager.errorBoundaryC(fallback, _ => ()) { implicit cm =>
        ComponentManager.onMounted(throw new RuntimeException("mounted"))
        "<p>ok</p>"
      }}</div>"
    }
    flush().flatMap(_ => flush()).map { _ =>
      assertEquals(root.querySelector(".fb").textContent, "mounted")
    }
  }

  test("入れ子の境界では内側が受け止め、外側の中身はそのまま") {
    val root = mount { implicit cm =>
      s"<div>${ComponentManager.errorBoundaryC(_ => _ => "<p class='outer'>outer</p>", _ => ()) { implicit cm =>
        s"<span>keep</span>${ComponentManager.errorBoundaryC(fallback, _ => ()) { _ => throw new RuntimeException("inner") }}"
      }}</div>"
    }
    assertEquals(root.querySelector(".fb").textContent, "inner")
    assertEquals(root.querySelector("span").textContent, "keep")
    assertEquals(root.querySelector(".outer"), null)
  }

}
