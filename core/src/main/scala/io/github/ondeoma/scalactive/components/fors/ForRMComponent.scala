package io.github.ondeoma.scalactive.components.fors

import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager}
import io.github.ondeoma.scalactive.controllers.NodesComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.{RMList, ReactiveModel}
import org.scalajs.dom.*

object ForRMComponent extends BaseComponent {

  def apply[A, M <: ReactiveModel[A, M]](root: HTMLElement,
                                         am: AddMethod,
                                         values: RMList[A, M])
                                        (genHtml: (ComponentManager, M, IDX) => HTML): NodesComponentController = {
    ForGeneralComponent(root, am, values, _.rv, c => List(values.addWatcherRowLevel(_ => c.reload())))(genHtml)
  }

  def apply[A, M <: ReactiveModel[A, M]](vs: RMList[A, M])
                                        (genHtml: (ComponentManager, M, IDX) => HTML): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, vs)(genHtml)
  }

}
