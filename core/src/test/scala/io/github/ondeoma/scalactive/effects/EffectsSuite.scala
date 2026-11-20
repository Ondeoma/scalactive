package io.github.ondeoma.scalactive.effects

import io.github.ondeoma.scalactive.ScalactiveConfig
import io.github.ondeoma.scalactive.components.*
import io.github.ondeoma.scalactive.components.ComponentManager.{AddMethod, HTML}
import io.github.ondeoma.scalactive.effects.Effects.*
import io.github.ondeoma.scalactive.reactive.RV
import org.scalajs.dom.*

import scala.concurrent.duration.*
import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

/** 後片付けを自動で行うリスナー・タイマー(B)と、非同期の状態(C)のテスト */
class EffectsSuite extends munit.FunSuite {

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

  private def sleep(ms: Double): Future[Unit] = {
    val p = Promise[Unit]()
    window.setTimeout(() => p.success(()), ms)
    p.future
  }

  /** 条件で表示・非表示を切り替えるコンポーネントの中でbodyを実行する(showをfalseにすると片付けられる) */
  private def mountWhile(show: RV[Boolean])(body: ComponentManager ?=> Unit): HTMLElement = {
    val root = document.createElement("div").asInstanceOf[HTMLElement]
    document.body.appendChild(root)
    val component = new StatelessComponent {
      def genHtml(implicit cm: ComponentManager): HTML = {
        ifC(show) { implicit c =>
          body(using c)
          "<p>x</p>"
        }
      }
    }
    given HTMLElement = root
    given AddMethod = AddMethod.append(root)
    component().init() match {
      case Left(e) => fail(e)
      case Right(_) => root
    }
  }

  test("useEventListener: 呼ばれ、コンポーネントが片付けられると解除される") {
    val show = RV(true)
    var n = 0
    mountWhile(show) {
      useEventListener[Event](window, "x-test")(_ => n += 1)
    }
    window.dispatchEvent(new Event("x-test"))
    assertEquals(n, 1)
    show := false
    sleep(0).map { _ =>
      window.dispatchEvent(new Event("x-test"))
      assertEquals(n, 1)
    }
  }

  test("useEventListener: コンポーネントの外ではstop()で解除する") {
    var n = 0
    val h = useEventListener[Event](document, "x-test2")(_ => n += 1)
    document.dispatchEvent(new Event("x-test2"))
    h.stop()
    document.dispatchEvent(new Event("x-test2"))
    assertEquals(n, 1)
    assert(h.isStopped)
  }

  test("useInterval・useTimeout: 片付けられると止まる") {
    val show = RV(true)
    var ticks = 0
    var fired = false
    mountWhile(show) {
      useInterval(10)(ticks += 1)
      useTimeout(60) { fired = true }
    }
    sleep(35).flatMap { _ =>
      assert(ticks >= 2, s"ticks=$ticks")
      show := false
      sleep(5)
    }.flatMap { _ =>
      val t = ticks
      sleep(70).map { _ =>
        assertEquals(ticks, t)
        assert(!fired)
      }
    }
  }

  test("useVisiblePolling: 隠れている間は止まり、表示に戻るとすぐ実行する") {
    var state = "visible"
    js.Object.defineProperty(document, "visibilityState", js.Dynamic.literal(get = (() => state): js.Function0[String], configurable = true).asInstanceOf[js.PropertyDescriptor])
    var n = 0
    val h = useVisiblePolling(10, immediate = true)(n += 1)
    assertEquals(n, 1)
    sleep(25).flatMap { _ =>
      assert(n >= 2, s"n=$n")
      state = "hidden"
      document.dispatchEvent(new Event("visibilitychange"))
      val stopped = n
      sleep(30).map { _ =>
        assertEquals(n, stopped)
        state = "visible"
        document.dispatchEvent(new Event("visibilitychange"))
        assertEquals(n, stopped + 1)
        h.stop()
      }
    }.andThen { case _ => js.special.delete(document, "visibilityState") }
  }

