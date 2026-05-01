package io.github.ondeoma.scalactive.controllers

import org.scalajs.dom.*
import io.github.ondeoma.scalactive.reactive.*
import io.github.ondeoma.scalactive.utils.TypeAlias.*
import io.github.ondeoma.scalactive.reactive.Reactive

class HtmlElementsComponentController(val initF: HtmlElementsComponentController => Either[String, HtmlElementsComponentController],
                                      var elements: List[HTMLElement] = Nil,
                                      var children: ChildrenComponents = Nil,
                                      var watchInfos: WatchInfos = Nil,
                                      var abortControllers: List[AbortController] = Nil,
                                      var eventHandlers: List[ID] = Nil,
                                      var tmpReactives: List[Reactive[?]] = Nil,
                                     ) extends HtmlElementsController[HtmlElementsComponentController] {

  def init(): Either[String, HtmlElementsComponentController] = initF(this)

}
