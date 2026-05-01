package io.github.ondeoma.scalactive.components.fors

import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager}
import org.scalajs.dom.*
import io.github.ondeoma.scalactive.syntax.All.*
import io.github.ondeoma.scalactive.controllers.HtmlElementsComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.Reactive


object ForLRComponent extends BaseComponent {

  def apply[A](root: HTMLElement,
               am: AddMethod,
               values: Reactive[List[A]])
              (genHtml: (ComponentManager, A, IDX) => HTML): HtmlElementsComponentController = {
    ForGeneralComponent(root, am, values, _.v, c => List(values.addWatcher(_ => c.reload())))(genHtml)
  }

  def apply[A](values: Reactive[List[A]])
              (genHtml: (ComponentManager, A, IDX) => HTML): (HTMLElement, AddMethod) => HtmlElementsComponentController = {
    apply(_, _, values)(genHtml)
  }

}
