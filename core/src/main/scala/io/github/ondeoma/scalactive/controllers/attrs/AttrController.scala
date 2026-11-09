package io.github.ondeoma.scalactive.controllers.attrs

import cats.syntax.all.*
import io.github.ondeoma.scalactive.controllers.ComponentController
import io.github.ondeoma.scalactive.utils.TypeAlias.*
import org.scalajs.dom.*

class AttrController(val initF: AttrController => Either[String, AttrController],
                     var element: Option[HTMLElement] = None,
                     var watchInfos: WatchInfos = Nil) extends ComponentController[AttrController] {

  def init(): Either[String, AttrController] = initF(this)

  /** clearのときに呼ばれる処理(テンプレート参照の解除など) */
  var onClear: () => Unit = () => ()

  def clear(): Unit = {
    onClear()
    onClear = () => ()
    element = None
    watchInfos.foreach(_.abort())
    watchInfos = Nil
  }

}
