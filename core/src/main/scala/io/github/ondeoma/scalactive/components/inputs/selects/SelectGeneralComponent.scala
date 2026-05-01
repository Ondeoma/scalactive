package io.github.ondeoma.scalactive.components.inputs.selects

import cats.syntax.all.*
import org.scalajs.dom.*
import io.github.ondeoma.scalactive.syntax.All.*
import io.github.ondeoma.scalactive.controllers.HtmlElementsComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.{RV, Reactive}

object SelectGeneralComponent extends SelectComponentBase {

  def apply[A](root: HTMLElement,
               am: AddMethod,
               selects: Reactive[List[Select]] | List[Select],
               rv: RV[A],
               fromSV: SelectValue => A,
               toSV: A => SelectValue,
               attrs: Map[AttrName, String | Boolean],
               attrRs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
              ): HtmlElementsComponentController = {
    mkSimpleHtmlEsInputCC(
      rv,
      genElement(selects, rv, toSV, fromSV),
      ele => setValue(ele, toSV(rv.v)),
      attrs,
      attrRs,
    )(root, am)
  }

  def apply[A](selects: Reactive[List[Select]] | List[Select],
               rv: RV[A],
               fromSelected: SelectValue => A,
               toSelected: A => SelectValue,
               attrs: Map[AttrName, String | Boolean],
               attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
              ): (HTMLElement, AddMethod) => HtmlElementsComponentController = {
    apply(_, _, selects, rv, fromSelected, toSelected, attrs, attrRVs)
  }

}
