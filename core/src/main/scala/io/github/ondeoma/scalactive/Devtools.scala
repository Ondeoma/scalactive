package io.github.ondeoma.scalactive

import io.github.ondeoma.scalactive.controllers.{ComponentController, NodesController}
import io.github.ondeoma.scalactive.reactive.Reactive
import io.github.ondeoma.scalactive.utils.ConsoleUtil
import org.scalajs.dom.{HTMLElement, window}

import scala.scalajs.js

/**
 * 開発用の診断機能.
 *
 *  - logReloads: 描き直し(ifC・forC・リアクティブな値の変化など)のたびに、対象と所要時間をコンソールに出力します
 *  - highlightUpdates: 描き直した部分を一瞬枠で囲みます(どこが描き直されているかを目で確かめる)
 *  - stats: 表示中のコンポーネントの数、描き直しの回数. 画面を行き来しても表示中の数が増え続ける場合は解除漏れの疑いがあります
 *  - watcherCount: リアクティブな値を監視している数
 *
 * install()を呼ぶと、ブラウザのコンソールから `scalactive.stats()` などで使えます.
 */
object Devtools {

  /** 描き直しのたびにコンソールに出力する */
  var logReloads: Boolean = false

  /** 描き直した部分を一瞬枠で囲む */
  var highlightUpdates: Boolean = false

  final case class Stats(liveComponents: Int, reloads: Int)

  private var live = 0
  private var reloads = 0

  def stats: Stats = Stats(live, reloads)

  /** 描き直しの回数を0に戻します */
  def resetStats(): Unit = reloads = 0

  /** リアクティブな値を監視している数(ウォッチャーの数) */
  def watcherCount(r: Reactive[?]): Int = r.numWatchers

  private[scalactive] def componentMounted(): Unit = live += 1

  private[scalactive] def componentCleared(): Unit = live -= 1

  /** ReloadSchedulerが描き直しのたびに呼びます */
  private[scalactive] def measure(c: ComponentController[?])(f: => Unit): Unit = {
    reloads += 1
    if (!logReloads && !highlightUpdates) f
    else {
      val t0 = window.performance.now()
      f
      val ms = window.performance.now() - t0
      if (logReloads) ConsoleUtil.debug(f"[scalactive] reload ${c.getClass.getSimpleName} ${ms}%.2fms")
      if (highlightUpdates) highlight(c)
    }
  }

  private def highlight(c: ComponentController[?]): Unit = {
    c match {
      case n: NodesController[?] =>
        n.elements.foreach { (e: HTMLElement) =>
          val prev = e.style.outline
          e.style.outline = "2px solid rgba(255, 99, 71, 0.8)"
          window.setTimeout(() => e.style.outline = prev, 400)
        }
      case _ => ()
    }
  }

  /**
   * ブラウザのコンソールから使えるようにします(window.scalactive).
   * 例: `scalactive.stats()`, `scalactive.logReloads(true)`, `scalactive.highlightUpdates(true)`
   */
  def install(): Unit = {
    window.asInstanceOf[js.Dynamic].scalactive = js.Dynamic.literal(
      stats = () => js.Dynamic.literal(liveComponents = live, reloads = reloads),
      resetStats = () => resetStats(),
      logReloads = (b: Boolean) => logReloads = b,
      highlightUpdates = (b: Boolean) => highlightUpdates = b,
    )
  }

}
