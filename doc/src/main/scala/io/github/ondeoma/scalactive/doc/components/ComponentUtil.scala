package io.github.ondeoma.scalactive.doc.components

import io.github.ondeoma.scalactive.components.*
import io.github.ondeoma.scalactive.components.ComponentManager.*

object ComponentUtil {

  def txtC(rv: Reactive[String])
          (using ComponentManager): HTML = {
    %(TextComponent(rv)(identity))
  }

  def htmlC(rv: Reactive[String])
           (using ComponentManager): HTML = {
    %(HtmlComponent(rv)(identity))
  }

  def txtInputC(rv: RV[String])
               (using ComponentManager): HTML = {
    %(TextInputStringComponent(HtmlInputType.text, rv, Map("class" -> "input"), Map(), List(EventType.input)))
  }

}
