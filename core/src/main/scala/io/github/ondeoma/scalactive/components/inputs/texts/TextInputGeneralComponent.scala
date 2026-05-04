package io.github.ondeoma.scalactive.components.inputs.texts

import cats.syntax.all.*
import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager}
import io.github.ondeoma.scalactive.enums.EventType
import io.github.ondeoma.scalactive.enums.EventType.*
import org.scalajs.dom.*

object TextInputGeneralComponent extends BaseComponent {

  def apply[A](root: HTMLElement,
               am: AddMethod,
               inputType: StringInputType,
               value: RV[A],
               fromS: SelectValue => A,
               toS: A => SelectValue,
               attrs: Map[AttrName, String | Boolean],
               attrRs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
               updateEvents: List[EventType],
              ): NodesComponentController = {
    mkSimpleHtmlEsInputCC(
      value,
      genElement(value, fromS, inputType, updateEvents),
      ele => setValue(ele, toS(value.v)),
      attrs,
      attrRs
    )(root, am)
  }

  def apply[A](inputType: StringInputType,
               value: RV[A],
               fromS: SelectValue => A,
               toS: A => SelectValue,
               attrs: Map[AttrName, String | Boolean],
               attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
               updateEvents: List[EventType],
              ): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, inputType, value, fromS, toS, attrs, attrRVs, updateEvents)
  }

  private def genElement[A](rv: RV[A],
                            fromS: SelectValue => A,
                            inputType: StringInputType,
                            updateEvents: List[EventType]) = {
    ComponentManager { implicit cc =>
      // language=html
      s"""<input type="$inputType" ${ev(updateEvents, _.ifInput(ele => rv := fromS(ele.value)))} />""""
    }
  }

}
