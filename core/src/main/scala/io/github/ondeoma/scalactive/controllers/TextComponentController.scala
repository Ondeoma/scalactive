package io.github.ondeoma.scalactive.controllers

import org.scalajs.dom.Text
import io.github.ondeoma.scalactive.utils.TypeAlias.*

class TextComponentController(val initF: TextComponentController => Either[String, TextComponentController],
                              var text: Option[Text] = None,
                              var watchInfos: WatchInfos = Nil) extends ComponentController[TextComponentController] {

  def init(): Either[String, TextComponentController] = initF(this)

  def clear(): Unit = {
    text.foreach(_.data = "")
    text = None
    watchInfos.foreach(_.abort())
    watchInfos = Nil
  }

}
