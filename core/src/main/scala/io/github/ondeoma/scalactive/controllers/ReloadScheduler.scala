package io.github.ondeoma.scalactive.controllers

import io.github.ondeoma.scalactive.ScalactiveConfig
import io.github.ondeoma.scalactive.components.ErrorBoundaryContext
import org.scalajs.dom.window

import scala.collection.mutable
import scala.scalajs.js

/**
 * コンポーネントの描き直し(reload)を予約し、まとめて実行します.
 *
 *  - ScalactiveConfig.reloadTimerLazyDuration が0(既定)なら、現在の処理(イベントハンドラーなど)が終わった直後
 *    (マイクロタスク)に描き直します. 同じ処理の中での複数の変更は1回の描き直しにまとまります.
 *  - 0より大きければ、最後の予約からその時間だけ待ってから描き直します(入力中の連続した変更をまとめたいときなど).
 *  - 親と子の両方が予約されていたら親から描き直します. 親の描き直しで片付けられた子は描き直しません.
 */
private[scalactive] object ReloadScheduler {

  /** 予約されたコントローラー(予約順) */
  private val pending = mutable.LinkedHashSet[ComponentController[?]]()
  private var microtaskQueued = false
  private var timer = 0

  def schedule(c: ComponentController[?]): Unit = {
    pending.addOne(c)
    val delay = ScalactiveConfig.reloadTimerLazyDuration.toMillis.toDouble
    if (delay <= 0) {
      if (!microtaskQueued) {
        microtaskQueued = true
        js.Dynamic.global.queueMicrotask(() => {
          microtaskQueued = false
          flush()
        })
      }
    } else {
      // 予約のたびに待ち直す(以前のコンポーネントごとの待ち方と同じ)
      if (timer != 0) window.clearTimeout(timer)
      timer = window.setTimeout(() => {
        timer = 0
        flush()
      }, delay)
    }
  }

  def cancel(c: ComponentController[?]): Unit = {
    pending.remove(c)
  }

  def isPending(c: ComponentController[?]): Boolean = pending.contains(c)

  /** 予約された描き直しがなければtrue */
  def isIdle: Boolean = pending.isEmpty

  private def flush(): Unit = {
    if (pending.nonEmpty) {
      // 親(DOMの浅い位置)から描き直す. 深さは描き直しの前に計算しておく
      val ordered = pending.toSeq.zipWithIndex.sortBy { (c, i) => (c.depth, i) }.map(_._1)
      val keepScroll = ScalactiveConfig.keepWindowScrollPositionWhenReload
      val (x, y) = (window.scrollX, window.scrollY)
      ordered.foreach { c =>
        // 先に描き直した親に片付けられた(cancelされた)ものは飛ばす
        if (pending.contains(c)) {
          pending.remove(c)
          // 描き直しの中で作られるコントローラーも同じ境界を覚える. 例外は境界(なければコンソール)へ
          ErrorBoundaryContext.guard(c.boundary)(io.github.ondeoma.scalactive.Devtools.measure(c)(c.refresh()))
        }
      }
      if (keepScroll) window.scrollTo(x.toInt, y.toInt)
    }
    NextTick.check()
  }

}
