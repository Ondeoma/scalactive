package io.github.ondeoma.scalactive.doc

import io.github.ondeoma.scalactive.components.ComponentManager.*
import io.github.ondeoma.scalactive.doc.components.MainComponent
import org.scalajs.dom.*
import org.scalajs.dom.document.body
import io.github.ondeoma.scalactive.ScalactiveConfig

object App {

  @main
  def main(): Unit = {
    ScalactiveConfig.basePath = "scalactive"
    MainComponent()(body, AddMethod.appendS(IdSelector("app"))).init()
  } 

}
