package io.github.ondeoma.scalactive.routes

import io.github.ondeoma.scalactive.ScalactiveConfig
import io.github.ondeoma.scalactive.components.*
import io.github.ondeoma.scalactive.components.ComponentManager.{AddMethod, HTML}
import io.github.ondeoma.scalactive.components.routes.RouterComponent
import io.github.ondeoma.scalactive.routes.Router.*
import io.github.ondeoma.scalactive.utils.TypeAlias.*
import org.scalajs.dom.*

import scala.concurrent.Future
import scala.concurrent.duration.*
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

/**
 * Routerのナビゲーションガードと、RouterComponentのネストのテスト.
 * jsdomにはURLPatternがないため、パス部分だけを扱う簡易的なものを用意します.
 */
class RouterSuite extends munit.FunSuite {

  private var saved: (FiniteDuration, Boolean, Boolean, String) = null

  override def beforeAll(): Unit = {
    saved = (ScalactiveConfig.reloadTimerLazyDuration, ScalactiveConfig.keepWindowScrollPositionWhenReload,
      ScalactiveConfig.scrollToTopOnNavigate, ScalactiveConfig.basePath)
    ScalactiveConfig.reloadTimerLazyDuration = 0.millis
    // jsdomはwindow.scrollToを実装していない
    ScalactiveConfig.keepWindowScrollPositionWhenReload = false
    ScalactiveConfig.scrollToTopOnNavigate = false
    ScalactiveConfig.basePath = ""
    if (js.typeOf(js.Dynamic.global.URLPattern) == "undefined") {
      js.eval(
        """window.URLPattern = class {
          |  constructor(pattern, base) {
          |    const full = new URL(pattern, base || location.origin + "/").pathname;
          |    const esc = (s) => s.replace(/[.+?^${}()|[\]\\]/g, "\\$&");
          |    let src = "", last = 0, idx = 0;
          |    this.names = [];
          |    full.replace(/:(\w+)(\*)?|\*/g, (m, n, star, off) => {
          |      src += esc(full.slice(last, off));
          |      if (n) { this.names.push(n); src += star ? "(.*)" : "([^/]+)"; }
          |      else { this.names.push(String(idx++)); src += "(.*)"; }
          |      last = off + m.length;
          |      return m;
          |    });
          |    src += esc(full.slice(last));
          |    this.re = new RegExp("^" + src + "$");
          |  }
          |  test(input) { return this.re.test(new URL(input).pathname); }
          |  exec(input) {
          |    const p = new URL(input).pathname, m = this.re.exec(p);
          |    if (!m) return null;
          |    const groups = {};
          |    this.names.forEach((n, k) => groups[n] = m[k + 1]);
          |    return { pathname: { input: p, groups } };
          |  }
          |};""".stripMargin)
    }
  }

  override def afterAll(): Unit = {
    ScalactiveConfig.reloadTimerLazyDuration = saved._1
    ScalactiveConfig.keepWindowScrollPositionWhenReload = saved._2
    ScalactiveConfig.scrollToTopOnNavigate = saved._3
    ScalactiveConfig.basePath = saved._4
  }

  private def flush(): Future[Unit] = {
    js.Promise.resolve[Unit](()).toFuture.flatMap(_ => js.Promise.resolve[Unit](()).toFuture)
  }

  private def path: String = window.location.pathname

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

  // ページを開いた直後の確認は1度だけなので、最初に実行する
  test("ページを開いた直後の場所もガードで確認し、リダイレクトできる") {
    window.history.replaceState(js.Object(), "", "/secret")
    var seen = List.empty[(String, Option[String])]
    val remove = Router.beforeEach { (to, from) =>
      seen :+= (to.path, from.map(_.path))
      if (to.path == "/secret") Redirect("/login") else Continue
    }
    flush().map { _ =>
      remove()
      assertEquals(path, "/login")
      assertEquals(seen, List(("/secret", None), ("/login", Some("/secret"))))
    }
  }

