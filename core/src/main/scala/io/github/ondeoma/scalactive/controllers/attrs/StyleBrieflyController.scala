package io.github.ondeoma.scalactive.controllers.attrs

import cats.syntax.all.*
import io.github.ondeoma.scalactive.reactive.Reactive
import io.github.ondeoma.scalactive.utils.DomUtil.*
import org.scalajs.dom.*

import scala.concurrent.duration.FiniteDuration

object StyleBrieflyController {

  def apply(ele: HTMLElement,
            rv: Reactive[Boolean],
            duration: FiniteDuration,
            name: String,
            ifTrue: String,
            ifFalse: String): StyleController = {
    StyleController { sc =>
      val setFalse = () => ele.setStyle(name, ifFalse)
      val set = () => {
        if (rv.v) {
          ele.setStyle(name, ifTrue)
          window.setTimeout(setFalse, duration.toMillis.toDouble)
        } else setFalse()
      }
      sc.element = ele.some
      sc.name = name.some
      sc.origin = ele.getStyle(name).some
      sc.watchInfos = List(rv.addWatcher(_ => set()))
      set()
      Right(sc)
    }
  }

  def apply(rv: Reactive[Boolean],
            duration: FiniteDuration,
            name: String,
            ifTrue: String,
            ifFalse: String): HTMLElement => StyleController = {
    apply(_, rv, duration, name, ifTrue, ifFalse)
  }


}
