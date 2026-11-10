package io.github.ondeoma.scalactive.components

import io.github.ondeoma.scalactive.ScalactiveConfig
import io.github.ondeoma.scalactive.models.WatchInfo
import io.github.ondeoma.scalactive.reactive.Reactive
import org.scalajs.dom.document

import scala.collection.mutable.ListBuffer

/**
 * pageTitleで指定した画面のタイトル. 表示されている間だけ有効で、`stop()` で取り下げます.
 * コンポーネントの中(mountedを含む)で指定したものは、コンポーネントが片付けられると自動で取り下げます.
 */
final class TitleHandle private[components](source: Either[String, Reactive[String]],
                                            private[components] val order: Long) extends Reactive[Unit] {

  private var active = true

  private val info: Option[WatchInfo] = source.toOption.map(_.addWatcher(_ => TitleStack.refresh()))

  override def v: Unit = ()

  def title: String = source.fold(identity, r => io.github.ondeoma.scalactive.reactive.Tracking.untracked(r.v))

  def stop(): Unit = abort()

  override def abort(): Unit = {
    if (active) {
      active = false
      info.foreach(_.abort())
      TitleStack.remove(this)
    }
    super.abort()
  }

}

object TitleHandle {

  /**
   * @param inComponent ComponentManagerの中(描画中)で作られた場合はtrue.
   *                    falseで、onMountedの実行中なら、そのコンポーネントが描画された順を使います
   */
  private[scalactive] def create(source: Either[String, Reactive[String]],
                                 inComponent: Boolean): TitleHandle = {
    val running = LifecycleHooks.running
    val order = if (!inComponent && running != null) running.order else TitleStack.nextOrder()
    val h = new TitleHandle(source, order)
    TitleStack.push(h)
    h
  }

}

/**
 * 画面のタイトル(document.title)を決めます.
 *
 * 指定されたタイトルのうち、後から描画されたコンポーネントのもの(子・ネストしたルートの側)を使います.
 * mountedは子から先に実行されるため、実行した順ではなく、コンポーネントを描画した順で比べます.
 * どれも指定されていなければ ScalactiveConfig.defaultTitle(なければ指定される前のタイトル)に戻します.
 */
private[scalactive] object TitleStack {

  private var counter = 0L
  private val stack = ListBuffer[TitleHandle]()
  private var original: String = null

  def nextOrder(): Long = {
    counter += 1
    counter
  }

  def push(h: TitleHandle): Unit = {
    // 指定が1つもない状態から指定されたときのタイトルを、全て取り下げたときに戻す
    if (stack.isEmpty) original = document.title
    // 同じ順のものは、後から指定したものを優先する
    val i = stack.lastIndexWhere(_.order <= h.order)
    stack.insert(i + 1, h)
    refresh()
  }

  def remove(h: TitleHandle): Unit = {
    stack -= h
    refresh()
  }

  def refresh(): Unit = {
    val t = stack.lastOption match {
      case Some(h) => ScalactiveConfig.titleTemplate.replace("%s", h.title)
      case None => ScalactiveConfig.defaultTitle.getOrElse(original)
    }
    if (t != null && document.title != t) document.title = t
  }

}
