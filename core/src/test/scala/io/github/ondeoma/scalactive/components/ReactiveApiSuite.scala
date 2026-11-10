package io.github.ondeoma.scalactive.components

import io.github.ondeoma.scalactive.ScalactiveConfig
import io.github.ondeoma.scalactive.components.ComponentManager.*
import io.github.ondeoma.scalactive.components.texts.TextComponent
import org.scalajs.dom.*

import scala.concurrent.Future
import scala.concurrent.duration.*
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
 * computed・watch・untracked・mount・nextTick のテスト.
 */
class ReactiveApiSuite extends munit.FunSuite {

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

  private def newRoot(): HTMLElement = {
    val root = document.createElement("div").asInstanceOf[HTMLElement]
    document.body.appendChild(root)
    root
  }

  private def comp(gen: ComponentManager => HTML): GenComponentF = {
    new StatelessComponent {
      def genHtml(implicit cm: ComponentManager): HTML = gen(cm)
    }.apply()(_, _)
  }

  private def mountOrFail(root: Element, gen: ComponentManager => HTML): MountHandle = {
    mount(root, comp(gen)) match {
      case Left(e) => fail(e)
      case Right(h) => h
    }
  }

  // ---- computed ----

  test("computedは読んだ値の変化で計算し直す") {
    val a = RV(1)
    val b = RV(2)
    val sum = computed(a.v + b.v)
    var log = List.empty[(Int, Int)]
    sum.addWatcher { case (o, n) => log :+= (o, n) }
    assertEquals(sum.v, 3)
    a := 10
    assertEquals(sum.v, 12)
    b := 5
    assertEquals(sum.v, 15)
    assertEquals(log, List((3, 12), (12, 15)))
  }

  test("computedは計算し直すたびに依存を集め直す") {
    val flag = RV(true)
    val x = RV(1)
    val y = RV(100)
    var runs = 0
    val c = computed {
      runs += 1
      if (flag.v) x.v else y.v
    }
    assertEquals((c.v, runs), (1, 1))
    y := 200
    assertEquals(runs, 1, "読んでいないyでは計算し直さない")
    flag := false
    assertEquals((c.v, runs), (200, 2))
    x := 2
    assertEquals(runs, 2, "読まなくなったxの監視は解除される")
    y := 300
    assertEquals((c.v, runs), (300, 3))
  }

  test("computedは値が等しければ通知しない") {
    val n = RV(1)
    val big = computed(n.v > 3)
    var count = 0
    big.addWatcher(_ => count += 1)
    n := 2
    n := 3
    assertEquals(count, 0)
    n := 4
    assertEquals((count, big.v), (1, true))
  }

  test("computedはcomputed・mapC・RVListも依存にできる") {
    val list = RVList(List(1, 2))
    val n = RV(10)
    val doubled = n.mapC(_ * 2)
    val inner = computed(list.v.sum)
    val total = computed(inner.v + doubled.v)
    assertEquals(total.v, 23)
    list.add(3)
    assertEquals(total.v, 26)
    n := 1
    assertEquals(total.v, 8)
  }

  test("untrackedの中で読んだ値は依存に入らない") {
    val a = RV(1)
    val b = RV(10)
    val c = computed(a.v + untracked(b.v))
    b := 20
    assertEquals(c.v, 11)
    a := 2
    assertEquals(c.v, 22)
  }

  test("コンポーネントの中のcomputedは、片付けられると監視を解除する") {
    val n = RV(1)
    var runs = 0
    val root = newRoot()
    val h = mountOrFail(root, { implicit cm =>
      val c = computed { runs += 1; n.v * 2 }
      s"<p>${c.v}</p>"
    })
    assertEquals(root.textContent, "2")
    n := 2
    assertEquals(runs, 2)
    h.unmount()
    n := 3
    assertEquals(runs, 2)
    assertEquals(n.numWatchers, 0)
    root.remove()
  }

  // ---- watch ----

  test("watchは値の変化で前の値と新しい値を受け取り、stopで止まる") {
    val n = RV(1)
    var log = List.empty[(Int, Int)]
    val w = watch(n)((o, v) => log :+= (o, v))
    n := 2
    n := 2
    n := 5
    w.stop()
    n := 6
    assertEquals(log, List((1, 2), (2, 5)))
    assert(w.isStopped)
    assertEquals(n.numWatchers, 0)
  }

  test("watchのimmediateは登録したときにも実行する") {
    val n = RV(7)
    var log = List.empty[(Int, Int)]
    val w = watch(n, immediate = true)((o, v) => log :+= (o, v))
    n := 8
    assertEquals(log, List((7, 7), (7, 8)))
    w.stop()
  }

