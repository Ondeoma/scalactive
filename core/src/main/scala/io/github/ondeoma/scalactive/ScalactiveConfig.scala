package io.github.ondeoma.scalactive

import scala.concurrent.duration.*

object ScalactiveConfig {

  /**
   * 連続的にコンポーネントがリロードされるのを防ぐため、
   * 以下時間分遅延させ、後発リロード処理を優先し、
   * 先発分は無効化されます。
   */
  var reloadTimerLazyDuration: FiniteDuration = 100.millis

  /**
   * コンポーネントをリロードする際に、
   * 画面のガタツキを防ぐため、元のスクロール位置を維持する機能を、
   * 有効にする場合はtrue。
   */
  var keepWindowScrollPositionWhenReload: Boolean = true
  
}
