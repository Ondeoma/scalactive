package io.github.ondeoma.scalactive.components.htmls

import io.github.ondeoma.scalactive.components.BaseComponent
import io.github.ondeoma.scalactive.controllers.NodesComponentController
import org.scalajs.dom.HTMLElement

object HtmlComponent extends BaseComponent {

  def apply[A](root: HTMLElement,
               m: AddMethod,
               rv1: Reactive[A],
               formatter: A => HTML = (a: A) => a.toString): NodesComponentController = {
    NodesComponentController { c =>
      val nodes = formatter(rv1.v).toNodes.orDummyNode
      addNodes(c.parent.getOrElse(root))(m, nodes *)
      val wis = List(
        rv1.addWatcher(_ => c.reload()),
      )
      c.nodes = nodes
      c.watchInfos = wis
      Right(c)
    }
  }

  def apply[A](rv1: Reactive[A])
              (formatter: A => HTML): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, rv1, formatter)
  }

}
