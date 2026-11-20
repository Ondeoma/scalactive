package io.github.ondeoma.scalactive.effects

import io.github.ondeoma.scalactive.reactive.Reactive.registerScoped
import org.scalajs.dom.{Event, EventTarget, document, window}

import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

/**
 * 後片付けを自動で行うイベントリスナー・タイマーなど(`import ComponentManager.*` で使えます).
 *
 * どれもコンポーネントの中(描画中・onMounted・.st の def mounted)で作った場合は、
 * コンポーネントが片付けられたときに自動で止まります. それ以外の場所では戻り値の `stop()` で止めてください.
 */
object Effects {

  private[effects] def mkDebounced[A](ms: Double, f: A => Unit): DebouncedFn[A] = new DebouncedFn[A](ms, f)

  private[effects] def mkThrottled[A](ms: Double, f: A => Unit): ThrottledFn[A] = new ThrottledFn[A](ms, f)

  private[effects] def mkAsync[A](load: () => Future[A]): AsyncState[A] = new AsyncState[A](load)

  private[effects] def mkAction(): ActionState = new ActionState

  private[effects] def mkLatest(): LatestFn = new LatestFn

  // ---- イベントリスナー ----

  private[effects] def startListener[E <: Event](target: EventTarget,
                                                 tpe: String,
                                                 capture: Boolean,
                                                 passive: Boolean,
                                                 once: Boolean,
                                                 f: E => Unit): EffectHandle = {
    val listener: js.Function1[Event, Unit] = (e: Event) => f(e.asInstanceOf[E])
    val opts = js.Dynamic.literal(capture = capture, passive = passive, once = once)
    target.asInstanceOf[js.Dynamic].addEventListener(tpe, listener, opts)
    new EffectHandle(() => target.asInstanceOf[js.Dynamic].removeEventListener(tpe, listener, js.Dynamic.literal(capture = capture)))
  }

  /**
   * イベントリスナーを登録します(windowやdocumentなど、コンポーネントの外の要素にも使えます).
   * 例: `useEventListener[KeyboardEvent](window, "keydown") { e => if (e.key == "k" && e.ctrlKey) open() }`
   */
  inline def useEventListener[E <: Event](target: EventTarget,
                                          tpe: String,
                                          capture: Boolean = false,
                                          passive: Boolean = false,
                                          once: Boolean = false)
                                         (f: E => Unit): EffectHandle = {
    val h = startListener(target, tpe, capture, passive, once, f)
    registerScoped(h)
    h
  }

  // ---- タイマー ----

  private[effects] def startInterval(ms: Double, f: () => Unit): EffectHandle = {
    val id = window.setInterval(() => f(), ms)
    new EffectHandle(() => window.clearInterval(id))
  }

  private[effects] def startTimeout(ms: Double, f: () => Unit): EffectHandle = {
    var done = false
    val id = window.setTimeout(() => {
      done = true
      f()
    }, ms)
    new EffectHandle(() => if (!done) window.clearTimeout(id))
  }

  /** msミリ秒ごとにfを実行します(setInterval). */
  inline def useInterval(ms: Double)(f: => Unit): EffectHandle = {
    val h = startInterval(ms, () => f)
    registerScoped(h)
    h
  }

  /** msミリ秒後にfを1回実行します(setTimeout). 実行前に止めると実行しません. */
  inline def useTimeout(ms: Double)(f: => Unit): EffectHandle = {
    val h = startTimeout(ms, () => f)
    registerScoped(h)
    h
  }

  private[effects] def startVisiblePolling(ms: Double, immediate: Boolean, f: () => Unit): EffectHandle = {
    def visible: Boolean = document.visibilityState.asInstanceOf[String] == "visible"
    var timer: Option[Int] = None
    def start(): Unit = if (timer.isEmpty) timer = Some(window.setInterval(() => if (visible) f(), ms))
    def stop(): Unit = {
      timer.foreach(window.clearInterval)
      timer = None
    }
    val onVisibility: js.Function1[Event, Unit] = _ => {
      if (visible) {
        // 隠れている間に古くなった表示を、戻ってきたときにすぐ読み直す
        f()
        start()
      } else stop()
    }
    document.addEventListener("visibilitychange", onVisibility)
    if (visible) start()
    if (immediate && visible) f()
    new EffectHandle(() => {
      stop()
      document.removeEventListener("visibilitychange", onVisibility)
    })
  }

  /**
   * タブが表示されている間だけ、msミリ秒ごとにfを実行します(一覧の定期的な読み直しなど).
   * タブが隠れている間は止め、再び表示されたときにすぐ1回実行してから再開します.
   *
   * @param immediate trueなら、登録したときにも1回実行します
   */
  inline def useVisiblePolling(ms: Double, immediate: Boolean = false)(f: => Unit): EffectHandle = {
    val h = startVisiblePolling(ms, immediate, () => f)
    registerScoped(h)
    h
  }

  // ---- デバウンス・スロットル ----

  /**
   * 最後の呼び出しからmsミリ秒たってからfを実行します(入力中の検索など).
   * 例: `val search = useDebounce[String](300)(q => load(q))` → `@input="search(s.q.v)"`
   */
  inline def useDebounce[A](ms: Double)(f: A => Unit): DebouncedFn[A] = {
    val d = mkDebounced(ms, f)
    registerScoped(d)
    d
  }

