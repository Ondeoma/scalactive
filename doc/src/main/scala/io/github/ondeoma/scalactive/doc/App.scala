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
    // ルートのタイトルの書式(どのルートにもタイトルがなければ、index.htmlのタイトルのまま)
    ScalactiveConfig.titleTemplate = "%s | Scalactive Doc"
    // ブラウザのコンソールから scalactive.stats() などを使えるようにする
    io.github.ondeoma.scalactive.Devtools.install()
    MainComponent()(body, AddMethod.appendS(IdSelector("app"))).init()
  } 

}
