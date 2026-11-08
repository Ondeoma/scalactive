package io.github.ondeoma.scalactive.components.fors

import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager}
import io.github.ondeoma.scalactive.controllers.NodesComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.Reactive
import org.scalajs.dom.*


object ForLRComponent extends BaseComponent {

  def apply[A](root: HTMLElement,
               am: AddMethod,
               values: Reactive[List[A]])
              (genHtml: (ComponentManager, A, IDX) => HTML): NodesComponentController = {
    apply(root, am, values, true)(genHtml)
  }

  def apply[A](root: HTMLElement,
               am: AddMethod,
               values: Reactive[List[A]],
               usesIndex: Boolean)
              (genHtml: (ComponentManager, A, IDX) => HTML): NodesComponentController = {
    ForGeneralComponent(root, am, values, _.v, c => List(values.addWatcher(_ => c.reload())), usesIndex = usesIndex)(genHtml)
  }

  /** keyで行を対応付ける. キーが同じでも値が変わった行は作り直す */
  def apply[A](root: HTMLElement,
               am: AddMethod,
               values: Reactive[List[A]],
               usesIndex: Boolean,
               key: A => Any)
              (genHtml: (ComponentManager, A, IDX) => HTML): NodesComponentController = {
    ForGeneralComponent(root, am, values, _.v, c => List(values.addWatcher(_ => c.reload())),
      keyOf = key, snapshotOf = (a: A) => a, usesIndex = usesIndex)(genHtml)
  }

  def apply[A](values: Reactive[List[A]],
               usesIndex: Boolean,
               key: A => Any)
              (genHtml: (ComponentManager, A, IDX) => HTML): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, values, usesIndex, key)(genHtml)
  }

  def apply[A](values: Reactive[List[A]])
              (genHtml: (ComponentManager, A, IDX) => HTML): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, values, true)(genHtml)
  }

  def apply[A](values: Reactive[List[A]],
               usesIndex: Boolean)
              (genHtml: (ComponentManager, A, IDX) => HTML): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, values, usesIndex)(genHtml)
  }

}
