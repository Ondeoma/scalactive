package io.github.ondeoma.scalactive.components.inputs.radios

import cats.syntax.all.*
import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager}
import org.scalajs.dom.*
import io.github.ondeoma.scalactive.syntax.All.*
import io.github.ondeoma.scalactive.controllers.HtmlElementsComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.{RV, Reactive}

object RadioGeneralComponent extends BaseComponent {

  def apply[A](root: HTMLElement,
               am: AddMethod,
               value: String,
               rv: RV[A],
               fromElement: HTMLInputElement => A,
               toChecked: A => Boolean,
               attrs: Map[AttrName, String | Boolean],
               attrRs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
              ): HtmlElementsComponentController = {
    mkSimpleHtmlEsInputCC(
      rv,
      genElement(rv, value, fromElement),
      ele => ele.toInput.foreach(_.checked = toChecked(rv.v)),
      attrs,
      attrRs
    )(root, am)

  }

  def apply[A](value: String,
               rv: RV[A],
               fromElement: HTMLInputElement => A,
               toChecked: A => Boolean,
               attrs: Map[AttrName, String | Boolean],
               attrRs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
              ): (HTMLElement, AddMethod) => HtmlElementsComponentController = {
    apply(_, _, value, rv, fromElement, toChecked, attrs, attrRs)
  }


  private def genElement[V](rv: RV[V],
                            value: String,
                            getByElement: HTMLInputElement => V) = {
    ComponentManager { implicit cc =>
      // language=html
      s"""<input type="radio" 
         |       value="$value"
         |       ${ev(input, _.ifInput(ele => rv := getByElement(ele)))} />""".stripMargin
    }
  }

}
