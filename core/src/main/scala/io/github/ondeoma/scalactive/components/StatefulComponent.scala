package io.github.ondeoma.scalactive.components

import org.scalajs.dom.*

trait StatefulComponent[S] extends BaseComponent {

  def initState(): S

  def genHtml(s: S)
             (implicit cm: ComponentManager): HTML
  
  def apply()
           (implicit parent: HTMLElement,
            am: AddMethod): NodesComponentController = {
    mkNCC(genHtml(initState())(_))
  }

}
