package io.github.ondeoma.scalactive.components.inputs.texts

import cats.syntax.all.*
import org.scalajs.dom.*
import io.github.ondeoma.scalactive.syntax.All.*
import io.github.ondeoma.scalactive.components.BaseComponent
import io.github.ondeoma.scalactive.controllers.HtmlElementsComponentController
import io.github.ondeoma.scalactive.enums.EventType
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.{RV, Reactive}

object TextInputStringComponent extends BaseComponent {

  def apply(root: HTMLElement,
            am: AddMethod,
            inputType: StringInputType,
            value: RV[String],
            attrs: Map[AttrName, String | Boolean],
            attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
            updateEvents: List[EventType],
           ): HtmlElementsComponentController = {
    TextInputGeneralComponent.apply[String](root, am, inputType, value, identity, identity, attrs, attrRVs, updateEvents)
  }

  def apply(inputType: StringInputType,
            value: RV[String],
            attrs: Map[AttrName, String | Boolean],
            attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
            updateEvents: List[EventType],
           ): (HTMLElement, AddMethod) => HtmlElementsComponentController = {
    apply(_, _, inputType, value, attrs, attrRVs, updateEvents)
  }

}
