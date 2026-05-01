package io.github.ondeoma.scalactive.controllers.attrs

import cats.syntax.all.*
import org.scalajs.dom.*
import io.github.ondeoma.scalactive.syntax.All.*
import io.github.ondeoma.scalactive.reactive.Reactive

object AttrStringController {

  def apply(ele: HTMLElement,
            attr: String,
            rv: Reactive[String]): AttrController = {
    AttrController { ac =>
      val set = () => ele.setAttribute(attr, rv.v)
      ac.element = ele.some
      ac.watchInfos = List(rv.addWatcher(_ => set()))
      set()
      Right(ac)
    }
  }

  def apply(attr: String,
            rv: Reactive[String]): HTMLElement => AttrController = {
    apply(_, attr, rv)
  }

}
