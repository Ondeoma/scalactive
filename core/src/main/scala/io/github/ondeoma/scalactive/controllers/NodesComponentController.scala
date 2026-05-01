package io.github.ondeoma.scalactive.controllers

import org.scalajs.dom.*
import io.github.ondeoma.scalactive.reactive.*
import io.github.ondeoma.scalactive.utils.TypeAlias.*

class NodesComponentController(val initF: NodesComponentController => Either[String, NodesComponentController],
                               var nodes: List[Node] = Nil,
                               var children: ChildrenComponents = Nil,
                               var watchInfos: WatchInfos = Nil,
                               var abortControllers: List[AbortController] = Nil,
                               var eventHandlers: List[ID] = Nil) extends NodesController[NodesComponentController] {

  def init(): Either[String, NodesComponentController] = initF(this)

}
