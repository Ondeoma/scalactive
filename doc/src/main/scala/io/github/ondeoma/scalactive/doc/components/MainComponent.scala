package io.github.ondeoma.scalactive.doc.components

import io.github.ondeoma.scalactive.components.*
import org.scalajs.dom.*

object MainComponent extends StatelessComponent {
  
  def genHtml(implicit cm: ComponentManager): HTML = {
    // language=html
    s"""
       |${c(HeaderComponent())}
       |${c(SpNavComponent())}
       |<div class="main-area">
       |  ${c(SideNavComponent())}
       |  ${c(ContentComponent())}
       |</div>
       |${c(FooterComponent())}
       |""".stripMargin
  }

}
