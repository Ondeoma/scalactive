package io.github.ondeoma.scalactive.components

import io.github.ondeoma.scalactive.utils.TypeAlias.HTML

/**
 * 親から子コンポーネントへ渡す子要素(.stの<slot />).
 * 子コンポーネントのPropsに `slot: Slot = Slot.empty` を定義すると受け取れます.
 * 内容は子のComponentManagerで生成されるため、リアクティブな式やイベントもそのまま使えます.
 */
final class Slot private(val render: Option[ComponentManager => HTML]) {

  def isEmpty: Boolean = render.isEmpty

  def nonEmpty: Boolean = render.nonEmpty

  /** 子要素をHTMLとして展開します. 空の場合はfallbackを展開します. */
  def apply(fallback: ComponentManager => HTML = _ => "")
           (using cm: ComponentManager): HTML = {
    render.getOrElse(fallback)(cm)
  }

}

object Slot {

  val empty: Slot = new Slot(None)

  def apply(f: ComponentManager => HTML): Slot = new Slot(Some(f))

}
