package io.github.ondeoma.scalactive.controllers.attrs

import cats.syntax.all.*
import io.github.ondeoma.scalactive.reactive.Reactive
import io.github.ondeoma.scalactive.utils.DomUtil.*
import org.scalajs.dom.*

object StyleToggleController {

  def apply(ele: HTMLElement,
            rv: Reactive[Boolean],
            name: String,
            ifTrue: String,
            ifFalse: String): StyleController = {
    StyleController { sc =>
      val set = () => ele.setStyle(name, if (rv.v) ifTrue else ifFalse)
      sc.element = ele.some
      sc.name = name.some
      sc.origin = ele.getStyle(name).some
      sc.watchInfos = List(rv.addWatcher(_ => set()))
      set()
      Right(sc)
    }
  }

  def apply(rv: Reactive[Boolean],
            name: String,
            ifTrue: String,
            ifFalse: String): HTMLElement => StyleController = {
    apply(_, rv, name, ifTrue, ifFalse)
  }


}
