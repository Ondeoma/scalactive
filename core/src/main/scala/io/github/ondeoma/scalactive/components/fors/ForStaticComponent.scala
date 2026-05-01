package io.github.ondeoma.scalactive.components.fors

import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager}
import org.scalajs.dom.*
import io.github.ondeoma.scalactive.syntax.All.*
import io.github.ondeoma.scalactive.controllers.HtmlElementsComponentController
import io.github.ondeoma.scalactive.models.AddMethod


object ForStaticComponent extends BaseComponent {

  def apply[A](root: HTMLElement,
               am: AddMethod,
               values: List[A])
              (genHtml: (ComponentManager, A, IDX) => HTML): HtmlElementsComponentController = {
    ForGeneralComponent(root, am, values, identity, _ => Nil)(genHtml)
  }

  def apply[A](values: List[A])
              (genHtml: (ComponentManager, A, IDX) => HTML): (HTMLElement, AddMethod) => HtmlElementsComponentController = {
    apply(_, _, values)(genHtml)
  }

}
