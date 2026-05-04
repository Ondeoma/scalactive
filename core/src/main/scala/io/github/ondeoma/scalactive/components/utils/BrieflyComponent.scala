package io.github.ondeoma.scalactive.components.utils

import io.github.ondeoma.scalactive.components.BaseComponent
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.Reactive
import org.scalajs.dom.*

import scala.concurrent.duration.FiniteDuration

object BrieflyComponent extends BaseComponent {

  def apply(root: HTMLElement,
            am: AddMethod,
            openV: Reactive[Boolean],
            duration: FiniteDuration)
           (genHtml: CM => HTML): NodesComponentController = {
    NodesComponentController { c =>
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
          c.nodes = ns
          c.children = children
          c.tmpReactives = tmpRs
          c.eventHandlers = eIds
          window.setTimeout(() => c.clearNodes(), duration.toMillis.toDouble)
          c
        }
      } else {
        Right(c)
      }
    }
  }

  def apply(openV: Reactive[Boolean])
           (duration: FiniteDuration)
           (genHtml: CM => HTML): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, openV, duration)(genHtml)
  }

}
