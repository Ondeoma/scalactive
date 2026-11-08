package io.github.ondeoma.scalactive.components.fors

import io.github.ondeoma.scalactive.ScalactiveConfig
import io.github.ondeoma.scalactive.components.*
import io.github.ondeoma.scalactive.components.ComponentManager.{AddMethod, HTML}
import io.github.ondeoma.scalactive.reactive.*
import org.scalajs.dom.*

import scala.concurrent.Future
import scala.concurrent.duration.*
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

/**
 * forCの差分更新のテスト. 変わらない行はDOMノードごと使い回され、表示は常にリストと一致する必要があります.
 */
class ForDiffSuite extends munit.FunSuite {

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

  /** 予約された描き直しが終わるまで待つ */
  private def flush(): Future[Unit] = {
    js.Promise.resolve[Unit](()).toFuture.flatMap(_ => js.Promise.resolve[Unit](()).toFuture)
  }

  private def lis(root: HTMLElement): List[Element] = {
    val nl = root.querySelectorAll("li")
    List.tabulate(nl.length)(nl(_))
  }

  private def texts(root: HTMLElement): List[String] = lis(root).map(_.textContent)

  private def countNodes(n: Node): Int = {
    var count = 1
    var c = n.firstChild
    while (c != null) {
      count += countNodes(c)
      c = c.nextSibling
    }
    count
  }

  test("末尾への追加では既存の行を使い回す") {
    val list = RV(List(1, 2, 3))
    val root = mount(implicit cm => s"<ul>${ComponentManager.forC(list)((_, v, i) => s"<li>$i:$v</li>")}</ul>")
    val before = lis(root)
    list := List(1, 2, 3, 4)
    flush().map { _ =>
      assertEquals(texts(root), List("0:1", "1:2", "2:3", "3:4"))
      assert(lis(root).take(3).zip(before).forall(_ eq _), "既存の3行は同じノード")
    }
  }

  test("添字を使う行は、位置が変わったら作り直す") {
    val list = RV(List(1, 2, 3))
    val root = mount(implicit cm => s"<ul>${ComponentManager.forC(list)((_, v, i) => s"<li>$i:$v</li>")}</ul>")
    val before = lis(root)
    list := List(3, 2, 1)
    flush().map { _ =>
      assertEquals(texts(root), List("0:3", "1:2", "2:1"))
      val after = lis(root)
      assert(after(1) eq before(1), "位置が変わらない行は使い回す")
      assert(!(after(0) eq before(2)), "位置が変わった行は作り直す")
    }
  }

  test("RVList: 途中の行を消すと、前の行は使い回し、後ろの行(添字が変わる)は作り直す") {
    val list = RVList(List("a", "b", "c", "d"))
    val root = mount(implicit cm => s"<ul>${ComponentManager.forC(list)((_, rv, i) => s"<li>$i:${rv.v}</li>")}</ul>")
    val before = lis(root)
    list.rm(1)
    flush().map { _ =>
      assertEquals(texts(root), List("0:a", "1:c", "2:d"))
      assert(lis(root).head eq before.head)
    }
  }

  test("RVList: 代入で値が入れ替わった行だけを作り直す") {
    val list = RVList(List("a", "b", "c", "d"))
    val root = mount(implicit cm => s"<ul>${ComponentManager.forC(list)((_, rv, _) => s"<li>${rv.v}</li>")}</ul>")
    val before = lis(root)
    list := List("a", "d", "c", "b")
    flush().map { _ =>
      assertEquals(texts(root), List("a", "d", "c", "b"))
      val after = lis(root)
      assert(after(0) eq before(0))
      assert(after(2) eq before(2))
      assert(!(after(1) eq before(1)))
      assert(!(after(3) eq before(3)))
    }
  }

  test("同じ値が複数あっても正しく並べる") {
    val list = RV(List("a", "a", "b"))
    val root = mount(implicit cm => s"<ul>${ComponentManager.forC(list)((_, v, _) => s"<li>$v</li>")}</ul>")
    list := List("b", "a", "a", "a")
    flush().flatMap { _ =>
      assertEquals(texts(root), List("b", "a", "a", "a"))
      list := List("a")
      flush()
    }.map(_ => assertEquals(texts(root), List("a")))
  }

  test("消えた行の一時リアクティブ値(mapCなど)の監視を解除する") {
    val selected = new Counted(0)
    val list = RV(List(1, 2, 3, 4, 5))
    val root = mount { implicit cm =>
      val items = ComponentManager.forC(list) { (rowCm, v, _) =>
        given ComponentManager = rowCm
        s"<li ${ComponentManager.clsIf(selected.mapC(_ == v), "on")}>$v</li>"
      }
      s"<ul>$items</ul>"
    }
    assertEquals(selected.watcherCount, 5)
    list := List(1, 3)
    flush().flatMap { _ =>
      assertEquals(selected.watcherCount, 2)
      selected := 3
      assertEquals(lis(root).map(_.classList.contains("on")), List(false, true))
      list := Nil
      flush()
    }.map(_ => assertEquals(selected.watcherCount, 0))
  }

  test("空にして戻してもDOMノードが増えない") {
    val list = RV(List(1, 2))
    val root = mount(implicit cm => s"<ul>${ComponentManager.forC(list)((_, v, _) => s"<li>$v</li>")}</ul>")
    val initial = countNodes(root)
    list := Nil
    flush().flatMap { _ =>
      assertEquals(texts(root), Nil)
      list := List(1, 2)
      flush()
    }.map { _ =>
      assertEquals(texts(root), List("1", "2"))
      assertEquals(countNodes(root), initial)
    }
  }

  test("ランダムな変更を続けても、表示はリストと一致する") {
    val random = new scala.util.Random(7)
    val list = RV(List.empty[Int])
    val root = mount(implicit cm => s"<ul>${ComponentManager.forC(list)((_, v, i) => s"<li>$i:$v</li>")}</ul>")
    val steps = (1 to 40).map { _ =>
      val cur = list.v
      random.nextInt(5) match {
        case 0 => cur :+ random.nextInt(10)
        case 1 if cur.nonEmpty => cur.patch(random.nextInt(cur.size), Nil, 1)
        case 2 => random.shuffle(cur)
        case 3 => List.fill(random.nextInt(6))(random.nextInt(4))
        case _ => random.nextInt(10) :: cur
      }
    }
    steps.foldLeft(Future.successful(())) { (f, next) =>
      f.flatMap { _ =>
        list := next
        flush().map { _ =>
          assertEquals(texts(root), next.zipWithIndex.map((v, i) => s"$i:$v"))
          assertEquals(root.querySelector("ul").childNodes.length, next.size + 2, "行 + 目印(コメント) + 親のプレースホルダー")
        }
      }
    }
  }

  test("longestIncreasing: 値が増加する最長の部分列(負の値は除く)") {
    def run(xs: Int*): List[Int] = {
      val r = ForGeneralComponent.longestIncreasing(xs.toArray)
      xs.indices.filter(r(_)).map(xs(_)).toList
    }
    assertEquals(run(0, 1, 2, 3), List(0, 1, 2, 3))
    assertEquals(run(3, 2, 1, 0).size, 1)
    assertEquals(run(0, 3, 2, 1, 4).size, 3)
    assertEquals(run(-1, 0, -1, 1), List(0, 1))
    assertEquals(run(), Nil)
  }

}
