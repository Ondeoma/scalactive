package io.github.ondeoma.scalactive.components.inputs.textareas

import cats.syntax.all.*
import io.github.ondeoma.scalactive.components.BaseComponent
import io.github.ondeoma.scalactive.controllers.*
import io.github.ondeoma.scalactive.models.*
import io.github.ondeoma.scalactive.reactive.*
import io.github.ondeoma.scalactive.utils.TypeAlias.*
import org.scalajs.dom.*


object TextAreaComponent extends BaseComponent {

  def apply(root: HTMLElement,
            am: AddMethod,
            value: RV[String],
            attrs: Map[AttrName, String | Boolean],
            attrRs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
            updateEvents: List[EventType],
           ): NodesComponentController = {
    mkInputCC(
      value,
      genElement(value, updateEvents),
      ele => setValue(ele, value.v),
      attrs,
      attrRs
    )(root, am)
  }

  def apply(value: RV[String],
            attrs: Map[AttrName, String | Boolean],
            attrRs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
            updateEvents: List[EventType],
           ): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, value, attrs, attrRs, updateEvents)
  }

  private def genElement(rv: RV[String],
                         updateEvents: List[EventType]) = {
    ComponentManager { implicit cc =>
      // language=html
      s"""<textarea ${ev(updateEvents, _.ifTextArea(ele => rv := ele.value))} />""".stripMargin
    }
  }

}
 
