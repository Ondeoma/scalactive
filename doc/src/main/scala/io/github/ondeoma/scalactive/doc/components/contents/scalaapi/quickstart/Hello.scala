package io.github.ondeoma.scalactive.doc.components.contents.scalaapi.quickstart

import io.github.ondeoma.scalactive.components.*
import io.github.ondeoma.scalactive.reactive.*
import Hello.State

object Hello extends StatefulComponent[State] {

  case class State(count: RV[Int] = RV(0))

  def initState() = State()

  def genHtml(s: State)
             (implicit cm: ComponentManager): HTML = {
    // language=html
    s"""<p>Hello, Scalactive!</p>
       |<button ${evClick(s.count := s.count.v + 1)}>count: ${%(TextComponent(s.count)(_.toString))}</button>
       |""".stripMargin
  }
}
