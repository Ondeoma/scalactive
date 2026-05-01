package io.github.ondeoma.scalactive.components.fors

import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager}
import org.scalajs.dom.*
import io.github.ondeoma.scalactive.syntax.All.*
import io.github.ondeoma.scalactive.controllers.HtmlElementsComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.{ListRV, RV}


object ForRVComponent extends BaseComponent {

  def apply[A](root: HTMLElement,
               am: AddMethod,
               values: ListRV[A])
              (genHtml: (ComponentManager, RV[A], IDX) => HTML): HtmlElementsComponentController = {
    ForGeneralComponent(root, am, values, _.rv, c => List(values.addWatcherRowLevel(_ => c.reload())))(genHtml)
  }

  def apply[A](values: ListRV[A])
              (genHtml: (ComponentManager, RV[A], IDX) => HTML): (HTMLElement, AddMethod) => HtmlElementsComponentController = {
    apply(_, _, values)(genHtml)
  }

}
