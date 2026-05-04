package io.github.ondeoma.scalactive.components.inputs.checkboxes

import cats.syntax.all.*
import io.github.ondeoma.scalactive.components.BaseComponent
import io.github.ondeoma.scalactive.controllers.NodesComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.{RV, Reactive}
import org.scalajs.dom.*

object CheckboxBooleanComponent extends BaseComponent {

  def apply(root: HTMLElement,
            am: AddMethod,
            rv: RV[Boolean],
            attrs: Map[AttrName, String | Boolean],
            attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
           ): NodesComponentController = {
    CheckboxGeneralComponent[Boolean](root, am, rv, "", _.checked, identity, attrs, attrRVs)
  }

  def apply(rv: RV[Boolean],
            attrs: Map[AttrName, String | Boolean],
            attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
           ): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, rv, attrs, attrRVs)
  }

}
