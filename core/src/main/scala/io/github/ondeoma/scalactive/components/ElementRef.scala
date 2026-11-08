package io.github.ondeoma.scalactive.components

import org.scalajs.dom.Element

/**
 * テンプレート内の要素への参照(Vueのテンプレート参照、ReactのuseRef).
 *
 * `<input ${ref(input)}>` のように結び付けると、要素が作られたときに設定され、
 * コンポーネントが片付けられると空に戻ります. DOMに挿入された後の処理はonMountedの中で行ってください.
 * .stのコンポーネントは1つのobjectなので、インスタンスごとの参照はStateに持たせます.
 *
 * {{{
 * case class State(input: ElementRef[HTMLInputElement] = ElementRef())
 * }}}
 */
final class ElementRef[E <: Element] {

  private var element: Option[E] = None

  /** 結び付いている要素(描画前・片付け後はNone) */
  def get: Option[E] = element

  /** 結び付いている要素に対して処理を行います(なければ何もしません). */
  def foreach(f: E => Unit): Unit = element.foreach(f)

  private[scalactive] def set(e: Option[E]): Unit = element = e

}

object ElementRef {

  def apply[E <: Element](): ElementRef[E] = new ElementRef[E]

}
