package io.github.ondeoma.scalactive.components.texts

import cats.syntax.all.*
import io.github.ondeoma.scalactive.components.BaseComponent
import org.scalajs.dom.HTMLElement

object TextComponent extends BaseComponent {

  def apply[A](root: HTMLElement,
               m: AddMethod,
               rv1: Reactive[A],
               formatter: A => String = (a: A) => a.toString): TextComponentController = {
    TextComponentController { c =>
      val format = () => formatter(rv1.v)
      addText(root)(m, format()).map { text =>
        val replace = () => text.data = format()
        val wis = List(
          rv1.addWatcher(_ => replace()),
        )
        c.text = text.some
        c.watchInfos = wis
        c
      }.toRight(genErrorMessage)
    }
  }

  def apply[A](rv1: Reactive[A])
              (formatter: A => String): (HTMLElement, AddMethod) => TextComponentController = {
    apply(_, _, rv1, formatter)
  }

}
