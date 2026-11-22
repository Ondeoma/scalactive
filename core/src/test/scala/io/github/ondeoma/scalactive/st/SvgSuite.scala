package io.github.ondeoma.scalactive.st

import io.github.ondeoma.scalactive.ScalactiveConfig
import io.github.ondeoma.scalactive.components.*
import io.github.ondeoma.scalactive.components.ComponentManager.{AddMethod, HTML}
import io.github.ondeoma.scalactive.reactive.*
import org.scalajs.dom.*

import scala.concurrent.Future
import scala.concurrent.duration.*
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
 * `<svg>` の中で、v-for・v-if・子コンポーネントが作る要素がSVGの名前空間になること.
 * (HTML文字列を文脈なしに解析すると、HTMLの名前空間になって表示されない)
 */
class SvgSuite extends munit.FunSuite {

  private val SvgNs = "http://www.w3.org/2000/svg"
  private val HtmlNs = "http://www.w3.org/1999/xhtml"

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

  private def flush(): Future[Unit] = ComponentManager.nextTick()

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

  private def all(root: Element, selector: String): List[Element] = {
    val nl = root.querySelectorAll(selector)
    List.tabulate(nl.length)(nl(_))
  }

  private def assertNs(root: Element, selector: String, ns: String): Unit = {
    val es = all(root, selector)
    assert(es.nonEmpty, s"'$selector' was not found in: ${root.innerHTML}")
    es.foreach(e => assertEquals(e.namespaceURI, ns, s"$selector: ${e.outerHTML}"))
  }

  test("v-forの行(入れ子の要素とテキストも)はSVGの名前空間で作る") {
    val xs = RV(List(1, 2))
    val root = mount { implicit cm =>
      "<svg viewBox=\"0 0 10 10\">" +
        ComponentManager.forEachC(StRuntime.each(xs)) { (__cm, x) =>
          given ComponentManager = __cm
          "<g>" +
            "<circle " + StRuntime.attr("cx", x.toString) + " r=\"1\">" +
            "<title>" + StRuntime.text(s"t$x") + "</title>" +
            "</circle>" +
            "</g>"
        } +
        "</svg>"
    }
    assertNs(root, "svg", SvgNs)
    assertNs(root, "g", SvgNs)
    assertNs(root, "circle", SvgNs)
    assertNs(root, "title", SvgNs)
    assertEquals(all(root, "title").map(_.textContent), List("t1", "t2"))
    assertEquals(all(root, "circle").map(_.getAttribute("cx")), List("1", "2"))
    // 差分で足した行もSVGの名前空間
    xs := List(1, 2, 3)
    flush().map { _ =>
      assertEquals(all(root, "circle").length, 3)
      assertNs(root, "circle", SvgNs)
      assertNs(root, "title", SvgNs)
    }
  }

  test("v-ifの中身は、描き直してもSVGの名前空間") {
    val show = RV(true)
    val root = mount { implicit cm =>
      "<svg><g>" +
        ComponentManager.ifC(StRuntime.cond(show)) { implicit __cm => "<rect width=\"1\" height=\"1\"></rect>" } +
        ComponentManager.ifC(StRuntime.otherwise(show)) { implicit __cm => "<line x1=\"0\" x2=\"1\"></line>" } +
        "</g></svg>"
    }
    assertNs(root, "rect", SvgNs)
    show := false
    flush().flatMap { _ =>
      assertNs(root, "line", SvgNs)
      assertEquals(all(root, "rect").length, 0)
      show := true
      flush()
    }.map { _ =>
      assertNs(root, "rect", SvgNs)
    }
  }

