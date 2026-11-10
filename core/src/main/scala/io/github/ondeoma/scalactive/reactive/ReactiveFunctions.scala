package io.github.ondeoma.scalactive.reactive

import io.github.ondeoma.scalactive.models.WatchInfo
import io.github.ondeoma.scalactive.reactive.Reactive.registerScoped

/**
 * watchの登録. `stop()` で監視をやめます.
 * コンポーネントの中で作った場合は、コンポーネントが片付けられたときに自動で止まります.
 */
final class WatchHandle private[reactive](info: WatchInfo) extends Reactive[Unit] {

  private var stopped = false

  override def v: Unit = ()

  def isStopped: Boolean = stopped

  def stop(): Unit = abort()

  override def abort(): Unit = {
    if (!stopped) {
      stopped = true
      info.abort()
    }
    super.abort()
  }

}

object WatchHandle {

  private[reactive] def start[A](r: Reactive[A],
                                 immediate: Boolean,
                                 f: (A, A) => Unit): WatchHandle = {
    val h = new WatchHandle(r.addWatcher { case (o, n) => Tracking.untracked(f(o, n)) })
    if (immediate) {
      val now = Tracking.untracked(r.v)
      Tracking.untracked(f(now, now))
    }
    h
  }

}

/** computed・watch・untracked(`import ComponentManager.*` で使えます) */
object ReactiveFunctions {

  /**
   * 計算の中で読んだリアクティブ値(`.v`)を自動で監視するCRVを作ります(Vueのcomputed).
   * 例: `val total = computed { price.v * count.v }`
   *
   * 依存は計算し直すたびに集め直します. 計算し直した値が前の値と等しい(==)場合は通知しません.
   * コンポーネントの中で作った場合は、コンポーネントが片付けられたときに監視を解除します.
   * 計算の中では、リアクティブ値を書き換えないでください.
   */
  inline def computed[R](f: => R): CRV[R] = {
    val c = Computed.make(() => f)
    registerScoped(c)
    c
  }

  /**
   * bodyの中で読んだリアクティブ値を、computedの依存に入れません.
   * 例: `computed { if (enabled.v) untracked(log.v) else Nil }` はenabledの変化だけで計算し直します.
   */
  def untracked[T](body: => T): T = Tracking.untracked(body)

  /**
   * リアクティブ値が変わったときに f(前の値, 新しい値) を実行します(Vueのwatch).
   * 値が等しい(==)代入では実行しません. 複数の値を見たい場合は `watch(computed { (a.v, b.v) })` のように書きます.
   *
   * コンポーネントの中で登録した場合は、コンポーネントが片付けられたときに自動で解除されます.
   * それ以外の場所(initStateなど)では、戻り値の `stop()` で解除してください.
   *
   * @param immediate trueの場合は、登録したときにも1回実行します(前の値と新しい値には、どちらも今の値を渡します)
   */
  inline def watch[A](r: Reactive[A],
                      immediate: Boolean = false)
                     (f: (A, A) => Unit): WatchHandle = {
    val h = WatchHandle.start(r, immediate, f)
    registerScoped(h)
    h
  }

}
