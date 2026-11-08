package io.github.ondeoma.scalactive.controllers

import io.github.ondeoma.scalactive.ScalactiveConfig
import io.github.ondeoma.scalactive.components.*
import io.github.ondeoma.scalactive.components.ComponentManager.{AddMethod, HTML}
import io.github.ondeoma.scalactive.components.texts.TextComponent
import io.github.ondeoma.scalactive.reactive.*
import org.scalajs.dom.*

import scala.concurrent.duration.*
import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
 * コンポーネントを片付けた(clear/reload)後に、DOMノードや監視が残らないことのテスト.
 */
class ClearSuite extends munit.FunSuite {

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

  private def mount(gen: ComponentManager => HTML): (HTMLElement, NodesComponentController) = {
    val root = document.createElement("div").asInstanceOf[HTMLElement]
    document.body.appendChild(root)
    val component = new StatelessComponent {
      def genHtml(implicit cm: ComponentManager): HTML = gen(cm)
    }
    given HTMLElement = root
    given AddMethod = AddMethod.append(root)
    component().init() match {
      case Left(e) => fail(e)
      case Right(c) => (root, c)
    }
  }

  /** 予約されたreloadが終わるのを待つ */
  private def settle(): Future[Unit] = {
    val p = Promise[Unit]()
    window.setTimeout(() => p.success(()), 5)
    p.future
  }

  private def countNodes(n: Node): Int = {
    var count = 1
    var c = n.firstChild
    while (c != null) {
      count += countNodes(c)
      c = c.nextSibling
    }
    count
  }

  /** fを1回ずつ実行してreloadを待つ */
  private def repeat(times: Int)(f: Int => Unit): Future[Unit] = {
    (1 to times).foldLeft(Future.successful(())) { (acc, i) =>
      acc.flatMap { _ =>
        f(i)
        settle()
      }
    }
  }

  test("ifC: 切り替えを繰り返してもDOMノードが増えない") {
    val b = RV(true)
    val (root, _) = mount { implicit cm =>
      s"""<div>${ComponentManager.ifC(b)(_ => "<p>shown</p>")}</div>"""
    }
    val initial = countNodes(root)
    repeat(10)(i => b := i % 2 == 0).map { _ =>
      assertEquals(countNodes(root), initial)
      assertEquals(root.querySelectorAll("p").length, 1)
    }
  }

  test("forC: 空にして戻すのを繰り返してもDOMノードが増えない") {
    val list = RVList(List(1, 2, 3))
    val (root, _) = mount { implicit cm =>
      val items = ComponentManager.forC(list) { (_, rv, _) => s"<li>${rv.v}</li>" }
      s"""<ul>$items</ul>"""
    }
    val initial = countNodes(root)
    repeat(10)(i => list := (if (i % 2 == 1) Nil else List(1, 2, 3))).map { _ =>
      assertEquals(countNodes(root), initial)
      assertEquals(root.querySelectorAll("li").length, 3)
    }
  }

  test("ifCの直下のテキスト(TextComponent)も取り除く") {
    val b = RV(true)
    val n = RV(1)
    val (root, _) = mount { implicit cm =>
      s"""<span>${ComponentManager.ifC(b) { implicit cm => ComponentManager.%(TextComponent(n)(_.toString)) }}</span>"""
    }
    val span = root.querySelector("span")
    assertEquals(span.textContent, "1")
    val initial = span.childNodes.length
    repeat(6)(i => b := i % 2 == 0).map { _ =>
      assertEquals(span.childNodes.length, initial)
      assertEquals(span.textContent, "1")
    }
  }

  test("親を片付けた後に、子の予約済みのreloadで監視が残らない") {
    val b = new Counted(true)
    val (root, parent) = mount { implicit cm =>
      s"""<div>${ComponentManager.ifC(b)(_ => "<p>x</p>")}</div>"""
    }
    assertEquals(b.watcherCount, 1)
    b := false // 子(IfComponent)のreloadが予約される
    parent.clear()
    settle().map { _ =>
      assertEquals(b.watcherCount, 0)
      assertEquals(root.childNodes.length, 0)
    }
  }

}
