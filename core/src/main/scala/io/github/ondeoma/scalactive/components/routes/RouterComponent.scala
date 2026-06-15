package io.github.ondeoma.scalactive.components.routes

import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager}
import io.github.ondeoma.scalactive.facades.URLPattern
import org.scalajs.dom.*
import org.scalajs.dom.window.location

object RouterComponent extends BaseComponent {

  def apply(root: HTMLElement,
            am: AddMethod,
            routes: PathPatternRoutes,
            base: String): NodesComponentController = {
    val ups = toURLPatterns(routes, base)
    mkNCC(genElement(ups), c => List(
      Router.href.addWatcher { _ =>
        c.reload()
        c.parent.foreach { p =>
          p.scrollTop = 0
          p.scrollLeft = 0
        }
      }),
      _ => window.setTimeout(() => Router.scrollToHash(), 0)
    )(root, am)
  }

  def apply(routes: PathPatternRoutes,
            base: String): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, routes, base)
  }

  def toURLPatterns(routes: PathPatternRoutes,
                    base: String): URLPatternRoutes = {
    val fixedBase = {
      val slashed = base.headOption match {
        case None => ""
        case Some('/') => base
        case _ => s"/${base}"
      }
      s"${location.origin}${slashed}"
    }
    routes.map(r => URLPattern(r._1, fixedBase) -> r._2)
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
