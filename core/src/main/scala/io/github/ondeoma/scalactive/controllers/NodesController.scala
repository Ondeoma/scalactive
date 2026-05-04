package io.github.ondeoma.scalactive.controllers

import io.github.ondeoma.scalactive.events.EventManager
import io.github.ondeoma.scalactive.utils.TypeAlias.*
import org.scalajs.dom.*
import io.github.ondeoma.scalactive.utils.DomUtil.*
import io.github.ondeoma.scalactive.reactive.*

trait NodesController[CC <: ComponentController[CC]] extends ComponentController[CC] {
  self: CC =>
  var parent: Option[HTMLElement]
  var nodes: List[Node]
  var children: ChildrenComponents
  var watchInfos: WatchInfos
  var abortControllers: List[AbortController]
  var eventHandlers: List[ID]
  var tmpReactives: List[Reactive[?]]

  def elements: List[HTMLElement] = {
    nodes.toHtmlElements
  }
  
  def init(): Either[String, CC]

  def clear(): Unit = {
    clearNodes()
    
    watchInfos.foreach(_.abort())
    watchInfos = Nil
  }

  def clearNodes(): Unit = {
    saveParent()
    
    nodes.collect {
      case e: Element => e.remove()
      case t: CharacterData => t.data = ""
    }
    children.foreach(_.clear())
    abortControllers.foreach(_.abort())
    eventHandlers.foreach(EventManager.rmHandler)
    tmpReactives.foreach(_.abort())

    nodes = Nil
    abortControllers = Nil
    children = Nil
    eventHandlers = Nil
    tmpReactives = Nil
  }

  private def saveParent(): Unit = {
    parent = for {
      h <- nodes.headOption
      p = h.parentNode
      h <- p.toHtml
    } yield h
  }

}
