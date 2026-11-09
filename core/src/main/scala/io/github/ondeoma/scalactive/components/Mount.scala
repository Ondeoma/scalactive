package io.github.ondeoma.scalactive.components

import io.github.ondeoma.scalactive.controllers.ComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.Reactive
import io.github.ondeoma.scalactive.utils.TypeAlias.GenComponentF
import org.scalajs.dom.*

/**
 * mountで描画したコンポーネント. `unmount()` で片付けます(onUnmountedも実行されます).
 */
final class MountHandle private[components](val controller: ComponentController[?]) extends Reactive[Unit] {

  private var mounted = true

  override def v: Unit = ()

  def isMounted: Boolean = mounted

  /** 描画したものを取り除きます. 2回目以降は何もしません. */
  def unmount(): Unit = abort()

  override def abort(): Unit = {
    if (mounted) {
      mounted = false
      controller.clear()
    }
    super.abort()
  }

}

object Mount {

  private[scalactive] def start(el: Element,
                                component: GenComponentF): Either[String, MountHandle] = {
    val root = el.asInstanceOf[HTMLElement]
    component(root, AddMethod.append(root)).init().map(c => new MountHandle(c))
  }

}
