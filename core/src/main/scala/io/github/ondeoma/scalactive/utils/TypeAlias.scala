package io.github.ondeoma.scalactive.utils

import io.github.ondeoma.scalactive.controllers.ComponentController
import io.github.ondeoma.scalactive.models.{AddMethod, WatchInfo}
import io.github.ondeoma.scalactive.reactive.Reactive
import org.scalajs.dom.*
import io.github.ondeoma.scalactive.models.*

object TypeAlias {

  type IDX = Int
  type ID = String
  type HTML = String
  type ATTR = String
  type AttrName = String
  type SelectValue = String
  type SelectDisplayName = String

  type WatchInfos = List[WatchInfo]
  type ChildrenComponents = List[ComponentController[?]]
  type GenComponentF = (HTMLElement, AddMethod) => ComponentController[?]
  type ElementEffectF = HTMLElement => ComponentController[?]
  type EventHandlerIds = List[ID]
  type TemporaryReactiveValues = List[Reactive[?]]
  type GenResult = Either[String, (List[HTMLElement], ChildrenComponents, TemporaryReactiveValues, EventHandlerIds)]

}
