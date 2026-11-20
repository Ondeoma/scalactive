package io.github.ondeoma.scalactive.effects

import io.github.ondeoma.scalactive.reactive.Reactive

/**
 * 登録したイベントリスナー・タイマーなどの後片付けをするハンドル.
 * コンポーネントの中(描画中・onMounted・.st の def mounted)で作った場合は、コンポーネントが片付けられたときに自動で止まります.
 * それ以外の場所で作った場合は `stop()` で止めてください.
 */
class EffectHandle private[scalactive](private var stopF: () => Unit) extends Reactive[Unit] {

  private var stopped = false

  override def v: Unit = ()

  def isStopped: Boolean = stopped

  def stop(): Unit = abort()

  override def abort(): Unit = {
    if (!stopped) {
      stopped = true
      val f = stopF
      stopF = () => ()
      f()
    }
    super.abort()
  }

}
