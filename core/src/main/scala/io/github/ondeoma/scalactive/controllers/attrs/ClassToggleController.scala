package io.github.ondeoma.scalactive.controllers.attrs

import cats.syntax.all.*
import io.github.ondeoma.scalactive.reactive.Reactive
import io.github.ondeoma.scalactive.utils.DomUtil.*
import org.scalajs.dom.*

object ClassToggleController {

  def apply(ele: HTMLElement,
            rv: Reactive[Boolean],
            ifTrue: List[String],
            ifFalse: List[String]): ClassController = {
    ClassController { ac =>
      val set = () => {
        if (rv.v) {
          ele.rmClasses(ifFalse)
          ele.addClasses(ifTrue)
          ac.nowClss = ifTrue
        } else {
          ele.rmClasses(ifTrue)
          ele.addClasses(ifFalse)
          ac.nowClss = ifFalse
        }
      }
      ac.element = ele.some
      ac.watchInfos = List(rv.addWatcher(_ => set()))
      set()
      Right(ac)
    }
  }

  def apply(rv: Reactive[Boolean],
            ifTrue: List[String],
            ifFalse: List[String]): HTMLElement => ClassController = {
    apply(_, rv, ifTrue, ifFalse)
  }


}
