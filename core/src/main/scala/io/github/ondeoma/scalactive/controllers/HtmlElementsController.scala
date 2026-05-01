package io.github.ondeoma.scalactive.controllers

import org.scalajs.dom.*
import io.github.ondeoma.scalactive.utils.TypeAlias.*
import io.github.ondeoma.scalactive.events.EventManager
import io.github.ondeoma.scalactive.reactive.Reactive
import io.github.ondeoma.scalactive.reactive.*

trait HtmlElementsController[CC <: ComponentController[CC]] extends ComponentController[CC] {
  self: CC =>
  var elements: List[HTMLElement]
  var children: ChildrenComponents
  var watchInfos: WatchInfos
  var abortControllers: List[AbortController]
  var eventHandlers: List[ID]
  var tmpReactives: List[Reactive[?]]

  def init(): Either[String, CC]

  def clear(): Unit = {
    clearElements()

    watchInfos.foreach(_.abort())
    watchInfos = Nil
  }
  
  def clearElements(): Unit = {
    elements.foreach(_.remove())
    children.foreach(_.clear())
    abortControllers.foreach(_.abort())
    eventHandlers.foreach(EventManager.rmHandler)
    tmpReactives.foreach(_.abort())

    elements = Nil
    abortControllers = Nil
    children = Nil
    eventHandlers = Nil
    tmpReactives = Nil
  }

}