  test("useDebounce: 最後の呼び出しだけを、待ってから実行する. flush・cancel") {
    var got = List.empty[String]
    val d = useDebounce[String](20)(q => got :+= q)
    d("a")
    d("ab")
    d("abc")
    assertEquals(got, Nil)
    assert(d.isPending)
    sleep(40).map { _ =>
      assertEquals(got, List("abc"))
      d("x")
      d.flush()
      assertEquals(got, List("abc", "x"))
      d("y")
      d.cancel()
      d.stop()
      d("z")
    }.flatMap(_ => sleep(40)).map(_ => assertEquals(got, List("abc", "x")))
  }

  test("useThrottle: 最初はすぐ、間引いた間の最後の呼び出しは間隔があいた後に実行する") {
    var got = List.empty[Int]
    val t = useThrottle[Int](30)(i => got :+= i)
    t(1)
    t(2)
    t(3)
    assertEquals(got, List(1))
    sleep(50).map { _ =>
      assertEquals(got, List(1, 3))
      t.stop()
    }
  }

  test("useLatest: 後から始めた処理があれば、前の結果は届かない") {
    val l = useLatest()
    val p1 = Promise[Int]()
    val p2 = Promise[Int]()
    var got = List.empty[Int]
    l(p1.future).foreach(got :+= _)
    l(p2.future).foreach(got :+= _)
    p2.success(2)
    p1.success(1)
    sleep(5).map(_ => assertEquals(got, List(2)))
  }

  test("useAsync: 読み込み中・値・エラー. 読み直し中は前の値のまま、古い応答は捨てる") {
    var next = List(Promise[Int](), Promise[Int](), Promise[Int]())
    def load(): Future[Int] = {
      val p = next.head
      next = next.tail
      p.future
    }
    val ps = next
    val a = useAsync(load())
    assert(a.loading.v)
    assertEquals(a.data.v, None)
    ps(0).success(1)
    sleep(1).flatMap { _ =>
      assertEquals(a.data.v, Some(1))
      assert(!a.loading.v)
      assert(a.ready.v)
      a.reload()
      a.reload()
      assert(a.loading.v)
      assertEquals(a.getOrElse(0), 1, "読み直し中は前の値")
      ps(2).failure(new RuntimeException("NG"))
      ps(1).success(99)
      sleep(1)
    }.map { _ =>
      assertEquals(a.data.v, Some(1), "古い応答(99)は捨てる")
      assert(a.failed.v)
      assertEquals(a.errorMessage.v, "NG")
      a.set(5)
      assertEquals(a.data.v, Some(5))
      assert(!a.failed.v)
      a.update(_ + 1)
      assertEquals(a.data.v, Some(6))
    }
  }

  test("useAsync: immediate = false なら reload() まで読み込まない") {
    var n = 0
    val a = useAsync({ n += 1; Future.successful(n) }, immediate = false)
    assertEquals(n, 0)
    a.reload()
    sleep(1).map { _ =>
      assertEquals(a.data.v, Some(1))
    }
  }

  test("useAction: 実行中はbusyで、二重に実行しない. 失敗はerrorに入る") {
    val act = useAction()
    val p = Promise[Int]()
    var runs = 0
    val f1 = act.run({ runs += 1; p.future })
    assert(act.busy.v)
    val f2 = act.run({ runs += 1; Future.successful(0) })
    assertEquals(runs, 1)
    p.success(1)
    f2.failed.map(e => assertEquals(e, ActionState.Busy)).flatMap(_ => f1).flatMap { r =>
      assertEquals(r, 1)
      assert(!act.busy.v)
      act.run(Future.failed[Int](new RuntimeException("保存に失敗")))
      sleep(1)
    }.map { _ =>
      assert(act.failed.v)
      assertEquals(act.errorMessage.v, "保存に失敗")
      assert(!act.busy.v)
      act.clearError()
      assert(!act.failed.v)
    }
  }

  test("useAction: fが例外を投げても busy を戻す") {
    val act = useAction()
    act.run[Int](throw new RuntimeException("x"))
    sleep(1).map { _ =>
      assert(!act.busy.v)
      assertEquals(act.errorMessage.v, "x")
    }
  }

}
