package io.github.ondeoma.scalactive.controllers.attrs

import cats.syntax.all.*
import org.scalajs.dom.*
import io.github.ondeoma.scalactive.syntax.All.*
import io.github.ondeoma.scalactive.reactive.Reactive

object StyleStringController {

  def apply(ele: HTMLElement,
            rv: Reactive[String],
            name: String): StyleController = {
    StyleController { sc =>
      val set = () => ele.setStyle(name, rv.v)
      sc.element = ele.some
      sc.origin = ele.getStyle(name).some
      sc.name = name.some
      sc.watchInfos = List(rv.addWatcher(_ => set()))
      set()
      Right(sc)
    }
  } 

  def apply(rv: Reactive[String],
            name: String): HTMLElement => StyleController = {
    apply(_, rv, name)
  }


}
