package io.github.ondeoma.scalactive.doc.components.contents.component

import io.github.ondeoma.scalactive.components.*
import io.github.ondeoma.scalactive.reactive.*
import Sample2Component.State

object Sample2Component extends StatefulComponent[State] {

  case class State(count: RV[Int] = RV(0))

  def initState() = State()

  def genHtml(s: State)
             (implicit cm: ComponentManager): HTML = {
    // language=html
    s"""<div>
       :  <p>
       :    これは Sample2Component です。<br>
       :    可変値 => ${%(TextComponent(s.count)(_.toString))}
       :  </p>
       :  <button ${evClick(s.count := s.count.v + 1)}>Click</button>
       :</div>
       :""".stripMargin(':')
  }
}  
