package io.github.ondeoma.scalactive.reactive

import io.github.ondeoma.scalactive.ScalactiveConfig
import io.github.ondeoma.scalactive.reactive.RMCompatible.ext.toRM
import io.github.ondeoma.scalactive.models.WatchInfo
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

  /**
   * フィールドの監視. 最初にこのモデルが監視されたときに登録します.
   * (トレイトの本体の実行時点では、サブクラスのreactives/reactiveModelsがまだ初期化されていないため)
   * 監視されるまではフィールドが変わっても知らせる相手がいないので、遅れて登録しても取りこぼしません.
   */
  private var watchInfos: Option[WatchInfos] = None

  private def subscribe(): Unit = {
    if (watchInfos.isEmpty && reactives != null && reactiveModels != null) {
      old = v
      watchInfos = Some(
        reactives.map(_.addWatcher(_ => callWatchers())) :::
          reactiveModels.map(_.addWatcher((_, _) => callWatchers()))
      )
    }
  }

  override def addWatcher(w: Watcher): WatchInfo = {
    subscribe()
    super.addWatcher(w)
  }

  protected var reloadTimer = 0

  /** フィールドの変更をまとめて通知します(ScalactiveConfig.reloadTimerLazyDurationが0なら現在の処理の直後) */
  private var notifyQueued = false

  private def notifyWatchers(): Unit = {
    notifyQueued = false
    fire(old, v)
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

  // 項目のリアクティブ値は依存に入れない(このモデルが項目の変化も通知するため)
  def v: Org = {
    Tracking.record(this)
    Tracking.untracked(toOrigin)
  }

  def clone(using rmc: RMCompatible[Org, Self]): Self = {
    toOrigin.toRM
  }

  override def abort(): Unit = {
    watchInfos.foreach(_.foreach(_.abort()))
    watchInfos = None
    watchers.clear()
  }

  /** フィールドの監視を今すぐ登録します(通常は最初の監視のときに自動で登録されるため、呼ぶ必要はありません) */
  def init(): Unit = subscribe()

}
