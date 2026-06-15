package io.github.ondeoma.scalactive.doc.components.contents.component

import io.github.ondeoma.scalactive.components.*

object Sample4Component extends StatelessComponent {
  
  def genHtml(implicit cm: ComponentManager): HTML = {
    // language=html
    s"""<div>
       :  <p>これは Sample4Component です。</p>
       :  <hr>
       :  ${%(Sample1Component())}
       :  ${%(Sample2Component())}
       :  ${%(Sample3Component("BySample4Component"))}
       :</div>
       :""".stripMargin(':')
  }
  
}  
