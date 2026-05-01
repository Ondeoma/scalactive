package io.github.ondeoma.scalactive.components.fors

import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager}
import org.scalajs.dom.*
import io.github.ondeoma.scalactive.syntax.All.*
import io.github.ondeoma.scalactive.controllers.HtmlElementsComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.{ListRM, ReactiveModel}

object ForRMComponent extends BaseComponent {

  def apply[A, M <: ReactiveModel[A, M]](root: HTMLElement,
                                         am: AddMethod,
                                         values: ListRM[A, M])
                                        (genHtml: (ComponentManager, M, IDX) => HTML): HtmlElementsComponentController = {
    ForGeneralComponent(root, am, values, _.rv, c => List(values.addWatcherRowLevel(_ => c.reload())))(genHtml)
  }

  def apply[A, M <: ReactiveModel[A, M]](vs: ListRM[A, M])
                                        (genHtml: (ComponentManager, M, IDX) => HTML): (HTMLElement, AddMethod) => HtmlElementsComponentController = {
    apply(_, _, vs)(genHtml)
  }

}
