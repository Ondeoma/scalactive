package io.github.ondeoma.scalactive.controllers.attrs

import cats.syntax.all.*
import io.github.ondeoma.scalactive.controllers.ComponentController
import io.github.ondeoma.scalactive.utils.TypeAlias.*
import org.scalajs.dom.*

class AttrController(val initF: AttrController => Either[String, AttrController],
                     var element: Option[HTMLElement] = None,
                     var watchInfos: WatchInfos = Nil) extends ComponentController[AttrController] {

  def init(): Either[String, AttrController] = initF(this)

  def clear(): Unit = {
    element = None
    watchInfos.foreach(_.abort())
    watchInfos = Nil
  }

}
