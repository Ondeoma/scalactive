package io.github.ondeoma.scalactive.components.inputs.links

import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager}
import org.scalajs.dom.*
import io.github.ondeoma.scalactive.syntax.All.*
import io.github.ondeoma.scalactive.controllers.HtmlElementsComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.Reactive

object LinkStaticComponent extends BaseComponent {

  def apply(root: HTMLElement,
            am: AddMethod,
            text: String,
            onClick: Event => Unit,
            attrs: Map[AttrName, String | Boolean],
            attrRs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
           ): HtmlElementsComponentController = {
    mkSimpleHtmlEsWithAttrsCC(genElement(text, onClick, attrs), attrs, attrRs)(root, am)
  }

  def apply(text: String,
            onClick: Event => Unit,
            attrs: Map[AttrName, String | Boolean],
            attrRs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
           ): (HTMLElement, AddMethod) => HtmlElementsComponentController = {
    apply(_, _, text, onClick, attrs, attrRs)
  }

  private def genElement(text: String,
                         onClick: Event => Unit,
                         attrs: Map[AttrName, String | Boolean]) = {
    ComponentManager { implicit cm =>
      // language=html
      s"<a ${expandAttrs(attrs)} ${ev(click, onClick)}>$text</a>"
    }
  }

}
