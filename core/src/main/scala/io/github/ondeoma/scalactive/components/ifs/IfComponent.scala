package io.github.ondeoma.scalactive.components.ifs

import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager}
import org.scalajs.dom.*
import io.github.ondeoma.scalactive.syntax.All.*
import io.github.ondeoma.scalactive.controllers.HtmlElementsComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.Reactive

object IfComponent extends BaseComponent {

  def apply(root: HTMLElement,
            am: AddMethod,
            conditionV: Reactive[Boolean])
           (genHtml: ComponentManager => HTML): HtmlElementsComponentController = {
    HtmlElementsComponentController { c =>
      c.watchInfos = List(conditionV.addWatcher(_ => c.reload()))
      if (conditionV.v) {
        for {
          // Scala 3.4.0~
          // (ns, children, tmpRs, eIds) <- ComponentManager(genHtml)
          t4 <- ComponentManager(genHtml)
          (ns, children, tmpRs, eIds) = t4
          eles = ns.toHtmlElements
          _ <- addNodes(root)(am, ns *).toRight(addNodesErrorMessage)
        } yield {
          c.elements = eles
          c.children = children
          c.tmpReactives = tmpRs
          c.eventHandlers = eIds
          c
        }
      } else Right(c)
    } 
  }

  def apply(conditionV: Reactive[Boolean])
           (genHtml: ComponentManager => HTML): (HTMLElement, AddMethod) => HtmlElementsComponentController = {
    apply(_, _, conditionV)(genHtml)
  }

}
