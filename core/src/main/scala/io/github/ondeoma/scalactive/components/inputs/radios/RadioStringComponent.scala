package io.github.ondeoma.scalactive.components.inputs.radios

import cats.syntax.all.*
import io.github.ondeoma.scalactive.components.BaseComponent
import io.github.ondeoma.scalactive.controllers.NodesComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.{RV, Reactive}
import org.scalajs.dom.*

object RadioStringComponent extends BaseComponent {

  def apply(root: HTMLElement,
            am: AddMethod,
            rv: RV[String],
            value: String,
            attrs: Map[AttrName, String | Boolean],
            attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
           ): NodesComponentController = {
    RadioGeneralComponent(root, am, rv, value, _.value, _ == value, attrs, attrRVs)
  }

  def apply(rv: RV[String],
            value: String,
            attrs: Map[AttrName, String | Boolean],
            attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
           ): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, rv, value, attrs, attrRVs)
  }

}
 
