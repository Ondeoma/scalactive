package io.github.ondeoma.scalactive.components.fors

import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager}
import io.github.ondeoma.scalactive.controllers.NodesComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.Reactive
import org.scalajs.dom.*


object ForLRComponent extends BaseComponent {

  def apply[A](root: HTMLElement,
               am: AddMethod,
               values: Reactive[List[A]])
              (genHtml: (ComponentManager, A, IDX) => HTML): NodesComponentController = {
    ForGeneralComponent(root, am, values, _.v, c => List(values.addWatcher(_ => c.reload())))(genHtml)
  }

  def apply[A](values: Reactive[List[A]])
              (genHtml: (ComponentManager, A, IDX) => HTML): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, values)(genHtml)
  }

}
