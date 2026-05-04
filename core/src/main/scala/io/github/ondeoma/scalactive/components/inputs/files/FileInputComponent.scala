package io.github.ondeoma.scalactive.components.inputs.files

import cats.syntax.all.*
import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager}
import io.github.ondeoma.scalactive.controllers.NodesComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.{RV, Reactive}
import org.scalajs.dom.*

object FileInputComponent extends BaseComponent {

  def apply(root: HTMLElement,
            am: AddMethod,
            files: RV[List[File]],
            attrs: Map[AttrName, String | Boolean],
            attrRs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
           ): NodesComponentController = {
    mkSimpleHtmlEsWithAttrsCC(genElement(files, attrs), attrs, attrRs)(root, am)
  }

  def apply(files: RV[List[File]],
            attrs: Map[AttrName, String | Boolean],
            attrRs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
           ): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, files, attrs, attrRs)
  }

  private def genElement(files: RV[List[File]],
                         attrs: Map[AttrName, String | Boolean]) = {
    ComponentManager { implicit cc =>
      val handler = (ev: Event) => {
        ev.target.toInput.foreach(ele => files := ele.files.toList)
      }
      // language=html
      s"""<input type="file" ${ev(EventType.input, handler)} ${expandAttrs(attrs)} />"""
    }
  }

}
