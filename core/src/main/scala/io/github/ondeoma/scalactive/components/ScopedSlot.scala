package io.github.ondeoma.scalactive.components

import io.github.ondeoma.scalactive.utils.TypeAlias.HTML

/**
 * 子コンポーネントから値を受け取るスロット(Vueのスコープ付きスロット).
 *
 * 子はPropsに `item: ScopedSlot[Item] = ScopedSlot.empty` を定義し、`props.item(value)()`(.stでは `<slot name="item" :value="x">`)で展開します.
 * 親は `ScopedSlot { x => cm => ... }`(.stでは `<template #item="x">...</template>`)で、受け取った値を使って内容を作ります.
 * 内容は子のComponentManagerで生成されるため、リアクティブな式やイベントもそのまま使えます.
 */
final class ScopedSlot[-A] private(val render: Option[A => ComponentManager => HTML]) {

  def isEmpty: Boolean = render.isEmpty

  def nonEmpty: Boolean = render.nonEmpty

  /** 値を渡して展開します. 空の場合はfallbackを展開します. */
  def apply(value: A)
           (fallback: ComponentManager => HTML = _ => "")
           (using cm: ComponentManager): HTML = {
    render match {
      case Some(f) => f(value)(cm)
      case None => fallback(cm)
    }
  }

}

object ScopedSlot {

  val empty: ScopedSlot[Any] = new ScopedSlot[Any](None)

  def apply[A](f: A => ComponentManager => HTML): ScopedSlot[A] = new ScopedSlot(Some(f))

}
