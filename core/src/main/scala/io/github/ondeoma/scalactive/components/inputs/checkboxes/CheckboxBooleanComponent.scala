package io.github.ondeoma.scalactive.components.inputs.checkboxes

import cats.syntax.all.*
import org.scalajs.dom.*
import io.github.ondeoma.scalactive.syntax.All.*
import io.github.ondeoma.scalactive.components.BaseComponent
import io.github.ondeoma.scalactive.controllers.HtmlElementsComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.{RV, Reactive}

object CheckboxBooleanComponent extends BaseComponent {

  def apply(root: HTMLElement,
            am: AddMethod,
            rv: RV[Boolean],
            attrs: Map[AttrName, String | Boolean],
            attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
           ): HtmlElementsComponentController = {
    CheckboxGeneralComponent[Boolean](root, am, rv, "", _.checked, identity, attrs, attrRVs)
  }

  def apply(rv: RV[Boolean],
            attrs: Map[AttrName, String | Boolean],
            attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
           ): (HTMLElement, AddMethod) => HtmlElementsComponentController = {
    apply(_, _, rv, attrs, attrRVs)
  }

}
