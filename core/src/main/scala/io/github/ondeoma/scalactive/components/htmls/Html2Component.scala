package io.github.ondeoma.scalactive.components.htmls

import io.github.ondeoma.scalactive.components.BaseComponent
import io.github.ondeoma.scalactive.controllers.NodesComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.Reactive
import org.scalajs.dom.HTMLElement

object Html2Component extends BaseComponent {

  def apply[A, B](root: HTMLElement,
                  m: AddMethod,
                  rv1: Reactive[A],
                  rv2: Reactive[B],
                  formatter: (A, B) => String): NodesComponentController = {
    NodesComponentController { c =>
      val nodes = addHtml(root)(m, formatter(rv1.v, rv2.v))
      val wis = List(
        rv1.addWatcher(_ => c.reload()),
        rv2.addWatcher(_ => c.reload()),
      )
      c.nodes = nodes
      c.watchInfos = wis
      Right(c)
    }
  }

  def apply[A, B, C](rv1: Reactive[A],
                     rv2: Reactive[B])
                    (formatter: (A, B) => HTML): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, rv1, rv2, formatter)
  }

}
