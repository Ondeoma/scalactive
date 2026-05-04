package io.github.ondeoma.scalactive.components.fors

import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager}
import io.github.ondeoma.scalactive.controllers.NodesComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.{RVList, RV}
import org.scalajs.dom.*


object ForRVComponent extends BaseComponent {

  def apply[A](root: HTMLElement,
               am: AddMethod,
               values: RVList[A])
              (genHtml: (ComponentManager, RV[A], IDX) => HTML): NodesComponentController = {
    ForGeneralComponent(root, am, values, _.rv, c => List(values.addWatcherRowLevel(_ => c.reload())))(genHtml)
  }

  def apply[A](values: RVList[A])
              (genHtml: (ComponentManager, RV[A], IDX) => HTML): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, values)(genHtml)
  }

}
