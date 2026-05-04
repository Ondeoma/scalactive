package io.github.ondeoma.scalactive.controllers.attrs

import cats.syntax.all.*
import io.github.ondeoma.scalactive.controllers.ComponentController
import io.github.ondeoma.scalactive.utils.TypeAlias.*
import org.scalajs.dom.*

class ClassController(val initF: ClassController => Either[String, ClassController],
                      var element: Option[HTMLElement] = None,
                      var watchInfos: WatchInfos = Nil,
                      var nowClss: List[String] = Nil,
                     ) extends ComponentController[ClassController] {

  def init(): Either[String, ClassController] = initF(this)

  def clear(): Unit = {
    element = None
    watchInfos.foreach(_.abort())
    watchInfos = Nil
  }

}

