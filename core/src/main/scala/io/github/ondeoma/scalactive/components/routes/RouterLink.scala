package io.github.ondeoma.scalactive.components.routes

import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager, Slot}
import io.github.ondeoma.scalactive.routes.Router
import org.scalajs.dom.*
import org.scalajs.dom.window.location

/**
 * Router.goで遷移するリンク(Vue Routerのrouter-link).
 *
 * `<a href>` を描画し、クリックでRouter.goします. Ctrl/⌘/Shiftを押しながらのクリックや中クリックはブラウザに任せます.
 * 現在の場所に応じてクラスを付けます.
 *  - activeClass: 現在のパスがtoと同じか、その下(例: toが/guideなら/guide/intro)の場合
 *  - exactActiveClass: 現在のパスがtoと同じ場合
 *
 * .stでは `<RouterLink to="/guide">ガイド</RouterLink>`、Scalaでは `linkC("/guide")(_ => "ガイド")` と書きます.
 */
object RouterLink extends BaseComponent {

  private def normalize(path: String): String = {
    if (path.length > 1 && path.endsWith("/")) path.dropRight(1) else path
  }

  /** toのパス(ベースパスを除いたもの) */
  private def targetPath(to: String): String = {
    normalize(Router.RouteLocation(new URL(Router.fixUrl(to), location.href).href).path)
  }

  private[scalactive] def isExactActive(to: String, href: String): Boolean = {
    normalize(Router.RouteLocation(href).path) == targetPath(to)
  }

  private[scalactive] def isActive(to: String, href: String): Boolean = {
    val path = normalize(Router.RouteLocation(href).path)
    val target = targetPath(to)
    // "/"を前方一致にすると、すべてのページで有効になってしまうため完全一致にする
    path == target || (target != "/" && path.startsWith(target + "/"))
  }

  def apply(to: String,
            activeClass: String = "router-link-active",
            exactActiveClass: String = "router-link-exact-active",
            slot: Slot = Slot.empty)
           (implicit parent: HTMLElement,
            am: AddMethod): NodesComponentController = {
    mkNCC({ implicit cm =>
      val active = Router.href.mapC(h => isActive(to, h))
      val exact = Router.href.mapC(h => isExactActive(to, h))
      val href = io.github.ondeoma.scalactive.st.StRuntime.escape(Router.fixUrl(to))
      val onClick: Event => Unit = {
        case e: MouseEvent if !e.defaultPrevented && e.button == 0 && !e.ctrlKey && !e.metaKey && !e.shiftKey && !e.altKey =>
          e.preventDefault()
          Router.go(to)
        case _ => ()
      }
      s"""<a href="$href" ${clsIf(active, activeClass)} ${clsIf(exact, exactActiveClass)} ${ev(EventType.click, onClick)}>${slot()}</a>"""
    })
  }

}
