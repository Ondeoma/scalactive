package io.github.ondeoma.scalactive.components

import org.scalajs.dom.{Event, HTMLElement, document}

import scala.scalajs.js

/**
 * 要素に付ける独自のディレクティブ(Vueのカスタムディレクティブ).
 *
 * .stでは、組み込みでない `v-名前="値"` が、スコープにある `名前`(ケバブケースはキャメルケースに変換)の
 * Directiveとして扱われます. 値を省略した場合は `()` を渡します(Directive[Unit]).
 * 値の式で読んだリアクティブ値が変わると updated が呼ばれます.
 *
 *  - mounted: 要素がDOMに挿入された後(onMountedと同じタイミング)
 *  - updated: 値が変わり、その変更による描き直しが終わった後(v-htmlの中身も入れ替わった後)
 *  - unmounted: 要素が片付けられたとき
 *
 * 1つのDirectiveのインスタンスを複数の要素で使うので、要素ごとの状態は要素(dataset など)に持たせるか、
 * 要素ごとにインスタンスを作ってください(`v-x="..."` の `x` を def にすると、描画のたびに作られます).
 *
 * {{{
 * // v-htmlで描いた中のコードを色付けする
 * val highlight: Directive[String] = Directive((el, _) => Prism.highlightAllUnder(el))
 * // <div v-html="s.body" v-highlight="s.body"></div>
 * }}}
 */
trait Directive[A] {

  def mounted(el: HTMLElement, value: A): Unit = ()

  def updated(el: HTMLElement, value: A, oldValue: A): Unit = ()

  def unmounted(el: HTMLElement): Unit = ()

}

object Directive {

  /** mountedとupdatedの両方でfを呼ぶディレクティブ */
  def apply[A](f: (HTMLElement, A) => Unit): Directive[A] = new Directive[A] {
    override def mounted(el: HTMLElement, value: A): Unit = f(el, value)

    override def updated(el: HTMLElement, value: A, oldValue: A): Unit = f(el, value)
  }

  /** mountedだけでfを呼ぶディレクティブ */
  def onMounted[A](f: (HTMLElement, A) => Unit): Directive[A] = new Directive[A] {
    override def mounted(el: HTMLElement, value: A): Unit = f(el, value)
  }

  // ---- 組み込みのディレクティブ(.stの v-focus・v-scroll-into-view・v-click-outside) ----

  /**
   * `v-focus` / `v-focus="cond"`: DOMに挿入されたとき(condが真なら)と、condが真に変わったときにフォーカスします.
   * `v-focus.select` は中の文字列を選択します.
   */
  def focus(select: Boolean = false): Directive[Boolean] = new Directive[Boolean] {
    private def run(el: HTMLElement): Unit = {
      el.asInstanceOf[js.Dynamic].focus(js.Dynamic.literal(preventScroll = false))
      if (select && !js.isUndefined(el.asInstanceOf[js.Dynamic].select)) el.asInstanceOf[js.Dynamic].select()
    }

    override def mounted(el: HTMLElement, value: Boolean): Unit = if (value) run(el)

    override def updated(el: HTMLElement, value: Boolean, oldValue: Boolean): Unit = if (value && !oldValue) run(el)
  }

  /**
   * `v-scroll-into-view` / `v-scroll-into-view="cond"`: DOMに挿入されたとき(condが真なら)と、condが真に変わったときに
   * 要素が見える位置までスクロールします. 修飾子: `.smooth`(なめらかに)、`.start`・`.center`・`.end`(位置. 既定はnearest)
   */
  def scrollIntoView(smooth: Boolean = false, block: String = "nearest"): Directive[Boolean] = new Directive[Boolean] {
    private def run(el: HTMLElement): Unit = {
      el.asInstanceOf[js.Dynamic].scrollIntoView(js.Dynamic.literal(behavior = if (smooth) "smooth" else "auto", block = block))
    }

    override def mounted(el: HTMLElement, value: Boolean): Unit = if (value) run(el)

    override def updated(el: HTMLElement, value: Boolean, oldValue: Boolean): Unit = if (value && !oldValue) run(el)
  }

  /**
   * `v-click-outside="stmt"`: 要素の外を押したときに実行します(メニュー・ポップアップを閉じるなど). 文の中では `event` を参照できます.
   * 押し始め(pointerdown)で判定するので、押した要素がその後の描き直しで消えても正しく判定できます.
   */
  def clickOutside(): Directive[Event => Unit] = new Directive[Event => Unit] {
    private var handler: Event => Unit = _ => ()
    private var listener: Option[js.Function1[Event, Unit]] = None

    override def mounted(el: HTMLElement, value: Event => Unit): Unit = {
      handler = value
      val l: js.Function1[Event, Unit] = (e: Event) => {
        val path = e.asInstanceOf[js.Dynamic].composedPath.asInstanceOf[js.UndefOr[js.Function0[js.Array[Any]]]]
        val inside = path.toOption match {
          case Some(_) => e.asInstanceOf[js.Dynamic].composedPath().asInstanceOf[js.Array[Any]].exists(_ == el)
          case None => e.target match {
            case n: org.scalajs.dom.Node => el.contains(n)
            case _ => false
          }
        }
        if (!inside && el.isConnected) handler(e)
      }
      listener = Some(l)
      document.addEventListener("pointerdown", l, useCapture = true)
    }

    override def updated(el: HTMLElement, value: Event => Unit, oldValue: Event => Unit): Unit = handler = value

    override def unmounted(el: HTMLElement): Unit = {
      listener.foreach(l => document.removeEventListener("pointerdown", l, useCapture = true))
      listener = None
    }
  }

}
