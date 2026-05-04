package io.github.ondeoma.scalactive.components.inputs.texts

import cats.syntax.all.*
import io.github.ondeoma.scalactive.components.BaseComponent
import io.github.ondeoma.scalactive.controllers.NodesComponentController
import io.github.ondeoma.scalactive.enums.EventType
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.{RV, Reactive}
import org.scalajs.dom.*

object TextInputStringComponent extends BaseComponent {

  def apply(root: HTMLElement,
            am: AddMethod,
            inputType: StringInputType,
            value: RV[String],
            attrs: Map[AttrName, String | Boolean],
            attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
            updateEvents: List[EventType],
           ): NodesComponentController = {
    TextInputGeneralComponent.apply[String](root, am, inputType, value, identity, identity, attrs, attrRVs, updateEvents)
  }

  def apply(inputType: StringInputType,
            value: RV[String],
            attrs: Map[AttrName, String | Boolean],
            attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
            updateEvents: List[EventType],
           ): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, inputType, value, attrs, attrRVs, updateEvents)
  }

}
