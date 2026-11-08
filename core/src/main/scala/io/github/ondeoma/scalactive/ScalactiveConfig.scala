package io.github.ondeoma.scalactive

import scala.concurrent.duration.*

object ScalactiveConfig {

  /**
   * コンポーネントの描き直し(リロード)を待つ時間.
   *
   * 0(既定)の場合、現在の処理(イベントハンドラーなど)が終わった直後にまとめて描き直します.
   * 同じ処理の中での複数の変更は、1回の描き直しにまとまります.
   *
   * 0より大きい場合、最後の変更からこの時間だけ待ってから描き直します.
   * 文字入力のたびに大きなリストを描き直すのを避けたい場合などに指定してください.
   * (v0.1.xまでの既定値は100msでした)
   */
  var reloadTimerLazyDuration: FiniteDuration = 0.millis

  /**
   * コンポーネントをリロードする際に、
   * 画面のガタツキを防ぐため、元のスクロール位置を維持する機能を、
   * 有効にする場合はtrue。
   */
  var keepWindowScrollPositionWhenReload: Boolean = true

  /**
   * Router.goで遷移したときに、ウィンドウを先頭までスクロールする場合はtrue.
   */
  var scrollToTopOnNavigate: Boolean = true

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
