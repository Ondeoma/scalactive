package io.github.ondeoma.scalactive.components.inputs.checkboxes

import cats.syntax.all.*
import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager}
import io.github.ondeoma.scalactive.controllers.NodesComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.{RV, Reactive}
import org.scalajs.dom.*

object CheckboxGeneralComponent extends BaseComponent {

  def apply[A](root: HTMLElement,
               am: AddMethod,
               rv: RV[A],
               value: String,
               fromElement: HTMLInputElement => A,
               toChecked: A => Boolean,
               attrs: Map[AttrName, String | Boolean],
               attrRs: Map[AttrName, Reactive[String] | Reactive[Boolean]]): NodesComponentController = {
    mkSimpleHtmlEsInputCC(
      rv,
      genElement(rv, value, fromElement),
      ele => ele.toInput.foreach(_.checked = toChecked(rv.v)),
      attrs,
      attrRs
    )(root, am)
  }

  def apply[A](rv: RV[A],
               value: String,
               fromElement: HTMLInputElement => A,
               toChecked: A => Boolean,
               attrs: Map[AttrName, String | Boolean],
               attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
              ): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, rv, value, fromElement, toChecked, attrs, attrRVs)
  }

  private def genElement[V](rv: RV[V],
                            value: String,
                            getByElement: HTMLInputElement => V) = {
    ComponentManager { implicit cc =>
      // language=html
      s"""<input type="checkbox" 
         |       value="$value"
         |       ${ev(EventType.input, _.ifInput(ele => rv := getByElement(ele)))} />""".stripMargin
    }
  }

}
