package io.github.ondeoma.scalactive.components.inputs.links

import io.github.ondeoma.scalactive.components.inputs.links.LinkComponent.ComponentManager
import io.github.ondeoma.scalactive.components.texts.TextComponent
import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager}
import io.github.ondeoma.scalactive.controllers.NodesComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.Reactive
import org.scalajs.dom.*

object LinkComponent extends BaseComponent {

  def apply(root: HTMLElement,
            am: AddMethod,
            text: String | Reactive[String],
            onClick: Event => Unit,
            attrs: Map[AttrName, String | Boolean],
            attrRs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
           ): NodesComponentController = {
    mkNCCwAttrs(genElement(text, onClick, attrs), attrs, attrRs)(root, am)
  }

  def apply(text: String | Reactive[String],
            onClick: Event => Unit,
            attrs: Map[AttrName, String | Boolean],
            attrRs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
           ): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, text, onClick, attrs, attrRs)
  }

  private def genElement(text: String | Reactive[String],
                         onClick: Event => Unit,
                         attrs: Map[AttrName, String | Boolean])
                        (implicit cm: ComponentManager): HTML = {
    val txt = text match {
      case t: String => t
      case t: Reactive[String] => c(TextComponent(t)(identity))
    }
    // language=html
    s"<a ${expandAttrs(attrs)} ${ev(EventType.click, onClick)}>$txt</a>"
  }


}
