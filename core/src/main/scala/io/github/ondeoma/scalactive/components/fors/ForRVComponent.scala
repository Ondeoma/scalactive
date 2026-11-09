package io.github.ondeoma.scalactive.components.fors

import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager}
import io.github.ondeoma.scalactive.controllers.NodesComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.{RVList, RV}
import org.scalajs.dom.*


object ForRVComponent extends BaseComponent {

  def apply[A](root: HTMLElement,
               am: AddMethod,
               values: RVList[A])
              (genHtml: (ComponentManager, RV[A], IDX) => HTML): NodesComponentController = {
    apply(root, am, values, true)(genHtml)
  }

  def apply[A](root: HTMLElement,
               am: AddMethod,
               values: RVList[A],
               usesIndex: Boolean)
              (genHtml: (ComponentManager, RV[A], IDX) => HTML): NodesComponentController = {
    // 行はRVの同一性で対応付け、代入で値が書き換わった行は作り直す
    ForGeneralComponent(root, am, values, _.rv, c => List(values.addWatcherRowLevel(_ => c.reload())),
      snapshotOf = (rv: RV[A]) => rv.v, usesIndex = usesIndex)(genHtml)
  }

  /** keyで行を対応付ける. キーが同じでも、別のRVになった行・値が変わった行は作り直す */
  def apply[A](root: HTMLElement,
               am: AddMethod,
               values: RVList[A],
               usesIndex: Boolean,
               key: RV[A] => Any)
              (genHtml: (ComponentManager, RV[A], IDX) => HTML): NodesComponentController = {
    ForGeneralComponent(root, am, values, _.rv, c => List(values.addWatcherRowLevel(_ => c.reload())),
      keyOf = key, snapshotOf = (rv: RV[A]) => (rv, rv.v), usesIndex = usesIndex)(genHtml)
  }

  def apply[A](values: RVList[A],
               usesIndex: Boolean,
               key: RV[A] => Any)
              (genHtml: (ComponentManager, RV[A], IDX) => HTML): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, values, usesIndex, key)(genHtml)
  }

  def apply[A](values: RVList[A])
              (genHtml: (ComponentManager, RV[A], IDX) => HTML): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, values, true)(genHtml)
  }

  def apply[A](values: RVList[A],
               usesIndex: Boolean)
              (genHtml: (ComponentManager, RV[A], IDX) => HTML): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, values, usesIndex)(genHtml)
  }

}