  test("リアクティブな属性・class・styleはSVGの要素にも付く") {
    val x = RV("3")
    val c = RV("on")
    val color = RV("red")
    val root = mount { implicit cm =>
      "<svg>" +
        ComponentManager.forEachC(List(1)) { (__cm, _) =>
          given ComponentManager = __cm
          "<circle " + StRuntime.attr("cx", x) + " " + StRuntime.cls(c) + " " +
            StRuntime.style(color.mapC(v => s"fill: $v")) + "></circle>"
        } +
        "<polyline " + StRuntime.attr("points", x.mapC(v => s"0,0 $v,$v")) + "></polyline>" +
        "</svg>"
    }
    val circle = root.querySelector("circle")
    val polyline = root.querySelector("polyline")
    assertEquals(circle.namespaceURI, SvgNs)
    assertEquals(circle.getAttribute("cx"), "3")
    assert(circle.classList.contains("on"))
    assertEquals(circle.asInstanceOf[HTMLElement].style.getPropertyValue("fill"), "red")
    assertEquals(polyline.getAttribute("points"), "0,0 3,3")
    x := "5"
    c := "off"
    color := "blue"
    flush().map { _ =>
      assertEquals(circle.getAttribute("cx"), "5")
      assert(circle.classList.contains("off"))
      assertEquals(circle.asInstanceOf[HTMLElement].style.getPropertyValue("fill"), "blue")
      assertEquals(polyline.getAttribute("points"), "0,0 5,5")
    }
  }

  private object Dot extends StatelessPropsComponent[Int] {
    def genHtml(p: Int)(implicit cm: ComponentManager): HTML =
      "<g class=\"dot\"><circle " + StRuntime.attr("r", p.toString) + "></circle></g>"
  }

  test("ルートがSVGの要素の子コンポーネントも、SVGの名前空間で作る") {
    val root = mount { implicit cm =>
      "<svg>" + ComponentManager.%((r, am) => Dot(2)(r, am)) + "</svg>"
    }
    assertNs(root, "g.dot", SvgNs)
    assertNs(root, "circle", SvgNs)
    assertEquals(root.querySelector("circle").getAttribute("r"), "2")
  }

  test("foreignObjectの中はHTMLに戻る") {
    val root = mount { implicit cm =>
      "<svg><foreignObject width=\"10\" height=\"10\">" +
        ComponentManager.forEachC(List(1)) { (__cm, _) => "<div class=\"in\"><span>x</span></div>" } +
        "</foreignObject></svg>"
    }
    assertNs(root, "foreignObject", SvgNs)
    assertNs(root, "div.in", HtmlNs)
    assertNs(root, "span", HtmlNs)
  }

  test("SVGの外のv-forはHTMLのまま(同じHTMLの解析結果を取り違えない)") {
    val root = mount { implicit cm =>
      "<svg>" + ComponentManager.forEachC(List(1)) { (__cm, _) => "<a></a>" } + "</svg>" +
        "<div>" + ComponentManager.forEachC(List(1)) { (__cm, _) => "<a></a>" } + "</div>"
    }
    assertEquals(root.querySelector("svg a").namespaceURI, SvgNs)
    assertEquals(root.querySelector(":scope > div a").namespaceURI, HtmlNs)
  }

  test("v-htmlの中身もSVGの名前空間") {
    val body = RV("<circle r=\"1\"></circle>")
    val root = mount { implicit cm => "<svg><g>" + StRuntime.html(body) + "</g></svg>" }
    assertNs(root, "circle", SvgNs)
    body := "<rect></rect>"
    flush().map(_ => assertNs(root, "rect", SvgNs))
  }

  test("xlink:hrefはxlinkの名前空間の属性にする") {
    val href = RV("#a")
    val root = mount { implicit cm =>
      "<svg>" + ComponentManager.forEachC(List(1)) { (__cm, _) =>
        given ComponentManager = __cm
        "<use " + StRuntime.attr("xlink:href", href) + "></use>"
      } + "</svg>"
    }
    val use = root.querySelector("use")
    assertEquals(use.namespaceURI, SvgNs)
    assertEquals(use.getAttributeNS("http://www.w3.org/1999/xlink", "href"), "#a")
    href := "#b"
    flush().map(_ => assertEquals(use.getAttributeNS("http://www.w3.org/1999/xlink", "href"), "#b"))
  }

  test("mountでSVGの要素に描画すると、SVGの名前空間で作る") {
    val svg = document.createElementNS(SvgNs, "svg")
    document.body.appendChild(svg)
    ComponentManager.mount(svg, (r, am) => Dot(1)(r, am)) match {
      case Left(e) => fail(e)
      case Right(_) => assertNs(svg, "circle", SvgNs)
    }
  }

}
