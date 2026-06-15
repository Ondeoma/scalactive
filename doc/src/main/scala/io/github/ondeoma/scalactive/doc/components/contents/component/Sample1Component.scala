package io.github.ondeoma.scalactive.doc.components.contents.component

import io.github.ondeoma.scalactive.components.*
import org.scalajs.dom.window.alert

object Sample1Component extends StatelessComponent {

  def genHtml(implicit cm: ComponentManager): HTML = {
    // language=html
    s"""<div>
       :  <p>これは Sample1Component です。</p>
       :  <button ${evClick(alert("Sample1Component"))}>Click</button>
       :</div>
       :""".stripMargin(':')
  }
}  
