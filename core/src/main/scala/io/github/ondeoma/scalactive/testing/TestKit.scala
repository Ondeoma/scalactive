package io.github.ondeoma.scalactive.testing

import io.github.ondeoma.scalactive.ScalactiveConfig
import io.github.ondeoma.scalactive.components.{ComponentManager, StatelessComponent}
import io.github.ondeoma.scalactive.controllers.NodesComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.utils.TypeAlias.{GenComponentF, HTML}
import org.scalajs.dom.*

import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.scalajs.js

/**
 * コンポーネントのテスト用のヘルパー(テストフレームワークには依存しません).
 * jsdom(scalajs-env-jsdom-nodejs)やブラウザ上のテストで使えます.
 *
 * {{{
 * TestKit.useTestConfig()
 * val m = TestKit.mount(CounterComponent()(_, _))
 * TestKit.click(m.find("button"))
 * TestKit.flush().map(_ => assertEquals(m.find("p").textContent, "1"))
 * }}}
 */
object TestKit {

  /** 描画したコンポーネント */
  final class Mounted(val root: HTMLElement,
                      val controller: NodesComponentController) {

    /** root以下でselectorに一致する最初の要素(なければ例外) */
    def find(selector: String): HTMLElement = {
      Option(root.querySelector(selector)).map(_.asInstanceOf[HTMLElement])
        .getOrElse(throw new NoSuchElementException(s"'$selector' was not found in: ${root.innerHTML}"))
    }

    /** root以下でselectorに一致するすべての要素 */
    def findAll(selector: String): List[HTMLElement] = {
      val nl = root.querySelectorAll(selector)
      List.tabulate(nl.length)(i => nl(i).asInstanceOf[HTMLElement])
    }

    def text: String = root.textContent

    def html: String = root.innerHTML

    /** 片付けて(onUnmountedなどを実行して)、rootを取り除く */
    def unmount(): Unit = {
      controller.clear()
      root.remove()
    }

  }

  private var saved: Option[(FiniteDuration, Boolean, Boolean)] = None

  /**
   * テスト向けの設定にします.
   *  - 描き直しを待たない(reloadTimerLazyDuration = 0)
   *  - スクロール位置の保存・復元とRouter.goでのスクロールをしない(jsdomはwindow.scrollToを実装していない)
   * restoreConfigで元に戻します.
   */
  def useTestConfig(): Unit = {
    if (saved.isEmpty) {
      saved = Some((ScalactiveConfig.reloadTimerLazyDuration, ScalactiveConfig.keepWindowScrollPositionWhenReload,
        ScalactiveConfig.scrollToTopOnNavigate))
    }
    ScalactiveConfig.reloadTimerLazyDuration = 0.millis
    ScalactiveConfig.keepWindowScrollPositionWhenReload = false
    ScalactiveConfig.scrollToTopOnNavigate = false
  }

  def restoreConfig(): Unit = {
    saved.foreach { (d, k, s) =>
      ScalactiveConfig.reloadTimerLazyDuration = d
      ScalactiveConfig.keepWindowScrollPositionWhenReload = k
      ScalactiveConfig.scrollToTopOnNavigate = s
    }
    saved = None
  }

  /** コンポーネントをdocument.bodyの下の新しいdivに描画します. */
  def mount(component: GenComponentF): Mounted = {
    val root = document.createElement("div").asInstanceOf[HTMLElement]
    document.body.appendChild(root)
    val wrapper = new StatelessComponent {
      def genHtml(implicit cm: ComponentManager): HTML = c(component)
    }
    wrapper.apply()(root, AddMethod.append(root)).init() match {
      case Right(c) => new Mounted(root, c)
      case Left(e) =>
        root.remove()
        throw new IllegalStateException(s"failed to mount: $e")
    }
  }

  /** HTMLを返す関数を、その場で作ったコンポーネントとして描画します. */
  def mountHtml(gen: ComponentManager => HTML): Mounted = {
    mount(new StatelessComponent {
      def genHtml(implicit cm: ComponentManager): HTML = gen(cm)
    }.apply()(_, _))
  }

  /**
   * 予約された描き直しとonMountedが終わるまで待ちます.
   * 描き直しの中でさらに予約されたものも待つため、マイクロタスクを数回待ってから、タイマー1回分待ちます.
   * (reloadTimerLazyDurationを0より大きくしている場合は、その時間も待ちます)
   */
  def flush()(using ec: ExecutionContext): Future[Unit] = {
    def micro(n: Int): Future[Unit] = {
      if (n == 0) Future.successful(())
      else js.Promise.resolve[Unit](()).toFuture.flatMap(_ => micro(n - 1))
    }
    micro(5).flatMap(_ => sleep(ScalactiveConfig.reloadTimerLazyDuration.toMillis.toDouble)).flatMap(_ => micro(5))
  }

  /** 指定したミリ秒だけ待ちます. */
  def sleep(ms: Double): Future[Unit] = {
    val p = Promise[Unit]()
    window.setTimeout(() => p.success(()), ms)
    p.future
  }

  // ---- 操作 ----

  def click(e: Element): Unit = {
    e.dispatchEvent(new MouseEvent("click", new MouseEventInit {
      bubbles = true
      cancelable = true
    }))
  }

  /** 入力欄の値を変えて、input(lazyならchange)イベントを送ります(v-modelの反映). */
  def input(e: Element, value: String, change: Boolean = false): Unit = {
    e.asInstanceOf[js.Dynamic].value = value
    e.dispatchEvent(new Event(if (change) "change" else "input", new EventInit {
      bubbles = true
    }))
  }

  /** チェックボックス・ラジオボタンの状態を変えて、changeイベントを送ります. */
  def check(e: Element, checked: Boolean = true): Unit = {
    e.asInstanceOf[js.Dynamic].checked = checked
    e.dispatchEvent(new Event("change", new EventInit {
      bubbles = true
    }))
  }

  def keydown(e: Element, key: String, ctrl: Boolean = false, shift: Boolean = false): Unit = {
    val k = key
    e.dispatchEvent(new KeyboardEvent("keydown", new KeyboardEventInit {
      this.key = k
      ctrlKey = ctrl
      shiftKey = shift
      bubbles = true
    }))
  }

}
