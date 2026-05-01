package io.github.ondeoma.scalactive.components.inputs.checkboxes

import cats.syntax.all.*
import org.scalajs.dom.*
import io.github.ondeoma.scalactive.syntax.All.*
import io.github.ondeoma.scalactive.components.BaseComponent
import io.github.ondeoma.scalactive.controllers.HtmlElementsComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.{RV, Reactive}

object CheckboxGroupPartComponent extends BaseComponent {

  def apply(root: HTMLElement,
            am: AddMethod,
            value: String,
            values: RV[List[String]],
            attrs: Map[AttrName, String | Boolean],
            attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
           ): HtmlElementsComponentController = {
    CheckboxGeneralComponent[List[String]](root, am, values, value,
      ele =>
        if (ele.checked) (ele.value :: values.v).distinct
        else values.v.filter(_ != ele.value).distinct,
      _ => values.v.contains(value),
      attrs,
      attrRVs
    )
  }

  def apply(value: String,
            values: RV[List[String]],
            attrs: Map[AttrName, String | Boolean],
            attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
           ): (HTMLElement, AddMethod) => HtmlElementsComponentController = {
    apply(_, _, value, values, attrs, attrRVs)
  }

}
