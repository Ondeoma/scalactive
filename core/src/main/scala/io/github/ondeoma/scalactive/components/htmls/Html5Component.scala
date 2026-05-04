package io.github.ondeoma.scalactive.components.htmls

import io.github.ondeoma.scalactive.components.BaseComponent
import io.github.ondeoma.scalactive.controllers.NodesComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.Reactive
import org.scalajs.dom.HTMLElement

object Html5Component extends BaseComponent {

  def apply[A, B, C, D, E](root: HTMLElement,
                           m: AddMethod,
                           rv1: Reactive[A],
                           rv2: Reactive[B],
                           rv3: Reactive[C],
                           rv4: Reactive[D],
                           rv5: Reactive[E],
                           formatter: (A, B, C, D, E) => String): NodesComponentController = {
    NodesComponentController { c =>
      val nodes = addHtml(root)(m, formatter(rv1.v, rv2.v, rv3.v, rv4.v, rv5.v))
      val wis = List(
        rv1.addWatcher(_ => c.reload()),
        rv2.addWatcher(_ => c.reload()),
        rv3.addWatcher(_ => c.reload()),
        rv4.addWatcher(_ => c.reload()),
        rv5.addWatcher(_ => c.reload()),
      )
      c.nodes = nodes
      c.watchInfos = wis
      Right(c)
    }
  }

  def apply[A, B, C, D, E, F](rv1: Reactive[A],
                              rv2: Reactive[B],
                              rv3: Reactive[C],
                              rv4: Reactive[D],
                              rv5: Reactive[E])
                             (formatter: (A, B, C, D, E) => HTML): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, rv1, rv2, rv3, rv4, rv5, formatter)
  }

}
