package io.github.ondeoma.scalactive.enums

import io.github.ondeoma.scalactive.enums.EventType.*

sealed abstract class HtmlInputType(name: String) {
  override def toString: String = name
}

object HtmlInputType {

  sealed abstract class StringInputType(name: String) extends HtmlInputType(name)

  object button extends HtmlInputType("button")

  object checkbox extends HtmlInputType("checkbox")

  object color extends StringInputType("color")

  object date extends StringInputType("date")

  object datetimeLocal extends StringInputType("datetime-local")

  object email extends StringInputType("email")

  object file extends StringInputType("file")

  object hidden extends HtmlInputType("hidden")

  object image extends HtmlInputType("image")

  object month extends StringInputType("month")

  object number extends StringInputType("number")

  object password extends StringInputType("password")

  object radio extends HtmlInputType("radio")

  object range extends StringInputType("range")

  // object reset extends TextInputType("reset)

  object search extends StringInputType("search")

  object submit extends HtmlInputType("submit")

  object tel extends StringInputType("tel")

  object text extends StringInputType("text")

  object time extends StringInputType("time")

  object url extends StringInputType("url")

  object week extends StringInputType("week")


}
