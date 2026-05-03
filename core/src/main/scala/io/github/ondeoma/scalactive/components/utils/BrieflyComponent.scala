package io.github.ondeoma.scalactive.components.utils

import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager}
import org.scalajs.dom.*
import io.github.ondeoma.scalactive.syntax.All.*
import io.github.ondeoma.scalactive.controllers.HtmlElementsComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.Reactive

import scala.concurrent.duration.FiniteDuration

object BrieflyComponent extends BaseComponent {

  def apply(root: HTMLElement,
            am: AddMethod,
            openV: Reactive[Boolean],
            duration: FiniteDuration)
           (genHtml: ComponentManager => HTML): HtmlElementsComponentController = {
    HtmlElementsComponentController { c =>
      val watcher = openV.addWatcher { _ => c.reload() }
      c.watchInfos = List(watcher)
      if (openV.v) {
        for {
          // Scala 3.4.0~
          // (eles, children, tmpRs, eIds) <- ComponentManager(genHtml)
          t4 <- ComponentManager(genHtml)
          (ns, children, tmpRs, eIds) = t4
          _ <- addNodes(root)(am, ns *).toRight(addNodesErrorMessage)
        } yield {
          c.elements = ns.toHtmlElements
          c.children = children
          c.tmpReactives = tmpRs
          c.eventHandlers = eIds
          window.setTimeout(() => c.clearElements(), duration.toMillis.toDouble)
          c
        }
      } else {
        Right(c)
      }
    }
  }

  def apply(openV: Reactive[Boolean])
           (duration: FiniteDuration)
           (genHtml: ComponentManager => HTML): (HTMLElement, AddMethod) => HtmlElementsComponentController = {
    apply(_, _, openV, duration)(genHtml)
  }

}
