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

  /**
   * Router.goで遷移する時/RouterComponentのベースパス
   */
  var basePath: String = ""

  /**
   * basePathを以下のように整形した値
   * 絶対パスの場合は末尾にスラッシュ付与.
   * 相対パスの場合は先頭と末尾にスラッシュ付与.
   */
  def fixedBasePath: String = {
    val p = basePath
    if (p.isEmpty) ""
    else if (p.startsWith("http")) {
      if (!p.endsWith("/")) s"$p/" else p
    } else s"/${p.replaceAll("^/", "").replaceAll("/$", "")}"
  }

}
