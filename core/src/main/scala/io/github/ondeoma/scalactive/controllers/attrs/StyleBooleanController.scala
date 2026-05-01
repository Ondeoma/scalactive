package io.github.ondeoma.scalactive.controllers.attrs

import cats.syntax.all.*
import org.scalajs.dom.*
import io.github.ondeoma.scalactive.syntax.All.*
import io.github.ondeoma.scalactive.reactive.Reactive

object StyleBooleanController {

  def apply(ele: HTMLElement,
            rv: Reactive[Boolean],
            name: String,
            value: String): StyleController = {
    StyleController { sc =>
      val set = () => {
        if (rv.v) ele.setStyle(name, value)
        else ele.setStyle(name, sc.origin.getOrElse(""))
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
            name: String,
            value: String): HTMLElement => StyleController = {
    apply(_, rv, name, value)
  }


}
