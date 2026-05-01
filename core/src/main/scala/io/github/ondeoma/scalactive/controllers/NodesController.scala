package io.github.ondeoma.scalactive.controllers

import org.scalajs.dom.*
import io.github.ondeoma.scalactive.utils.TypeAlias.*
import io.github.ondeoma.scalactive.events.EventManager

trait NodesController[CC <: ComponentController[CC]] extends ComponentController[CC] {
  self: CC =>
  var nodes: List[Node]
  var children: ChildrenComponents
  var watchInfos: WatchInfos
  var abortControllers: List[AbortController]
  var eventHandlers: List[ID]

  def init(): Either[String, CC]

  def clear(): Unit = {
    nodes.collect {
      case e: Element => e.remove()
      case t: CharacterData => t.data = ""
    }
    children.foreach(_.clear())
    abortControllers.foreach(_.abort())
    watchInfos.foreach(_.abort())
    eventHandlers.foreach(EventManager.rmHandler)

    nodes = Nil
    abortControllers = Nil
    watchInfos = Nil
    children = Nil
    eventHandlers = Nil
  }

}
