package io.github.ondeoma.scalactive.components.routes

import io.github.ondeoma.scalactive.ScalactiveConfig
import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager}
import io.github.ondeoma.scalactive.facades.URLPattern
import org.scalajs.dom.*
import org.scalajs.dom.window.location

import scala.scalajs.js

object RouterComponent extends BaseComponent {

  /**
   * 描き直すかどうかを決めるための、マッチしたルートの情報.
   * ルート・名前付きのパスパラメータ・クエリ文字列のどれかが変わったときだけ描き直します.
   * 名前のないワイルドカード(*)の部分だけが変わった場合は描き直さないので、
   * ルートのコンポーネントの中に置いたRouterComponent(ネストしたルート)だけが切り替わります.
   */
  private final case class MatchKey(route: Int, params: Map[String, String], search: String)

  private def matchKey(routes: URLPatternRoutes, href: String): Option[MatchKey] = {
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
    val ups = toURLPatterns(routes, base)
    var current: Option[MatchKey] = None
    mkNCC({ cm =>
      current = matchKey(ups, location.href)
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
  }

  def apply(routes: PathPatternRoutes,
            base: String = ScalactiveConfig.fixedBasePath): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, routes, base)
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

  def toURLPatterns(routes: PathPatternRoutes,
                    base: String): URLPatternRoutes = {
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
        if (hasBaseSubPath && r._1.headOption.contains('/')) r._1.tail
        else r._1
      URLPattern(fixedPattern, fixedBase) -> r._2
    })
  }

  private def genElement(routes: URLPatternRoutes)
                        (implicit cm: ComponentManager) = {
    val href = location.href
    routes.find(r => r._1.test(href)).fold("") { (p, f) =>
      val cmp = f(p.exec(href).get)
      c(cmp)
    }
  }

}
