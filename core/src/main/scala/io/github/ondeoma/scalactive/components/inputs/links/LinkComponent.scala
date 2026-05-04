package io.github.ondeoma.scalactive.components.inputs.links

import io.github.ondeoma.scalactive.components.texts.TextComponent
import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager}
import io.github.ondeoma.scalactive.controllers.NodesComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.Reactive
import org.scalajs.dom.*

object LinkComponent extends BaseComponent {

  def apply(root: HTMLElement,
            am: AddMethod,
            text: Reactive[String],
            onClick: Event => Unit,
            attrs: Map[AttrName, String | Boolean],
            attrRs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
           ): NodesComponentController = {
    mkSimpleHtmlEsWithAttrsCC(genElement(text, onClick, attrs), attrs, attrRs)(root, am)
  }

  def apply(text: Reactive[String],
            onClick: Event => Unit,
            attrs: Map[AttrName, String | Boolean],
            attrRs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
           ): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, text, onClick, attrs, attrRs)
  }

  private def genElement(text: Reactive[String],
                         onClick: Event => Unit,
                         attrs: Map[AttrName, String | Boolean]): GenResult = {
    ComponentManager { implicit cm =>
      // language=html
      s"<a ${expandAttrs(attrs)} ${ev(EventType.click, onClick)}>${c(TextComponent(text)(identity))}</a>"
    }
  }

}