  /**
   * fを、msミリ秒に1回までに間引いて実行します(スクロール・リサイズなど).
   * 最初の呼び出しはすぐに実行し、間引いた間の最後の呼び出しは、間隔があいた後に実行します.
   */
  inline def useThrottle[A](ms: Double)(f: A => Unit): ThrottledFn[A] = {
    val t = mkThrottled(ms, f)
    registerScoped(t)
    t
  }

  // ---- 非同期の状態 ----

  /**
   * 非同期に読み込む値の状態(読み込み中・エラー・値)を作ります.
   * 読み直し中も前の値を表示し続けます. 後から読み直した場合、古い応答は捨てます.
   *
   * {{{
   * case class State(items: AsyncState[List[Item]] = useAsync(Api.items()))
   * // <p v-if="s.items.loading">読み込み中…</p>
   * // <p v-if="s.items.failed" class="error">{{ s.items.errorMessage }}</p>
   * // <li v-for="i in s.items.getOrElse(Nil)">{{ i.name }}</li>
   * }}}
   *
   * @param immediate trueなら、作ったときに読み込みを始めます(falseなら reload() を呼ぶまで読み込みません)
   */
  inline def useAsync[A](load: => Future[A], immediate: Boolean = true): AsyncState[A] = {
    val a = mkAsync[A](() => load)
    registerScoped(a)
    if (immediate) a.reload()
    a
  }

  /**
   * 実行中は二重に実行できない操作(保存・送信など)の状態を作ります.
   *
   * {{{
   * case class State(save: ActionState = useAction())
   * def submit(s: State) = s.save.run(Api.save(...)).foreach(_ => toast("保存しました"))
   * // <button :disabled="s.save.busy" @click="submit(s)">保存</button>
   * // <p v-if="s.save.failed" class="error">{{ s.save.errorMessage }}</p>
   * }}}
   */
  inline def useAction(): ActionState = {
    val a = mkAction()
    registerScoped(a)
    a
  }

  /**
   * 最後に始めた非同期の処理の結果だけを受け取ります(古い応答を捨てる).
   * 例: `val latest = useLatest()` → `latest(Api.search(q)).foreach(r => s.items := r)`
   */
  inline def useLatest(): LatestFn = {
    val l = mkLatest()
    registerScoped(l)
    l
  }

}

/** [[Effects.useDebounce]] の戻り値. `apply(a)` で呼び出し、`flush()` ですぐ実行、`cancel()` で取り消します. */
final class DebouncedFn[A] private[effects](ms: Double, f: A => Unit) extends EffectHandle(() => ()) {

  private var timer: Option[Int] = None
  private var last: Option[A] = None

  /** 実行を待っている呼び出しがあればtrue */
  def isPending: Boolean = timer.isDefined

  def apply(a: A): Unit = {
    if (!isStopped) {
      timer.foreach(window.clearTimeout)
      last = Some(a)
      timer = Some(window.setTimeout(() => flush(), ms))
    }
  }

  /** 待っている呼び出しを、すぐに実行します */
  def flush(): Unit = {
    timer.foreach(window.clearTimeout)
    timer = None
    val a = last
    last = None
    a.foreach(f)
  }

  /** 待っている呼び出しを取り消します */
  def cancel(): Unit = {
    timer.foreach(window.clearTimeout)
    timer = None
    last = None
  }

  override def abort(): Unit = {
    cancel()
    super.abort()
  }

}

/** [[Effects.useThrottle]] の戻り値 */
final class ThrottledFn[A] private[effects](ms: Double, f: A => Unit) extends EffectHandle(() => ()) {

  private var lastRun = Double.NegativeInfinity
  private var timer: Option[Int] = None
  private var pending: Option[A] = None

  def apply(a: A): Unit = {
    if (!isStopped) {
      val now = js.Date.now()
      val wait = lastRun + ms - now
      if (wait <= 0 && timer.isEmpty) {
        lastRun = now
        f(a)
      } else {
        pending = Some(a)
        if (timer.isEmpty) {
          timer = Some(window.setTimeout(() => {
            timer = None
            lastRun = js.Date.now()
            val p = pending
            pending = None
            p.foreach(f)
          }, math.max(wait, 0)))
        }
      }
    }
  }

  /** 待っている呼び出しを取り消します */
  def cancel(): Unit = {
    timer.foreach(window.clearTimeout)
    timer = None
    pending = None
  }

  override def abort(): Unit = {
    cancel()
    super.abort()
  }

}

/**
 * [[Effects.useLatest]] の戻り値. `apply(future)` は、後から別の処理を始めた場合や止めた後は、完了しないFutureを返します.
 */
final class LatestFn private[effects]() extends EffectHandle(() => ()) {

  private var seq = 0

  def apply[A](f: Future[A]): Future[A] = {
    seq += 1
    val my = seq
    val p = Promise[A]()
    f.onComplete { r =>
      if (my == seq && !isStopped) p.complete(r)
    }
    p.future
  }

  /** 実行中の処理の結果を捨てます */
  def cancel(): Unit = seq += 1

}
