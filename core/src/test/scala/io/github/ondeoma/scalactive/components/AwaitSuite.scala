package io.github.ondeoma.scalactive.components

import io.github.ondeoma.scalactive.ScalactiveConfig
import io.github.ondeoma.scalactive.components.ComponentManager.{AddMethod, HTML}
import io.github.ondeoma.scalactive.reactive.*
import io.github.ondeoma.scalactive.utils.TypeAlias.GenComponentF
import org.scalajs.dom.*

import scala.concurrent.duration.*
import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

class AwaitSuite extends munit.FunSuite {

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

  /** Futureの完了と描き直しを待つ */
  private def settle(): Future[Unit] = {
    val p = Promise[Unit]()
    window.setTimeout(() => p.success(()), 5)
    p.future
  }

  test("完了するまでpending、完了したら結果を表示する") {
    val p = Promise[String]()
    val root = mount { implicit cm =>
      s"<div>${ComponentManager.awaitC(p.future, pending = _ => "<i>loading</i>")(s => _ => s"<b>$s</b>")}</div>"
    }
    assertEquals(root.textContent, "loading")
    p.success("done")
    settle().map(_ => assertEquals(root.querySelector("b").textContent, "done"))
  }

  test("失敗したらerrorを表示し、retryでもう1度読み込む") {
    var calls = 0
    var next = Promise[Int]()
    val root = mount { implicit cm =>
      s"<div>${ComponentManager.awaitC({ calls += 1; next.future },
        error = err => implicit cm => s"""<p class="err">${err.message}</p><button ${ComponentManager.evClick(err.retry())}>retry</button>""")(
        n => _ => s"<b>$n</b>")}</div>"
    }
    next.failure(new RuntimeException("failed"))
    settle().flatMap { _ =>
      assertEquals(root.querySelector(".err").textContent, "failed")
      next = Promise[Int]()
      root.querySelector("button").asInstanceOf[HTMLElement].click()
      next.success(42)
      settle()
    }.map { _ =>
      assertEquals(calls, 2)
      assertEquals(root.querySelector("b").textContent, "42")
    }
  }

  test("完了前に片付けられたら、完了しても描き直さない") {
    val p = Promise[String]()
    val show = RV(true)
    var rendered = 0
    val root = mount { implicit cm =>
      s"<div>${ComponentManager.ifC(show) { implicit cm =>
        ComponentManager.awaitC(p.future)(s => _ => { rendered += 1; s"<b>$s</b>" })
      }}</div>"
    }
    show := false
    settle().flatMap { _ =>
      p.success("late")
      settle()
    }.map { _ =>
      assertEquals(rendered, 0)
      assertEquals(root.querySelectorAll("b").length, 0)
    }
  }

  test("asyncComponent: 読み込んだコンポーネントを描画する") {
    val p = Promise[GenComponentF]()
    val root = mount { implicit cm =>
      s"<div>${ComponentManager.%(ComponentManager.asyncComponent(() => p.future, _ => "<i>loading</i>"))}</div>"
    }
    assertEquals(root.textContent, "loading")
    p.success(new StatelessComponent {
      def genHtml(implicit cm: ComponentManager): HTML = "<section>heavy</section>"
    }.apply()(_, _))
    settle().map(_ => assertEquals(root.querySelector("section").textContent, "heavy"))
  }

}
