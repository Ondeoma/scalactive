package io.github.ondeoma.scalactive.components.fors

import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager}
import io.github.ondeoma.scalactive.controllers.NodesComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import org.scalajs.dom.*


object ForStaticComponent extends BaseComponent {

  def apply[A](root: HTMLElement,
               am: AddMethod,
               values: List[A])
              (genHtml: (ComponentManager, A, IDX) => HTML): NodesComponentController = {
    ForGeneralComponent(root, am, values, identity, _ => Nil)(genHtml)
  }

  def apply[A](values: List[A])
              (genHtml: (ComponentManager, A, IDX) => HTML): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, values)(genHtml)
  }

}
