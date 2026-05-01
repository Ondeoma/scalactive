package io.github.ondeoma.scalactive.components.inputs.selects

import cats.syntax.all.*
import org.scalajs.dom.*
import io.github.ondeoma.scalactive.syntax.All.*
import io.github.ondeoma.scalactive.controllers.HtmlElementsComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.{RV, Reactive}

object SelectStringComponent extends SelectComponentBase {

  def apply(root: HTMLElement,
            am: AddMethod,
            selects: Reactive[List[(SelectValue, SelectDisplayName)]] | List[(SelectValue, SelectDisplayName)],
            rv: RV[String],
            attrs: Map[AttrName, String | Boolean],
            attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
           ): HtmlElementsComponentController = {
    SelectGeneralComponent.apply(root, am, selects, rv, identity, identity, attrs, attrRVs)
  }

  def apply(selects: Reactive[List[(SelectValue, SelectDisplayName)]] | List[(SelectValue, SelectDisplayName)],
            rv: RV[String],
            attrs: Map[AttrName, String | Boolean],
            attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
           ): (HTMLElement, AddMethod) => HtmlElementsComponentController = {
    apply(_, _, selects, rv, attrs, attrRVs)
  }

}
