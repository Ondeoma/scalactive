package io.github.ondeoma.scalactive.components.fors

import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager}
import io.github.ondeoma.scalactive.controllers.NodesComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.{RMList, ReactiveModel}
import org.scalajs.dom.*

object ForRMComponent extends BaseComponent {

  def apply[A, M <: ReactiveModel[A, M]](root: HTMLElement,
               am: AddMethod,
               values: RMList[A, M])
              (genHtml: (ComponentManager, M, IDX) => HTML): NodesComponentController = {
    apply(root, am, values, true)(genHtml)
  }

  def apply[A, M <: ReactiveModel[A, M]](root: HTMLElement,
                                         am: AddMethod,
                                         values: RMList[A, M],
                                         usesIndex: Boolean)
                                        (genHtml: (ComponentManager, M, IDX) => HTML): NodesComponentController = {
    ForGeneralComponent(root, am, values, _.rv, c => List(values.addWatcherRowLevel(_ => c.reload())), usesIndex = usesIndex)(genHtml)
  }

  /** keyで行を対応付ける. キーが同じでも別のReactiveModelになった行は作り直す */
  def apply[A, M <: ReactiveModel[A, M]](root: HTMLElement,
                                         am: AddMethod,
                                         values: RMList[A, M],
                                         usesIndex: Boolean,
                                         key: M => Any)
                                        (genHtml: (ComponentManager, M, IDX) => HTML): NodesComponentController = {
    ForGeneralComponent(root, am, values, _.rv, c => List(values.addWatcherRowLevel(_ => c.reload())),
      keyOf = key, snapshotOf = (m: M) => m, usesIndex = usesIndex)(genHtml)
  }

  def apply[A, M <: ReactiveModel[A, M]](vs: RMList[A, M],
                                         usesIndex: Boolean,
                                         key: M => Any)
                                        (genHtml: (ComponentManager, M, IDX) => HTML): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, vs, usesIndex, key)(genHtml)
  }

  def apply[A, M <: ReactiveModel[A, M]](vs: RMList[A, M])
                                        (genHtml: (ComponentManager, M, IDX) => HTML): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, vs, true)(genHtml)
  }

  def apply[A, M <: ReactiveModel[A, M]](vs: RMList[A, M],
                                         usesIndex: Boolean)
                                        (genHtml: (ComponentManager, M, IDX) => HTML): (HTMLElement, AddMethod) => NodesComponentController = {
    apply(_, _, vs, usesIndex)(genHtml)
  }

}
