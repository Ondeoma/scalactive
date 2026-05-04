package io.github.ondeoma.scalactive.enums

import io.github.ondeoma.scalactive.enums.EventType.*

sealed abstract class EventType(name: String) {
  override def toString: String = name
}

object EventType {

  case object blur extends EventType("blur")

  case object click extends EventType("click")

  case object compositionend extends EventType("compositionend")

  case object compositionstart extends EventType("compositionstart")

  case object compositionupdate extends EventType("compositionupdate")

  case object contextmenu extends EventType("contextmenu")

  case object dblclick extends EventType("dblclick")

  case object focus extends EventType("focus")

  case object focusin extends EventType("focusin")

  case object focusout extends EventType("focusout")

  case object input extends EventType("input")

  case object keydown extends EventType("keydown")

  case object keyup extends EventType("keyup")

  case object mousedown extends EventType("mousedown")

  case object mouseenter extends EventType("mouseenter")

  case object mouseleave extends EventType("mouseleave")

  case object mousemove extends EventType("mousemove")

  case object mouseout extends EventType("mouseout")

  case object mouseover extends EventType("mouseover")

  case object mouseup extends EventType("mouseup")

  case object scroll extends EventType("scroll")

  case object scrollend extends EventType("scrollend")

  case object touchcancel extends EventType("touchcancel")

  case object touchend extends EventType("touchend")

  case object touchmove extends EventType("touchmove")

  case object touchstart extends EventType("touchstart")

  case class CustomEventType(name: String) extends EventType(name)

}
