package io.github.ondeoma.scalactive.components.inputs.selects

import cats.syntax.all.*
import io.github.ondeoma.scalactive.controllers.*
import io.github.ondeoma.scalactive.models.*
import io.github.ondeoma.scalactive.reactive.*
import io.github.ondeoma.scalactive.utils.TypeAlias.*
import org.scalajs.dom.*

object SelectStringComponent extends SelectComponentBase {

  def apply(root: HTMLElement,
            am: AddMethod,
            selects: Reactive[List[(SelectValue, SelectDisplayName)]] | List[(SelectValue, SelectDisplayName)],
            rv: RV[String],
            attrs: Map[AttrName, String | Boolean],
            attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
           ): NodesComponentController = {
    SelectGeneralComponent.apply(root, am, selects, rv, identity, identity, attrs, attrRVs)
  }

  def apply(selects: Reactive[List[(SelectValue, SelectDisplayName)]] | List[(SelectValue, SelectDisplayName)],
            rv: RV[String],
            attrs: Map[AttrName, String | Boolean],
            attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
           ): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, selects, rv, attrs, attrRVs)
  }

}
