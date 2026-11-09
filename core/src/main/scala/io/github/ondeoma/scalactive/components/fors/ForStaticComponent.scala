package io.github.ondeoma.scalactive.components.fors

import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager}
import io.github.ondeoma.scalactive.controllers.NodesComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import org.scalajs.dom.*


object ForStaticComponent extends BaseComponent {

  def apply[A](root: HTMLElement,
               am: AddMethod,
               values: List[A])
              (genHtml: (ComponentManager, A, IDX) => HTML): NodesComponentController = {
    apply(root, am, values, true)(genHtml)
  }

  def apply[A](root: HTMLElement,
               am: AddMethod,
               values: List[A],
               usesIndex: Boolean)
              (genHtml: (ComponentManager, A, IDX) => HTML): NodesComponentController = {
    ForGeneralComponent(root, am, values, identity, _ => Nil, usesIndex = usesIndex)(genHtml)
  }

  /** keyで行を対応付ける. キーが同じでも値が変わった行は作り直す */
  def apply[A](root: HTMLElement,
               am: AddMethod,
               values: List[A],
               usesIndex: Boolean,
               key: A => Any)
              (genHtml: (ComponentManager, A, IDX) => HTML): NodesComponentController = {
    ForGeneralComponent(root, am, values, identity, _ => Nil, keyOf = key, snapshotOf = (a: A) => a, usesIndex = usesIndex)(genHtml)
  }

  def apply[A](values: List[A],
               usesIndex: Boolean,
               key: A => Any)
              (genHtml: (ComponentManager, A, IDX) => HTML): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, values, usesIndex, key)(genHtml)
  }

  def apply[A](values: List[A])
              (genHtml: (ComponentManager, A, IDX) => HTML): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, values, true)(genHtml)
  }

  def apply[A](values: List[A],
               usesIndex: Boolean)
              (genHtml: (ComponentManager, A, IDX) => HTML): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, values, usesIndex)(genHtml)
  }

}
