package io.github.ondeoma.scalactive.controllers

import io.github.ondeoma.scalactive.ScalactiveConfig
import io.github.ondeoma.scalactive.components.*
import io.github.ondeoma.scalactive.components.ComponentManager.{AddMethod, HTML}
import io.github.ondeoma.scalactive.reactive.*
import org.scalajs.dom.*

import scala.concurrent.duration.*
import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

/**
 * 描き直し(reload)の予約とまとめ方のテスト(ReloadScheduler).
 */
class ReloadSchedulerSuite extends munit.FunSuite {

  private var savedDelay: FiniteDuration = 0.millis
  private var savedKeepScroll = true

  override def beforeAll(): Unit = {
    savedDelay = ScalactiveConfig.reloadTimerLazyDuration
    savedKeepScroll = ScalactiveConfig.keepWindowScrollPositionWhenReload
    // jsdomはwindow.scrollToを実装していない
    ScalactiveConfig.keepWindowScrollPositionWhenReload = false
  }

  override def beforeEach(context: BeforeEach): Unit = ScalactiveConfig.reloadTimerLazyDuration = 0.millis

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

  /** 現在の処理の後のマイクロタスクまで待つ(タイマーは待たない) */
  private def afterMicrotasks(): Future[Unit] = {
    js.Promise.resolve[Unit](()).toFuture.flatMap(_ => js.Promise.resolve[Unit](()).toFuture)
  }

  private def sleep(ms: Double): Future[Unit] = {
    val p = Promise[Unit]()
    window.setTimeout(() => p.success(()), ms)
    p.future
  }

  test("既定(0ms)では、タイマーを待たずに現在の処理の直後に描き直す") {
    assertEquals(ScalactiveConfig.reloadTimerLazyDuration, 0.millis)
    val b = RV(true)
    val root = mount(implicit cm => s"<div>${ComponentManager.ifC(b)(_ => "<p>shown</p>")}</div>")
    b := false
    assertEquals(root.querySelectorAll("p").length, 1, "同期的にはまだ描き直さない")
    afterMicrotasks().map { _ =>
      assertEquals(root.querySelectorAll("p").length, 0)
    }
  }

  test("同じ処理の中での複数の変更は1回の描き直しにまとめる") {
    val b = RV(true)
    var renders = 0
    val root = mount { implicit cm =>
      s"<div>${ComponentManager.ifC(b) { _ => renders += 1; "<p>shown</p>" }}</div>"
    }
    assertEquals(renders, 1)
    b := false
    b := true
    b := false
    b := true
    afterMicrotasks().map { _ =>
      assertEquals(renders, 2)
      assertEquals(root.querySelectorAll("p").length, 1)
    }
  }

  test("親と子の両方が予約されたら親から描き直し、子は親の描き直しの分だけ") {
    val outer = RV(true)
    val inner = RV(true)
    var innerRenders = 0
    val root = mount { implicit cm =>
      s"<div>${
        ComponentManager.ifC(outer) { implicit cm =>
          s"<section>${ComponentManager.ifC(inner) { _ => innerRenders += 1; "<p>inner</p>" }}</section>"
        }
      }</div>"
    }
    assertEquals(innerRenders, 1)
    // 子を先に予約してから親を予約する(どちらも最終的な値は同じだが、途中で変化して通知される)
    inner := false
    inner := true
    outer := false
    outer := true
    afterMicrotasks().map { _ =>
      assertEquals(innerRenders, 2)
      assertEquals(root.querySelectorAll("p").length, 1)
    }
  }

  test("待ち時間を指定すると、最後の変更からその時間だけ待って描き直す") {
    ScalactiveConfig.reloadTimerLazyDuration = 40.millis
    val b = RV(true)
    val root = mount(implicit cm => s"<div>${ComponentManager.ifC(b)(_ => "<p>shown</p>")}</div>")
    b := false
    for {
      _ <- afterMicrotasks()
      _ = assertEquals(root.querySelectorAll("p").length, 1, "マイクロタスクでは描き直さない")
      _ <- sleep(80)
    } yield assertEquals(root.querySelectorAll("p").length, 0)
  }

}
