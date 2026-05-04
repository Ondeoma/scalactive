package io.github.ondeoma.scalactive.components.ifs

import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager}
import io.github.ondeoma.scalactive.controllers.NodesComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.Reactive
import org.scalajs.dom.*

object IfComponent extends BaseComponent {

  def apply(root: HTMLElement,
            am: AddMethod,
            conditionV: Reactive[Boolean])
           (genHtml: ComponentManager => HTML): NodesComponentController = {
    NodesComponentController { c =>
      c.watchInfos = List(conditionV.addWatcher(_ => c.reload()))
      if (conditionV.v) {
        for {
          // Scala 3.4.0~
          // (ns, children, tmpRs, eIds) <- ComponentManager(genHtml)
          t4 <- ComponentManager(genHtml)
          (ns, children, tmpRs, eIds) = t4
          _ <- addNodes(root)(am, ns *).toRight(addNodesErrorMessage)
        } yield {
          c.nodes = ns
          c.children = children
          c.tmpReactives = tmpRs
          c.eventHandlers = eIds
          c
        }
      } else {
        val ns = List(new Comment)
        for {
          _ <- addNodes(root)(am, ns *).toRight(addNodesErrorMessage)
        } yield {
          c.nodes = ns
          c
        }
      }
    }
  }

  def apply(conditionV: Reactive[Boolean])
           (genHtml: ComponentManager => HTML): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, conditionV)(genHtml)
  }

}
