package io.github.ondeoma.scalactive.doc.components

import io.github.ondeoma.scalactive.components.*

object FooterComponent extends StatelessComponent {

  def genHtml(implicit cm: ComponentManager): HTML = {
    
    val go = Router.go 
    
    // language=html
    s"""<footer></footer>
       |""".stripMargin
  }
  
}
