package io.github.ondeoma.scalactive.components.inputs.buttons

import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager}
import org.scalajs.dom.*
import io.github.ondeoma.scalactive.syntax.All.*
import io.github.ondeoma.scalactive.controllers.HtmlElementsComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.Reactive

object ButtonComponent extends BaseComponent {

  def apply(root: HTMLElement,
            am: AddMethod,
            text: String,
            onClick: Event => Unit,
            attrs: Map[AttrName, String | Boolean],
            attrRs: Map[AttrName, Reactive[String] | Reactive[Boolean]]): HtmlElementsComponentController = {
    mkSimpleHtmlEsWithAttrsCC(genElement(onClick, text, attrs), attrs, attrRs)(root, am)
  }

  def apply(text: String,
            onClick: Event => Unit,
            attrs: Map[AttrName, String | Boolean],
            attrRs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
           ): (HTMLElement, AddMethod) => HtmlElementsComponentController = {
    apply(_, _, text, onClick, attrs, attrRs)
  }

  private def genElement(onClick: Event => Unit,
                         text: String,
                         attrs: Map[AttrName, String | Boolean]) = {
    ComponentManager { implicit cc =>
      s"""<button ${ev(click, onClick)} ${expandAttrs(attrs)}>$text</button>"""
    }
  }

}
