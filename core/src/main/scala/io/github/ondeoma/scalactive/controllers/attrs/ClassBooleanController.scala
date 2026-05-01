package io.github.ondeoma.scalactive.controllers.attrs

import cats.syntax.all.*
import org.scalajs.dom.*
import io.github.ondeoma.scalactive.syntax.All.*
import io.github.ondeoma.scalactive.reactive.Reactive

object ClassBooleanController {

  def apply(ele: HTMLElement,
            rv: Reactive[Boolean],
            clss: List[String]): ClassController = {
    ClassController { ac =>
      val set = () => {
        if (rv.v) {
          ele.addClasses(clss)
          ac.nowClss = clss
        } else {
          ele.rmClasses(clss)
          ac.nowClss = Nil
        }
      }
      ac.element = ele.some
      ac.watchInfos = List(rv.addWatcher(_ => set()))
      set()
      Right(ac)
    }
  }

  def apply(rv: Reactive[Boolean],
            clss: List[String]): HTMLElement => ClassController = {
    apply(_, rv, clss)
  }


}
