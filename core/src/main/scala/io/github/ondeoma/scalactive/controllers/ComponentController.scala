package io.github.ondeoma.scalactive.controllers

import io.github.ondeoma.scalactive.ScalactiveConfig
import org.scalajs.dom.window

trait ComponentController[CC <: ComponentController[?]] {

  def init(): Either[String, CC]

  def clear(): Unit

  private var scrollX: Double = 0
  private var scrollY: Double = 0

  /**
   * 描き直し(clear + init)を予約します. 予約はReloadSchedulerがまとめて実行します.
   * (ScalactiveConfig.reloadTimerLazyDuration が0なら現在の処理の直後、それより大きければその時間の後)
   */
  def reload(): Unit = {
    ReloadScheduler.schedule(this)
  }

  /**
   * 予約済みのreloadを取り消します.
   * clearの後(親コンポーネントに片付けられた後など)に遅れてreloadが走ると、
   * 監視だけが登録されて解除されないままになるため、clearで呼びます.
   */
  protected def cancelReload(): Unit = {
    ReloadScheduler.cancel(this)
  }

  /** DOM上の深さ. 親子の両方の描き直しが予約されたとき、親を先に描き直すために使います. */
  private[scalactive] def depth: Int = 0

  def backupScrollPosition(): Unit = {
    if (ScalactiveConfig.keepWindowScrollPositionWhenReload) {
      scrollX = window.scrollX
      scrollY = window.scrollY
    }
  }

  def restoreScrollPosition(): Unit = {
    if (ScalactiveConfig.keepWindowScrollPositionWhenReload) {
      window.scrollTo(scrollX.toInt, scrollY.toInt)
    }
  }

}
