package io.github.ondeoma.scalactive.components.texts

import cats.syntax.all.*
import io.github.ondeoma.scalactive.components.BaseComponent
import io.github.ondeoma.scalactive.controllers.TextComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.Reactive
import org.scalajs.dom.HTMLElement

object Text5Component extends BaseComponent {

  def apply[A, B, C, D, E](root: HTMLElement,
                           m: AddMethod,
                           rv1: Reactive[A],
                           rv2: Reactive[B],
                           rv3: Reactive[C],
                           rv4: Reactive[D],
                           rv5: Reactive[E],
                           formatter: (A, B, C, D, E) => String): TextComponentController = {
    TextComponentController { c =>
      val format = () => formatter(rv1.v, rv2.v, rv3.v, rv4.v, rv5.v)
      addText(root)(m, format()).map { text =>
        val replace = () => text.data = format()
        val wis = List(
          rv1.addWatcher(_ => replace()),
          rv2.addWatcher(_ => replace()),
          rv3.addWatcher(_ => replace()),
          rv4.addWatcher(_ => replace()),
          rv5.addWatcher(_ => replace()),
        )
        c.text = text.some
        c.watchInfos = wis
        c
      }.toRight(genErrorMessage)
    }
  }

  def apply[A, B, C, D, E](rv1: Reactive[A],
                           rv2: Reactive[B],
                           rv3: Reactive[C],
                           rv4: Reactive[D],
                           rv5: Reactive[E])
                          (formatter: (A, B, C, D, E) => String): (HTMLElement, AddMethod) => TextComponentController = {
    apply(_, _, rv1, rv2, rv3, rv4, rv5, formatter)
  }

}
