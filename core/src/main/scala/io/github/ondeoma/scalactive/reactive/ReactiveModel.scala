package io.github.ondeoma.scalactive.reactive

import io.github.ondeoma.scalactive.ScalactiveConfig
import io.github.ondeoma.scalactive.reactive.RMCompatible.ext.toRM
import io.github.ondeoma.scalactive.utils.TypeAlias.WatchInfos
import org.scalajs.dom.window

import scala.scalajs.js

trait ReactiveModel[Org, Self <: ReactiveModel[Org, Self]] extends Reactive[Org] with Product {
  self: Self =>

  type O = Org
  type S = Self

  val reactives: List[Reactive[?]]
  val reactiveModels: List[ReactiveModel[?, ?]]

  private var old: Org = v

  private lazy val watchInfos: WatchInfos = {
    reactives.map(_.addWatcher(_ => callWatchers())) :::
      reactiveModels.map(_.addWatcher((_, _) => callWatchers()))
  }

  protected var reloadTimer = 0

  /** フィールドの変更をまとめて通知します(ScalactiveConfig.reloadTimerLazyDurationが0なら現在の処理の直後) */
  private var notifyQueued = false

  private def notifyWatchers(): Unit = {
    notifyQueued = false
    watchers.foreach(_._2(old, v))
    old = v
  }

  private def callWatchers(): Unit = {
    if (reloadTimer != 0) {
      window.clearTimeout(reloadTimer)
      reloadTimer = 0
    }
    val delay = ScalactiveConfig.reloadTimerLazyDuration.toMillis.toDouble
    if (delay <= 0) {
      if (!notifyQueued) {
        notifyQueued = true
        js.Dynamic.global.queueMicrotask(() => if (notifyQueued) notifyWatchers())
      }
    } else {
      notifyQueued = false
      reloadTimer = window.setTimeout(() => {
        reloadTimer = 0
        notifyWatchers()
      }, delay)
    }
  }

  def reload(org: Org): Unit

  def reload(other: Self): Unit = {
    reload(other.toOrigin)
  }

  def :=(org: Org): Unit = {
    reload(org)
  }

  def :=(other: Self): Unit = {
    reload(other)
  }

  def toOrigin: Org

  def v: Org = toOrigin

  def clone(using rmc: RMCompatible[Org, Self]): Self = {
    toOrigin.toRM
  }

  override def abort(): Unit = {
    watchInfos.foreach(_.abort())
    watchers.clear()
  }

  // サブタイプ側で初期化されるreactives/reactiveModelsを待ってから、
  // watchInfosを割り当てるための苦しい実装.
  def init(): Unit = {
    window.setTimeout(() => {
      Option(reactives).fold(init())(_ => watchInfos)
    }, 100)
  }

  init()

}
