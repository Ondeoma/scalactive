package io.github.ondeoma.scalactive.doc.components.contents.component

import io.github.ondeoma.scalactive.components.*
import org.scalajs.dom.HTMLElement

object Sample3Component extends BaseComponent {

  def apply(valueFromParent: String)
           (implicit parent: HTMLElement,
            am: AddMethod): NodesComponentController = {
    mkNCC(genHtml(valueFromParent))
  }

  def genHtml(valueFromParent: String)
             (implicit cm: ComponentManager): HTML = {
    // language=html
    s"""<div>
       :  これは Sample3Component です。<br>
       :  親コンポーネントからは"${valueFromParent}"を受け取りました。
       :</div>
       :""".stripMargin(':')
  }
  
}  
