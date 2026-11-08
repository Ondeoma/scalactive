package io.github.ondeoma.scalactive.components.routes

import io.github.ondeoma.scalactive.ScalactiveConfig
import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager}
import io.github.ondeoma.scalactive.facades.{URLPattern, URLPatternResult}
import org.scalajs.dom.*
import org.scalajs.dom.window.location

import io.github.ondeoma.scalactive.routes.Router
import io.github.ondeoma.scalactive.utils.TypeAlias.GenComponentF

import scala.scalajs.js

/**
 * ルートの定義.
 *
 * @param beforeEnter このルートに(別のルートから)入るときに呼ばれるガード(既定ではそのまま遷移)
 * @param meta        ガードの中で to.meta として参照できる情報(例: Map("requiresAuth" -> true))
 */
final case class Route(pattern: String,
                       component: URLPatternResult => GenComponentF,
                       beforeEnter: Router.Guard = (_, _) => Router.Continue,
                       meta: Map[String, Any] = Map.empty)

object RouterComponent extends BaseComponent {

  /**
   * 描き直すかどうかを決めるための、マッチしたルートの情報.
   * ルート・名前付きのパスパラメータ・クエリ文字列のどれかが変わったときだけ描き直します.
   * 名前のないワイルドカード(*)の部分だけが変わった場合は描き直さないので、
   * ルートのコンポーネントの中に置いたRouterComponent(ネストしたルート)だけが切り替わります.
   */
  private final case class MatchKey(route: Int, params: Map[String, String], search: String)

  private def matchKey(routes: List[(URLPattern, Route)], href: String): Option[MatchKey] = {
    routes.iterator.zipWithIndex.collectFirst {
      case ((p, _), i) if p.test(href) =>
        val groups = p.exec(href).toOption.map(_.pathname.groups.toMap).getOrElse(Map.empty)
        MatchKey(i, groups.filterNot(_._1.forall(_.isDigit)), new URL(href).search)
    }
  }

  def apply(root: HTMLElement,
            am: AddMethod,
            routes: PathPatternRoutes,
            base: String): NodesComponentController = {
    withRoutes(root, am, routes.map((p, f) => Route(p, f)), base)
  }

  /** Route(ルートごとのガード・meta付き)で定義する */
  def withRoutes(root: HTMLElement,
                 am: AddMethod,
                 routes: List[Route],
                 base: String): NodesComponentController = {
    val ups = toURLPatternRoutes(routes, base)
    var current: Option[MatchKey] = None
    var unregister: () => Unit = () => ()
    val controller = mkNCC({ cm =>
      current = matchKey(ups, location.href)
      unregister()
      unregister = Router.registerRoutes(ups.map((p, r) => Router.RouteEntry(p, r.meta, r.beforeEnter)))
      genElement(ups)(using cm)
    }, c => List(
      Router.href.addWatcher { _ =>
        if (matchKey(ups, location.href) != current) {
          c.reload()
          c.parent.foreach { p =>
            p.scrollTop = 0
            p.scrollLeft = 0
          }
        }
      }),
      c => window.setTimeout(() => Router.scrollToHash(c.parent), 0)
    )(root, am)
    controller.onClear = Some(_ => {
      unregister()
      unregister = () => ()
    })
    controller
  }

  def apply(routes: PathPatternRoutes,
            base: String = ScalactiveConfig.fixedBasePath): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, routes, base)
  }

  /**
   * Route(ルートごとのガード・meta付き)で定義する.
   * (タプルのリストを受け取るapplyと名前を分けているのは、オーバーロードにすると関数の型が推論できなくなるため)
   */
  def of(routes: List[Route],
         base: String = ScalactiveConfig.fixedBasePath): (HTMLElement, AddMethod) => NodesComponentController = {
    withRoutes(_, _, routes, base)
  }

  /**
   * ネストしたルート. ルートのコンポーネント(レイアウト)の中に置き、prefixより下のパスでコンポーネントを選びます.
   * 外側のルートは "/guide/\*" のように、ワイルドカードでprefix以下のパスにマッチさせてください.
   * 例: `RouterComponent.nested("/guide")(List(("intro", _ => Intro()), ("api", _ => Api())))`
   */
  def nested(prefix: String)
            (routes: PathPatternRoutes): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(routes, Router.joinSlash(ScalactiveConfig.fixedBasePath, prefix))
  }

  /** Route(ルートごとのガード・meta付き)で定義するネストしたルート */
  def nestedOf(prefix: String)
              (routes: List[Route]): (HTMLElement, AddMethod) => NodesComponentController = {
    withRoutes(_, _, routes, Router.joinSlash(ScalactiveConfig.fixedBasePath, prefix))
  }

  def toURLPatterns(routes: PathPatternRoutes,
                    base: String): URLPatternRoutes = {
    toURLPatternRoutes(routes.map((p, f) => Route(p, f)), base).map((u, r) => u -> r.component)
  }

  private def toURLPatternRoutes(routes: List[Route],
                                 base: String): List[(URLPattern, Route)] = {
    val origin = location.origin
    val (fixedBase, hasBaseSubPath) = {
      if (base.isEmpty) (origin, false)
      else {
        val path =
          if (base.startsWith("http")) s"${base.replaceAll("/$", "")}/"
          else s"$origin/${base.replaceAll("^/", "").replaceAll("/$", "")}/"
        (path, path != s"$origin/")
      }
    }
    routes.map(r => {
      // サブパスが指定されている場合パターンは相対パスとして認識したいはずなので、
      // 先頭にスラッシュがある場合は取り除く.
      val fixedPattern =
        if (hasBaseSubPath && r.pattern.headOption.contains('/')) r.pattern.tail
        else r.pattern
      URLPattern(fixedPattern, fixedBase) -> r
    })
  }

  private def genElement(routes: List[(URLPattern, Route)])
                        (implicit cm: ComponentManager) = {
    val href = location.href
    routes.find(r => r._1.test(href)).fold("") { (p, r) =>
      val cmp = r.component(p.exec(href).get)
      c(cmp)
    }
  }

}