  test("beforeEach: Cancelで遷移を取り消し、Redirectで別のパスへ遷移する. afterEachは遷移の後に呼ばれる") {
    window.history.replaceState(js.Object(), "", "/home")
    Router.href := window.location.href
    var after = List.empty[String]
    val removeGuard = Router.beforeEach { (to, _) =>
      to.path match {
        case "/blocked" => Cancel
        case "/old" => Redirect("/new")
        case _ => Continue
      }
    }
    val removeAfter = Router.afterEach((to, from) => after :+= s"${from.map(_.path).getOrElse("-")}->${to.path}")
    try {
      Router.go("/blocked")
      assertEquals(path, "/home")
      Router.go("/old")
      assertEquals(path, "/new")
      assertEquals(Router.href.v, window.location.href)
      Router.go("/a?x=1")
      assertEquals(path, "/a")
      assertEquals(after, List("/home->/new", "/new->/a"))
    } finally {
      removeGuard()
      removeAfter()
    }
    Router.go("/blocked")
    assertEquals(path, "/blocked", "解除したガードは呼ばれない")
  }

  test("beforeEach: 戻る・進む(popstate)もガードで確認し、Cancelなら元のURLに戻す") {
    window.history.replaceState(js.Object(), "", "/page1")
    Router.href := window.location.href
    val remove = Router.beforeEach((to, _) => if (to.path == "/page0") Cancel else Continue)
    try {
      // 戻るボタンでURLが変わった状態を再現する
      window.history.replaceState(js.Object(), "", "/page0")
      window.dispatchEvent(new Event("popstate"))
      assertEquals(path, "/page1")
      window.history.replaceState(js.Object(), "", "/page2")
      window.dispatchEvent(new Event("popstate"))
      assertEquals(Router.href.v, window.location.href)
      assertEquals(path, "/page2")
    } finally remove()
  }

  test("リダイレクトがループしても止まる") {
    window.history.replaceState(js.Object(), "", "/start")
    val remove = Router.beforeEach((to, _) => if (to.path == "/x") Redirect("/y") else if (to.path == "/y") Redirect("/x") else Continue)
    try {
      Router.go("/x")
      assertEquals(path, "/start")
    } finally remove()
  }

  test("ネストしたルート: 子のパスだけが変わったときは外側(レイアウト)を作り直さない") {
    window.history.replaceState(js.Object(), "", "/guide/a")
    Router.href := window.location.href
    var layoutRenders = 0
    val layout: GenComponentF = new StatelessComponent {
      def genHtml(implicit cm: ComponentManager): HTML = {
        layoutRenders += 1
        val inner = RouterComponent.nested("/guide")(List(
          ("a", _ => new StatelessComponent { def genHtml(implicit cm: ComponentManager): HTML = "<i>A</i>" }.apply()(_, _)),
          ("b", _ => new StatelessComponent { def genHtml(implicit cm: ComponentManager): HTML = "<i>B</i>" }.apply()(_, _)),
        ))
        s"""<section>layout ${%(inner)}</section>"""
      }
    }.apply()(_, _)
    val other: GenComponentF = new StatelessComponent {
      def genHtml(implicit cm: ComponentManager): HTML = "<p>other</p>"
    }.apply()(_, _)
    val routes: PathPatternRoutes = List(("/guide/*", _ => layout), ("/other", _ => other))
    val root = mount(implicit cm => s"<main>${ComponentManager.%(RouterComponent(routes))}</main>")
    assertEquals(root.querySelector("main").textContent.trim, "layout A")
    Router.go("/guide/b")
    flush().flatMap { _ =>
      assertEquals(root.querySelector("main").textContent.trim, "layout B")
      assertEquals(layoutRenders, 1)
      Router.go("/other")
      flush()
    }.map { _ =>
      assertEquals(root.querySelector("main").textContent.trim, "other")
    }
  }

  test("名前付きのパスパラメータが変わったときは描き直す") {
    window.history.replaceState(js.Object(), "", "/users/1")
    Router.href := window.location.href
    val root = mount { implicit cm =>
      val routes: PathPatternRoutes = List(("/users/:id", r => {
        val id = r.pathname.groups("id")
        new StatelessComponent { def genHtml(implicit cm: ComponentManager): HTML = s"<b>user $id</b>" }.apply()(_, _)
      }))
      s"<div>${ComponentManager.%(RouterComponent(routes))}</div>"
    }
    assertEquals(root.textContent, "user 1")
    Router.go("/users/2")
    flush().map(_ => assertEquals(root.textContent, "user 2"))
  }

  test("RouteLocation: ベースパスを除いたパス") {
    ScalactiveConfig.basePath = "app"
    try {
      assertEquals(RouteLocation("http://localhost/app/users/1?q=1#h").path, "/users/1")
      assertEquals(RouteLocation("http://localhost/app").path, "/")
      assertEquals(RouteLocation("http://localhost/other").path, "/other")
    } finally ScalactiveConfig.basePath = ""
  }

}