  test("watchはcomputedと組み合わせて複数の値を見られる") {
    val a = RV(1)
    val b = RV("x")
    var log = List.empty[(Int, String)]
    val w = watch(computed((a.v, b.v)))((_, v) => log :+= v)
    a := 2
    b := "y"
    assertEquals(log, List((2, "x"), (2, "y")))
    w.stop()
  }

  test("コンポーネントの中・onMountedの中のwatchは、片付けられると自動で解除される") {
    val n = RV(0)
    var log = List.empty[String]
    val root = newRoot()
    val h = mountOrFail(root, { implicit cm =>
      watch(n)((_, v) => log :+= s"render:$v")
      onMounted {
        // onMountedの中ではComponentManagerを受け取らないが、実行中のコンポーネントに登録される
        watch(n)((_, v) => log :+= s"mounted:$v")
      }
      "<p>w</p>"
    })
    nextTick().map { _ =>
      n := 1
      // 監視の実行順は決まっていない
      assertEquals(log.sorted, List("mounted:1", "render:1"))
      h.unmount()
      n := 2
      assertEquals(log.size, 2)
      assertEquals(n.numWatchers, 0)
      root.remove()
    }
  }

  // ---- mount ----

  test("mountで描画し、unmountで取り除いてonUnmountedを実行する") {
    val root = newRoot()
    val n = RV(1)
    var unmounted = 0
    val h = mountOrFail(root, { implicit cm =>
      onUnmounted(unmounted += 1)
      s"<p>${%(TextComponent(n)(_.toString))}</p>"
    })
    assertEquals(root.textContent, "1")
    n := 2
    nextTick().map { _ =>
      assertEquals(root.textContent, "2")
      h.unmount()
      h.unmount()
      assertEquals(root.innerHTML, "")
      assertEquals(unmounted, 1)
      assert(!h.isMounted)
      root.remove()
    }
  }

  test("onMountedの中でmountしたものは、親が片付けられると一緒に片付けられる") {
    val root = newRoot()
    val external = newRoot()
    var childUnmounted = false
    val h = mountOrFail(root, { implicit cm =>
      onMounted {
        // 外部のライブラリが管理する要素に描画する想定
        mount(external, comp { implicit cm =>
          onUnmounted { childUnmounted = true }
          "<i>node</i>"
        })
      }
      "<p>parent</p>"
    })
    nextTick().map { _ =>
      assertEquals(external.textContent, "node")
      h.unmount()
      assertEquals(external.innerHTML, "")
      assert(childUnmounted)
      root.remove()
      external.remove()
    }
  }

  test("mount(el) { ... } でコンポーネントのapplyをそのまま渡せる") {
    val root = newRoot()
    val c = new StatelessComponent {
      def genHtml(implicit cm: ComponentManager): HTML = "<b>curried</b>"
    }
    mount(root) { c() } match {
      case Left(e) => fail(e)
      case Right(h) =>
        assertEquals(root.textContent, "curried")
        h.unmount()
        assertEquals(root.innerHTML, "")
    }
    root.remove()
  }

  // ---- nextTick ----

  test("nextTickは描き直しとonMountedが終わってから完了する") {
    val root = newRoot()
    val show = RV(false)
    var mounted = false
    val h = mountOrFail(root, { implicit cm =>
      s"<div>${ifC(show) { implicit cm =>
        onMounted { mounted = true }
        """<input id="nt-input">"""
      }}</div>"
    })
    show := true
    assert(document.getElementById("nt-input") == null, "描き直しは同期的には行わない")
    nextTick().map { _ =>
      assert(document.getElementById("nt-input") != null)
      assert(mounted)
      h.unmount()
      root.remove()
    }
  }

  test("nextTickは描き直しを遅らせている場合も、その描き直しを待つ") {
    ScalactiveConfig.reloadTimerLazyDuration = 30.millis
    val root = newRoot()
    val n = RV(1)
    val h = mountOrFail(root, { implicit cm =>
      s"<p>${%(TextComponent(n)(_.toString))}</p>"
    })
    n := 2
    nextTick().map { _ =>
      assertEquals(root.textContent, "2")
    }.andThen { _ =>
      ScalactiveConfig.reloadTimerLazyDuration = 0.millis
      h.unmount()
      root.remove()
    }
  }

  test("待つものがなければnextTickはすぐに完了する") {
    nextTick().map(_ => assert(true))
  }

}
