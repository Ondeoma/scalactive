package io.github.ondeoma.scalactive.doc.components

import io.github.ondeoma.scalactive.components.*
import io.github.ondeoma.scalactive.reactive.*
import SpNavComponent.State

object SpNavComponent extends StatefulComponent[State] {

  case class State(open: RV[Boolean] = RV(false))

  def initState() = State()
  
  def genHtml(s: State)
             (implicit cm: ComponentManager): HTML = {
    import Router.*
    href.addWatcher(_ => s.open := false)
    val menuIcon = """<svg data-v-0f6864fa="" xmlns="http://www.w3.org/2000/svg" aria-hidden="true" focusable="false" viewBox="0 0 24 24" class="menu-icon"><path d="M17,11H3c-0.6,0-1-0.4-1-1s0.4-1,1-1h14c0.6,0,1,0.4,1,1S17.6,11,17,11z"></path><path d="M21,7H3C2.4,7,2,6.6,2,6s0.4-1,1-1h18c0.6,0,1,0.4,1,1S21.6,7,21,7z"></path><path d="M21,15H3c-0.6,0-1-0.4-1-1s0.4-1,1-1h18c0.6,0,1,0.4,1,1S21.6,15,21,15z"></path><path d="M17,19H3c-0.6,0-1-0.4-1-1s0.4-1,1-1h14c0.6,0,1,0.4,1,1S17.6,19,17,19z"></path></svg>"""
    // language=html
    s"""<nav class="sp-menu sp-only-block">
       |  <a ${evClick(s.open := !s.open.v)}>${menuIcon}</a>
       |</nav>
       |<div ${showIf(s.open)}
       |     ${evClick(s.open := false)}
       |     class="sp-side-menu-bg"></div>
       |<div ${showIf(s.open)} class="sp-side-menu">
       |  ${c(SideNavComponent())} 
       |</div>
       |""".stripMargin
  }

}
