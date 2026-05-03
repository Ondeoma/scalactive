package io.github.ondeoma.scalactive.controllers.attrs

import cats.syntax.all.*
import org.scalajs.dom.*
import io.github.ondeoma.scalactive.syntax.All.*
import io.github.ondeoma.scalactive.reactive.Reactive

import scala.concurrent.duration.FiniteDuration

object ClassBrieflyController {

  def apply(ele: HTMLElement,
            rv: Reactive[Boolean],
            duration: FiniteDuration,
            clss: List[String]): ClassController = {
    ClassController { ac =>
      val setFalse = () => {
        ele.rmClasses(clss)
        ac.nowClss = Nil
      }
      val set = () => {
        if (rv.v) {
          ele.addClasses(clss)
          ac.nowClss = clss
          window.setTimeout(setFalse, duration.toMillis.toDouble)
        } else setFalse()
      }
      ac.element = ele.some
      ac.watchInfos = List(rv.addWatcher(_ => set()))
      set()
      Right(ac)
    }
  }

  def apply(rv: Reactive[Boolean],
            duration: FiniteDuration,
            clss: List[String]): HTMLElement => ClassController = {
    apply(_, rv, duration, clss)
  }


}
