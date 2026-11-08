package io.github.ondeoma.scalactive.routes

import io.github.ondeoma.scalactive.ScalactiveConfig
import io.github.ondeoma.scalactive.reactive.RV
import io.github.ondeoma.scalactive.utils.ConsoleUtil
import org.scalajs.dom.window.{history, location}
import org.scalajs.dom.{HTMLElement, document, window}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationInt
import scala.scalajs.js
import scala.util.Try

object Router {

  /**
   * 遷移先・遷移元の場所.
   *
   * @param path ベースパス(ScalactiveConfig.basePath)を除いたパス. 例: "/users/1"
   */
  final case class RouteLocation(href: String,
                                 path: String,
                                 search: String,
                                 hash: String)

  object RouteLocation {

    def apply(href: String): RouteLocation = {
      val url = new org.scalajs.dom.URL(href)
      RouteLocation(href, stripBase(url.pathname), url.search, url.hash)
    }

    private def stripBase(pathname: String): String = {
      val base = ScalactiveConfig.fixedBasePath
      val basePath =
        if (base.startsWith("http")) new org.scalajs.dom.URL(base).pathname
        else base
      val b = basePath.stripSuffix("/")
      val p =
        if (b.nonEmpty && (pathname == b || pathname.startsWith(b + "/"))) pathname.drop(b.length)
        else pathname
      if (p.startsWith("/")) p else "/" + p
    }

  }

  /** ナビゲーションガードの結果 */
  sealed trait NavigationResult

  /** そのまま遷移する */
  case object Continue extends NavigationResult

  /** 遷移を取り消す */
  case object Cancel extends NavigationResult

  /** 別のパスへ遷移する(Router.goと同じくベースパスが考慮されます) */
  final case class Redirect(url: String) extends NavigationResult

  /** 遷移の前に呼ばれるガード. fromはページを開いた直後の最初の確認ではNone */
  type Guard = (RouteLocation, Option[RouteLocation]) => NavigationResult

  /** 遷移の後に呼ばれるフック */
  type AfterHook = (RouteLocation, Option[RouteLocation]) => Unit

  private val guards = ListBuffer[Guard]()
  private val afterHooks = ListBuffer[AfterHook]()
  private var initialChecked = false

  /** リダイレクトの連鎖の上限(ガード同士のリダイレクトのループを防ぐ) */
  private val maxRedirects = 10

  val href = RV(location.href)

  window.addEventListener("popstate", _ => onPopState())

  /**
   * 遷移の前に呼ばれるガードを登録します(Vue RouterのbeforeEach).
   * Router.go、ブラウザの戻る・進む、ページを開いた直後の最初の表示で呼ばれます.
   * 結果がContinueならそのまま遷移、Cancelなら取り消し、Redirect(url)なら別のパスへ遷移します.
   * 戻り値の関数を呼ぶと登録を解除します.
   */
  def beforeEach(guard: Guard): () => Unit = {
    guards += guard
    scheduleInitialCheck()
    () => guards -= guard
  }

  /** 遷移の後に呼ばれるフックを登録します. 戻り値の関数を呼ぶと登録を解除します. */
  def afterEach(hook: AfterHook): () => Unit = {
    afterHooks += hook
    () => afterHooks -= hook
  }

  private def runGuards(to: RouteLocation, from: Option[RouteLocation]): NavigationResult = {
    guards.toList.iterator.map(_(to, from)).find(_ != Continue).getOrElse(Continue)
  }

  private def runAfterHooks(to: RouteLocation, from: Option[RouteLocation]): Unit = {
    afterHooks.toList.foreach(_(to, from))
  }

  /** ページを開いた直後の場所もガードで確認する(すべてのガードが登録されるのを待つため、現在の処理の後に1度だけ) */
  private def scheduleInitialCheck(): Unit = {
    if (!initialChecked) {
      initialChecked = true
      js.Dynamic.global.queueMicrotask(() => {
        runGuards(RouteLocation(location.href), None) match {
          case Redirect(url) => navigate(url, replace = true, redirects = 1)
          case _ => ()
        }
      })
    }
  }

  private def onPopState(): Unit = {
    val from = RouteLocation(href.v)
    val to = RouteLocation(location.href)
    runGuards(to, Some(from)) match {
      case Continue =>
        href := location.href
        runAfterHooks(to, Some(from))
      case Cancel =>
        // 戻る・進むで変わったURLを元に戻す
        history.pushState(js.Object.apply(), "", from.href)
      case Redirect(url) =>
        navigate(url, replace = true, redirects = 1)
    }
  }

  def go(url: String): Try[Unit] = {
    Try(navigate(url, replace = false, redirects = 0))
  }

  private def navigate(url: String,
                       replace: Boolean,
                       redirects: Int): Unit = {
    val now = location.href
    val fixedUrl = fixUrl(url)
    val to = RouteLocation(new org.scalajs.dom.URL(fixedUrl, now).href)
    val from = Some(RouteLocation(now))
    runGuards(to, from) match {
      case Continue =>
        if (ScalactiveConfig.scrollToTopOnNavigate) window.scrollTo(0, 0)
        if (replace) history.replaceState(js.Object.apply(), "", fixedUrl)
        else history.pushState(js.Object.apply(), "", fixedUrl)
        startCheckHref(now)
        runAfterHooks(to, from)
      case Cancel => ()
      case Redirect(next) =>
        if (redirects >= maxRedirects) ConsoleUtil.error(s"Router: too many redirects (last: $next)")
        else navigate(next, replace, redirects + 1)
    }
  }

  def fixUrl(url: String): String = {
    lazy val relPath = !url.startsWith("http")
    lazy val isSameOrigin = url.startsWith(location.origin)
    lazy val base = ScalactiveConfig.fixedBasePath
    if (relPath) joinSlash(base, url)
    else if (isSameOrigin) joinSlash(base, url.replace(location.origin, ""))
    else url
  }

  def joinSlash(a: String,
                b: String): String = {
    if (a.endsWith("/") && b.startsWith("/")) s"$a${b.tail}"
    else if (!a.endsWith("/") && !b.startsWith("/")) s"$a/$b"
    else s"$a$b"
  }

  def setHash(hash: String): Try[Unit] = {
    Try {
      val to = location.origin + location.pathname + location.search + "#" + hash
      history.replaceState(js.Object.apply(), "", to)
      scrollToHash(None)
    }
  }

  def scrollToHash(target: Option[HTMLElement]): Unit = {
    val hash = location.hash
    if (hash.nonEmpty && hash.startsWith("#")) {
      val id = js.Dynamic.global.decodeURIComponent(hash.drop(1)).asInstanceOf[String]
      Option(document.getElementById(id)).foreach(_.scrollIntoView())
    } else {
      target.foreach { e =>
        e.scrollTop = 0
        e.scrollLeft = 0
      }
    }
  }

  /**
   * pushStateの後にhref値を書き換えます.
   * 通常はpushStateの直後にlocation.hrefが変わっているので、すぐに反映します(以前は最初の確認まで50ms待っていました).
   * 変わっていない場合に備えて、しばらく定期的に確認します.
   */
  def startCheckHref(old: String): Unit = {

    val maxCount = 10
    val interval = 50.milliseconds

    if (old != location.href) {
      href := location.href
      return
    }

    def check(count: Int = 1): Unit = {
      window.setTimeout(() => {
        if (old != location.href) {
          href := location.href
        } else if (count < maxCount) {
          check(count + 1)
        }
      }, interval.toMillis.toDouble)
    }

    check()
  }

}
