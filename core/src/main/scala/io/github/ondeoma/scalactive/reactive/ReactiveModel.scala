package io.github.ondeoma.scalactive.reactive

import io.github.ondeoma.scalactive.reactive.RMCompatible.ext.toRM
import io.github.ondeoma.scalactive.utils.TypeAlias.WatchInfos

trait ReactiveModel[Org, Self <: ReactiveModel[Org, Self]] extends Product {
  self: Self =>

  type O = Org
  type S = Self

  val reactives: List[Reactive[?]]

  val reactiveModels: List[ReactiveModel[?, ?]]

  def addWatcher(f: () => Unit): WatchInfos = {
    reactives.map(_.addWatcher(_ => f())) :::
      reactiveModels.flatMap(_.addWatcher(() => f()))
  }

  def reload(org: Org): Unit

  def reload(other: Self): Unit = {
    reload(other.toOrigin)
  }

  def toOrigin: Org

  def v: Org = toOrigin

  def clone(using rmc: RMCompatible[Org, Self]): Self = {
    toOrigin.toRM
  }

}
