package io.github.ondeoma.scalactive.utils

import io.github.ondeoma.scalactive.components.ComponentManager
import io.github.ondeoma.scalactive.controllers.ComponentController
import io.github.ondeoma.scalactive.models.*
import io.github.ondeoma.scalactive.reactive.Reactive
import org.scalajs.dom.*

object TypeAlias {

  type IDX = Int
  type ID = String
  type HTML = String
  type ATTR = String
  type AttrName = String
  type SelectValue = String
  type SelectDisplayName = String

  type CM = ComponentManager
  type WatchInfos = List[WatchInfo]
  type ChildrenComponents = List[ComponentController[?]]
  type GenComponentF = (HTMLElement, AddMethod) => ComponentController[?]
  type ElementEffectF = HTMLElement => ComponentController[?]
  type EventHandlerIds = List[ID]
  type TemporaryReactiveValues = List[Reactive[?]]
  type GenResult = Either[String, (List[Node], ChildrenComponents, TemporaryReactiveValues, EventHandlerIds)]

}
