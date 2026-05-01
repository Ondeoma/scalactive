package io.github.ondeoma.scalactive.components.texts

import cats.syntax.all.*
import org.scalajs.dom.HTMLElement
import io.github.ondeoma.scalactive.syntax.All.*
import io.github.ondeoma.scalactive.components.BaseComponent
import io.github.ondeoma.scalactive.controllers.TextComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.Reactive

object Text3Component extends BaseComponent {

  def apply[A, B, C](root: HTMLElement,
                     m: AddMethod,
                     rv1: Reactive[A],
                     rv2: Reactive[B],
                     rv3: Reactive[C],
                     formatter: (A, B, C) => String): TextComponentController = {
    TextComponentController { c =>
      val format = () => formatter(rv1.v, rv2.v, rv3.v)
      addText(root)(m, format()).map { text =>
        val replace = () => text.data = format()
        val wis = List(
          rv1.addWatcher(_ => replace()),
          rv2.addWatcher(_ => replace()),
          rv3.addWatcher(_ => replace()),
        )
        c.text = text.some
        c.watchInfos = wis
        c
      }.toRight(genErrorMessage)
    }
  }

  def apply[A, B, C](rv1: Reactive[A],
                     rv2: Reactive[B],
                     rv3: Reactive[C])
                    (formatter: (A, B, C) => String): (HTMLElement, AddMethod) => TextComponentController = {
    apply(_, _, rv1, rv2, rv3, formatter)
  }


}
