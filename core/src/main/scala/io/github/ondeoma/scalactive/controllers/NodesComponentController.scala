package io.github.ondeoma.scalactive.controllers

import io.github.ondeoma.scalactive.reactive.*
import io.github.ondeoma.scalactive.utils.TypeAlias.*
import org.scalajs.dom.*

class NodesComponentController(val initF: NodesComponentController => Either[String, NodesComponentController],
                               var parent: Option[HTMLElement] = None,
                               var nodes: List[Node] = Nil,
                               var children: ChildrenComponents = Nil,
                               var watchInfos: WatchInfos = Nil,
                               var abortControllers: List[AbortController] = Nil,
                               var eventHandlers: List[ID] = Nil,
                               var tmpReactives: List[Reactive[?]] = Nil) extends NodesController[NodesComponentController] {

  def init(): Either[String, NodesComponentController] = initF(this)

}
