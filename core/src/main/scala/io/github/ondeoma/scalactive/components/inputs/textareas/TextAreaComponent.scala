package io.github.ondeoma.scalactive.components.inputs.textareas

import cats.syntax.all.*
import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager}
import org.scalajs.dom.*
import io.github.ondeoma.scalactive.syntax.All.*
import io.github.ondeoma.scalactive.controllers.HtmlElementsComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.{RV, Reactive}

object TextAreaComponent extends BaseComponent {

  def apply(root: HTMLElement,
            am: AddMethod,
            value: RV[String],
            attrs: Map[AttrName, String | Boolean],
            attrRs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
           ): HtmlElementsComponentController = {
    mkSimpleHtmlEsInputCC(
      value,
      genElement(value),
      ele => setValue(ele, value.v),
      attrs,
      attrRs
    )(root, am)
  }

  def apply(value: RV[String],
            attrs: Map[AttrName, String | Boolean],
            attrRs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
           ): (HTMLElement, AddMethod) => HtmlElementsComponentController = {
    apply(_, _, value, attrs, attrRs)
  }

  private def genElement(rv: RV[String]) = {
    ComponentManager { implicit cc =>
      // language=html
      s"""<textarea ${ev(input, _.ifTextArea(ele => rv := ele.value))} />""".stripMargin
    }
  }

}
 
