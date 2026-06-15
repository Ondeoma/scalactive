package io.github.ondeoma.scalactive.components

import org.scalajs.dom.*

trait StatelessComponent extends BaseComponent {
  
  def genHtml(implicit cm: ComponentManager): HTML
  
  def apply()
           (implicit parent: HTMLElement,
            am: AddMethod): NodesComponentController = {
    mkNCC(genHtml)
  }
  
}
