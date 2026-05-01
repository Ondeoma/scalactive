package io.github.ondeoma.scalactive.components.inputs.radios

import cats.syntax.all.*
import org.scalajs.dom.*
import io.github.ondeoma.scalactive.syntax.All.*
import io.github.ondeoma.scalactive.components.BaseComponent
import io.github.ondeoma.scalactive.controllers.HtmlElementsComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.{RV, Reactive}

object RadioStringComponent extends BaseComponent {

  def apply(root: HTMLElement,
            am: AddMethod,
            value: String,
            rv: RV[String],
            attrs: Map[AttrName, String | Boolean],
            attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
           ): HtmlElementsComponentController = {
    RadioGeneralComponent(root, am, value, rv, _.value, _ == value, attrs, attrRVs)
  }

  def apply(value: String,
            rv: RV[String],
            attrs: Map[AttrName, String | Boolean],
            attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
           ): (HTMLElement, AddMethod) => HtmlElementsComponentController = {
    apply(_, _, value, rv, attrs, attrRVs)
  }

}
 
