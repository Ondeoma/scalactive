package io.github.ondeoma.scalactive.controllers.attrs

import cats.syntax.all.*
import org.scalajs.dom.*
import io.github.ondeoma.scalactive.syntax.All.*
import io.github.ondeoma.scalactive.reactive.Reactive

object ClassStringsController {

  def apply(ele: HTMLElement,
            rv: Reactive[List[String]]): ClassController = {
    ClassController { ac =>
      val set = () => {
        ele.rmClasses(ac.nowClss)
        ele.addClasses(rv.v)
        ac.nowClss = rv.v
      }
      ac.element = ele.some
      ac.watchInfos = List(rv.addWatcher(_ => set()))
      set()
      Right(ac)
    }
  }

  def apply(rv: Reactive[List[String]]): HTMLElement => ClassController = {
    apply(_, rv)
  }

}
