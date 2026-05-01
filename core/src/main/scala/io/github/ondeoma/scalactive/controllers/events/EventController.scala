package io.github.ondeoma.scalactive.controllers.events

import cats.syntax.all.*
import org.scalajs.dom.*
import io.github.ondeoma.scalactive.utils.DomUtil.*
import io.github.ondeoma.scalactive.reactive.*
import io.github.ondeoma.scalactive.utils.TypeAlias.*
import io.github.ondeoma.scalactive.controllers.ComponentController
import io.github.ondeoma.scalactive.enums.EventType

class EventController(val initF: EventController => Either[String, EventController],
                      var element: Option[HTMLElement] = None,
                      var abortController: Option[AbortController] = None) extends ComponentController[EventController] {

  def init(): Either[String, EventController] = initF(this)

  def clear(): Unit = {
    element = None
    abortController.foreach(_.abort())
    abortController = None
  }

}

object EventController {

  def apply(ele: HTMLElement,
            et: EventType,
            handler: Event => Unit): EventController = {
    new EventController(ec =>
      ec.element = ele.some
      ec.abortController = addEventToElement(ele, et, handler).some
      Right(ec)
    )
  }

  def apply(et: EventType,
            handler: Event => Unit): HTMLElement => EventController = {
    apply(_, et, handler)
  }

}
