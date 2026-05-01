package io.github.ondeoma.scalactive.controllers.attrs

import cats.syntax.all.*
import org.scalajs.dom.*
import io.github.ondeoma.scalactive.syntax.All.*
import io.github.ondeoma.scalactive.reactive.Reactive

object ClassStringController {

  def apply(ele: HTMLElement,
            rv: Reactive[String]): ClassController = {
    ClassController { ac =>
      val set = () => {
        ele.rmClasses(ac.nowClss)
        ele.addClass(rv.v)
        ac.nowClss = List(rv.v)
      }
      ac.element = ele.some
      ac.watchInfos = List(rv.addWatcher(_ => set()))
      set()
      Right(ac)
    }
  }

  def apply(rv: Reactive[String]): HTMLElement => ClassController = {
    apply(_, rv)
  }

}
