package io.github.ondeoma.scalactive.testing

import io.github.ondeoma.scalactive.Devtools
import io.github.ondeoma.scalactive.components.*
import io.github.ondeoma.scalactive.reactive.*
import io.github.ondeoma.scalactive.st.StRuntime
import org.scalajs.dom.*

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
 * TestKit(テスト用のヘルパー)とDevtools(診断機能)のテスト.
 */
class TestKitSuite extends munit.FunSuite {

  override def beforeAll(): Unit = TestKit.useTestConfig()

  override def afterAll(): Unit = TestKit.restoreConfig()

  test("TestKit: 描画・操作・描き直しの待機・片付け") {
    val name = RV("")
    val count = RV(0)
    var unmounted = false
    val m = TestKit.mountHtml { implicit cm =>
      ComponentManager.onUnmounted { unmounted = true }
      s"""<input ${StRuntime.model(name)}>
         |<button ${ComponentManager.evClick(count := count.v + 1)}>+</button>
         |<p>${StRuntime.text(name)}</p>
         |${ComponentManager.ifC(count.mapC(_ >= 2))(_ => "<b>2以上</b>")}""".stripMargin
    }
    TestKit.input(m.find("input"), "abc")
    assertEquals(m.find("p").textContent, "abc")
    TestKit.click(m.find("button"))
    TestKit.click(m.find("button"))
    TestKit.flush().map { _ =>
      assertEquals(m.findAll("b").map(_.textContent), List("2以上"))
      m.unmount()
      assert(unmounted)
      assert(!m.root.isConnected)
    }
  }

  test("Devtools: 表示中のコンポーネントの数・描き直しの回数・監視の数") {
    val show = RV(true)
    val before = Devtools.stats.liveComponents
    Devtools.resetStats()
    val m = TestKit.mountHtml { implicit cm =>
      s"<div>${ComponentManager.ifC(show)(_ => "<i>x</i>")}</div>"
    }
    val mounted = Devtools.stats.liveComponents
    assert(mounted > before)
    assertEquals(Devtools.watcherCount(show), 1)
    show := false
    TestKit.flush().flatMap { _ =>
      show := true
      TestKit.flush()
    }.map { _ =>
      assertEquals(Devtools.stats.reloads, 2)
      assertEquals(Devtools.stats.liveComponents, mounted, "切り替えても表示中の数は変わらない")
      m.unmount()
      assertEquals(Devtools.stats.liveComponents, before, "片付けると元に戻る")
      assertEquals(Devtools.watcherCount(show), 0)
    }
  }

  test("Devtools.highlightUpdates: 描き直した部分を枠で囲む") {
    val n = RV(1)
    Devtools.highlightUpdates = true
    val m = TestKit.mountHtml { implicit cm =>
      s"<div>${ComponentManager.ifC(n.mapC(_ > 0))(_ => "<i>x</i>")}</div>"
    }
    n := 0
    n := 2
    TestKit.flush().map { _ =>
      Devtools.highlightUpdates = false
      assert(m.find("i").style.outline.nonEmpty, m.html)
      m.unmount()
    }
  }

}
