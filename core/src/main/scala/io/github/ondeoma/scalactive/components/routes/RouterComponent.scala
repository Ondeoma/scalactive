package io.github.ondeoma.scalactive.components.routes

import io.github.ondeoma.scalactive.ScalactiveConfig
import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager}
import io.github.ondeoma.scalactive.facades.URLPattern
import org.scalajs.dom.*
import org.scalajs.dom.window.location

import scala.scalajs.js

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
      c => window.setTimeout(() => Router.scrollToHash(c.parent), 0)
    )(root, am)
  }

  def apply(routes: PathPatternRoutes,
            base: String = ScalactiveConfig.fixedBasePath): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, routes, base)
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
