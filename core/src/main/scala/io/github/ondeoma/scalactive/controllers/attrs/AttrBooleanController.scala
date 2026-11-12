package io.github.ondeoma.scalactive.controllers.attrs

import cats.syntax.all.*
import io.github.ondeoma.scalactive.reactive.Reactive
import org.scalajs.dom.*

object AttrBooleanController {

  def apply(ele: HTMLElement,
            attr: String,
            rv: Reactive[Boolean],
            value: String): AttrController = {
    AttrController { ac =>
      val set = () => {
        if (rv.v) FormProps.set(ele, attr, value)
        else FormProps.remove(ele, attr)
      }
      ac.element = ele.some
      ac.watchInfos = List(rv.addWatcher(_ => set()))
      set()
      Right(ac)
    }
  }

  def apply(attr: String,
            rv: Reactive[Boolean],
            value: String): HTMLElement => AttrController = {
    apply(_, attr, rv, value)
  }
}
