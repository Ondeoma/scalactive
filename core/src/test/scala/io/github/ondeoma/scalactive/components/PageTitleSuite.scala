package io.github.ondeoma.scalactive.components

import io.github.ondeoma.scalactive.ScalactiveConfig
import io.github.ondeoma.scalactive.components.ComponentManager.*
import org.scalajs.dom.*

import scala.concurrent.duration.*
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/** 外部の状態(コンポーネントの外で定義した値) */
object PageTitleSuiteStore {
  val shared: RV[Int] = RV(0)
}

/**
 * pageTitle と、onMountedの中で作った値の扱いのテスト.
 */
class PageTitleSuite extends munit.FunSuite {

  override def beforeAll(): Unit = {
    ScalactiveConfig.reloadTimerLazyDuration = 0.millis
    ScalactiveConfig.keepWindowScrollPositionWhenReload = false
  }

  override def beforeEach(context: BeforeEach): Unit = {
    document.title = "元"
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
    mount(root, comp(gen)).fold(e => fail(e), identity)
  }

  /** .stの def mounted と同じく、ComponentManagerを受け取らずにタイトルを指定する */
  private def titleInHook(t: String): Unit = pageTitle(t)

  test("表示中だけタイトルを変え、片付けると元に戻す") {
    val root = newRoot()
    val h = mountOrFail(root, { implicit cm =>
      pageTitle("一覧")
      "<p>x</p>"
    })
    assertEquals(document.title, "一覧")
    h.unmount()
    assertEquals(document.title, "元")
    root.remove()
  }

  test("mountedは子から先に実行されるが、子のタイトルを優先する") {
    val root = newRoot()
    val show = RV(true)
    val h = mountOrFail(root, { implicit cm =>
      onMounted(titleInHook("レイアウト"))
      s"<div>${ifC(show) { implicit cm =>
        %(comp { implicit cm =>
          onMounted(titleInHook("ページ"))
          "<i>child</i>"
        })
      }}</div>"
    })
    nextTick().flatMap { _ =>
      assertEquals(document.title, "ページ")
      show := false
      nextTick()
    }.map { _ =>
      assertEquals(document.title, "レイアウト", "子が片付けられたら親のタイトルに戻る")
      h.unmount()
      assertEquals(document.title, "元")
      root.remove()
    }
  }

  test("リアクティブな値のタイトルと、titleTemplate") {
    ScalactiveConfig.titleTemplate = "%s | App"
    val name = RV("読み込み中")
    val root = newRoot()
    val h = mountOrFail(root, { implicit cm =>
      pageTitle(name)
      "<p>x</p>"
    })
    assertEquals(document.title, "読み込み中 | App")
    name := "商品A"
    assertEquals(document.title, "商品A | App")
    h.unmount()
    ScalactiveConfig.titleTemplate = "%s"
    assertEquals(name.numWatchers, 0)
    root.remove()
  }

  test("onMountedの中で初めて使われたオブジェクトの値は、コンポーネントと一緒に止めない") {
    var log = List.empty[Int]
    val root = newRoot()
    val h = mountOrFail(root, { implicit cm =>
      onMounted(useStore(v => log :+= v))
      "<p>x</p>"
    })
    nextTick().map { _ =>
      val other = PageTitleSuiteStore.shared.addWatcher { case (_, n) => log :+= -n }
      h.unmount()
      PageTitleSuiteStore.shared := 1
      assertEquals(log, List(-1), "コンポーネントのwatchは止まり、他の監視は残る")
      other.abort()
      root.remove()
    }
  }

  /** ComponentManagerの外(.stのmountedなど)で、オブジェクトを初めて使う */
  private def useStore(f: Int => Unit): Unit = {
    watch(PageTitleSuiteStore.shared)((_, n) => f(n))
  }

}
