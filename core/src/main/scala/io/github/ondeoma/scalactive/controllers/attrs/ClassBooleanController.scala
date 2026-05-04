package io.github.ondeoma.scalactive.controllers.attrs

import cats.syntax.all.*
import io.github.ondeoma.scalactive.reactive.Reactive
import io.github.ondeoma.scalactive.utils.DomUtil.*
import org.scalajs.dom.*

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
