package io.github.ondeoma.scalactive.controllers.attrs

import cats.syntax.all.*
import io.github.ondeoma.scalactive.controllers.ComponentController
import io.github.ondeoma.scalactive.utils.TypeAlias.*
import org.scalajs.dom.*

class StyleController(val initF: StyleController => Either[String, StyleController],
                      var element: Option[HTMLElement] = None,
                      var name: Option[String] = None,
                      var origin: Option[String] = None,
                      var watchInfos: WatchInfos = Nil) extends ComponentController[StyleController] {

  def init(): Either[String, StyleController] = initF(this)

  def clear(): Unit = {
    element = None
    name = None
    origin = None
    watchInfos.foreach(_.abort())
    watchInfos = Nil
  }

}
