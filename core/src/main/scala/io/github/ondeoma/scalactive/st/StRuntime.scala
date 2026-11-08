package io.github.ondeoma.scalactive.st

import io.github.ondeoma.scalactive.components.{ComponentManager, Slot}
import io.github.ondeoma.scalactive.components.htmls.HtmlComponent
import io.github.ondeoma.scalactive.components.texts.TextComponent
import io.github.ondeoma.scalactive.controllers.attrs.AttrController
import io.github.ondeoma.scalactive.controllers.events.EventController
import io.github.ondeoma.scalactive.enums.EventType
import io.github.ondeoma.scalactive.reactive.*
import io.github.ondeoma.scalactive.utils.TypeAlias.*
import org.scalajs.dom.*

import scala.annotation.implicitNotFound
import scala.collection.mutable
import scala.scalajs.js

/**
 * .st(単一ファイルコンポーネント)からsbt-scalactiveが生成するコードが利用するヘルパー.
 * 値がリアクティブかどうかをオーバーロードで判定し、既存のコンポーネント/コントローラーへ振り分けます.
 */
object StRuntime {

  private val injectedStyles = mutable.Set[String]()

  /**
   * <style>ブロックの内容をheadに1度だけ挿入します.
   */
  def injectStyle(id: String,
                  css: String): Unit = {
    if (!injectedStyles.contains(id)) {
      injectedStyles.addOne(id)
      val style = document.createElement("style")
      style.setAttribute("data-st", id)
      style.textContent = css
      document.head.appendChild(style)
    }
  }

  def escape(s: String): String = {
    s.replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&#39;")
  }

  // {{ expr }}

  def text[A](rv: Reactive[A])
             (using cm: ComponentManager): HTML = {
    ComponentManager.%(TextComponent(rv)(String.valueOf(_)))
  }

  def text(v: Any)
          (using cm: ComponentManager): HTML = {
    escape(String.valueOf(v))
  }

  // v-html="expr"
  // 値をHTMLとしてそのまま展開します(エスケープしません). 信頼できない値を渡さないでください.

  def html(rv: Reactive[String])
          (using cm: ComponentManager): HTML = {
    ComponentManager.%(HtmlComponent(rv)(identity))
  }

  def html[A](rv: Reactive[A])
             (using d: DummyImplicit,
              cm: ComponentManager): HTML = {
    ComponentManager.%(HtmlComponent(rv)(a => if (a == null) "" else String.valueOf(a)))
  }

  def html(v: Any)
          (using cm: ComponentManager): HTML = {
    if (v == null) "" else String.valueOf(v)
  }

  // v-if / v-else-if / v-else / v-show

  def cond(rv: Reactive[Boolean]): Reactive[Boolean] = rv

  def cond(b: Boolean): Reactive[Boolean] = new RV(b)

  def not(rv: Reactive[Boolean])
         (using cm: ComponentManager): Reactive[Boolean] = {
    CRV(rv, (b: Boolean) => !b)
  }

  /**
   * 複数のリアクティブ値から計算される真偽値. 値が変わった時のみ通知します.
   * 一時リアクティブ値として登録され、abort時に元の値の監視を解除します.
   */
  private final class Branch(srcs: Seq[Reactive[Boolean]],
                             calc: () => Boolean) extends RV[Boolean](calc()) {

    private val infos = srcs.map(_.addWatcher { _ =>
      val n = calc()
      if (n != v) this := n
    })

    override def abort(): Unit = {
      infos.foreach(_.abort())
      super.abort()
    }

  }

  private def branch(srcs: Seq[Reactive[Boolean]],
                     calc: () => Boolean)
                    (using cm: ComponentManager): Reactive[Boolean] = {
    val b = new Branch(srcs, calc)
    cm.addTmpReactive(b)
    b
  }

  /** v-else-if: 先行する条件(prev)がすべて偽で、cが真の場合に真. */
  def elseIf(c: Reactive[Boolean],
             prev: Reactive[Boolean]*)
            (using cm: ComponentManager): Reactive[Boolean] = {
    branch(prev :+ c, () => !prev.exists(_.v) && c.v)
  }

  /** v-else: 先行する条件(prev)がすべて偽の場合に真. */
  def otherwise(prev: Reactive[Boolean]*)
               (using cm: ComponentManager): Reactive[Boolean] = {
    branch(prev, () => !prev.exists(_.v))
  }

  // :name="expr"

  def attr(name: String,
           rv: Reactive[String])
          (using cm: ComponentManager): ATTR = {
    ComponentManager.attrS(name, rv)
  }

  def attr(name: String,
           rv: Reactive[Boolean])
          (using d: DummyImplicit,
           cm: ComponentManager): ATTR = {
    ComponentManager.attrIf(rv, name)
  }

  def attr[A](name: String,
              rv: Reactive[A])
             (using d1: DummyImplicit,
              d2: DummyImplicit,
              cm: ComponentManager): ATTR = {
    ComponentManager.attrS(name, CRV(rv, (a: A) => String.valueOf(a)))
  }

  def attr(name: String,
           v: Any)
          (using cm: ComponentManager): ATTR = {
    v match {
      case true => s" $name"
      case false | null | None => ""
      case Some(x) => s""" $name="${escape(String.valueOf(x))}""""
      case x => s""" $name="${escape(String.valueOf(x))}""""
    }
  }

  // :class="expr"
  // 静的なclass属性と併用できるよう、常にクラスコントローラー経由で付与します.

  private def splitClasses(s: String): List[String] = {
    s.split("\\s+").toList.filter(_.nonEmpty)
  }

  def cls(rv: Reactive[String])
         (using cm: ComponentManager): ATTR = {
    ComponentManager.clsSs(CRV(rv, splitClasses))
  }

  def cls(rv: Reactive[List[String]])
         (using d: DummyImplicit,
          cm: ComponentManager): ATTR = {
    ComponentManager.clsSs(rv)
  }

  def cls(v: Any)
         (using cm: ComponentManager): ATTR = {
    val clss = v match {
      case s: String => splitClasses(s)
      case xs: Iterable[?] => xs.toList.map(String.valueOf).flatMap(splitClasses)
      case x => splitClasses(String.valueOf(x))
    }
    ComponentManager.clsSs(new RV(clss))
  }

  // @event="stmt"

  def on(name: String,
         handler: Event => Unit)
        (using cm: ComponentManager): ATTR = {
    ComponentManager.ev(EventType.CustomEventType(name), handler)
  }

  // @event.enter / .ctrl / .self などの修飾子

  /** キーボードイベントのkeyがkeysのいずれかであるか. */
  def key(event: Event,
          keys: String*): Boolean = {
    event match {
      case k: KeyboardEvent => keys.contains(k.key)
      case _ => false
    }
  }

  /** 修飾キー(ctrl/shift/alt/meta)が押されているか. */
  def modifier(event: Event,
               name: String): Boolean = {
    event.asInstanceOf[js.Dynamic].selectDynamic(name + "Key").asInstanceOf[js.UndefOr[Boolean]].getOrElse(false)
  }

  /** イベントが要素自身で発生したか(子要素からのバブリングでないか). */
  def self(event: Event): Boolean = {
    event.target == event.currentTarget
  }

  // v-model="rv"

  /**
   * v-modelで要素とRVを同期する方法.
   * String/Int/Double/Boolean/List[String]のインスタンスを用意しています.
   */
  @implicitNotFound("v-model does not support RV[${A}] (supported: RV[String], RV[Int], RV[Double], RV[Boolean], RV[List[String]])")
  trait ModelBinding[A] {

    /** 値を要素へ反映します. */
    def write(ele: HTMLElement, a: A, trim: Boolean): Unit

    /** 要素から値を読み取ります. 反映すべき値がない場合はNone. */
    def read(ele: HTMLElement, current: A, trim: Boolean): Option[A]

  }

  private def dyn(ele: HTMLElement): js.Dynamic = ele.asInstanceOf[js.Dynamic]

  private def inputType(ele: HTMLElement): String = {
    if (ele.tagName.equalsIgnoreCase("input")) String.valueOf(dyn(ele).`type`).toLowerCase else ""
  }

  private def isCheckable(ele: HTMLElement): Boolean = {
    val t = inputType(ele)
    t == "checkbox" || t == "radio"
  }

  private def isSelect(ele: HTMLElement): Boolean = ele.tagName.equalsIgnoreCase("select")

  private def elementValue(ele: HTMLElement): String = String.valueOf(dyn(ele).value)

  private def checked(ele: HTMLElement): Boolean = dyn(ele).checked.asInstanceOf[js.UndefOr[Boolean]].getOrElse(false)

  private def stringLike[A](toS: A => String,
                            fromS: String => Option[A]): ModelBinding[A] = new ModelBinding[A] {

    def write(ele: HTMLElement, a: A, trim: Boolean): Unit = {
      if (isCheckable(ele)) {
        dyn(ele).checked = elementValue(ele) == toS(a)
      } else if (!read(ele, a, trim).contains(a)) {
        // 入力途中の値(末尾の空白や"1."など)を書き換えないよう、読み取り結果が異なる場合のみ反映する
        dyn(ele).value = toS(a)
      }
    }

    def read(ele: HTMLElement, current: A, trim: Boolean): Option[A] = {
      if (isCheckable(ele)) {
        if (checked(ele)) fromS(elementValue(ele)) else None
      } else {
        val s = elementValue(ele)
        fromS(if (trim) s.trim else s)
      }
    }

  }

  object ModelBinding {

    given ModelBinding[String] = stringLike(identity, Some(_))

    given ModelBinding[Int] = stringLike(_.toString, _.trim.toIntOption)

    given ModelBinding[Double] = stringLike(_.toString, _.trim.toDoubleOption)

    given ModelBinding[Boolean] = new ModelBinding[Boolean] {

      def write(ele: HTMLElement, a: Boolean, trim: Boolean): Unit = dyn(ele).checked = a

      def read(ele: HTMLElement, current: Boolean, trim: Boolean): Option[Boolean] = Some(checked(ele))

    }

    /** チェックボックスのグループ、または複数選択のselect. */
    given ModelBinding[List[String]] = new ModelBinding[List[String]] {

      private def options(ele: HTMLElement): List[js.Dynamic] = {
        val os = dyn(ele).options
        (0 until os.length.asInstanceOf[Int]).map(i => os.item(i)).toList
      }

      def write(ele: HTMLElement, a: List[String], trim: Boolean): Unit = {
        if (isSelect(ele)) {
          options(ele).foreach(o => o.selected = a.contains(String.valueOf(o.value)))
        } else {
          dyn(ele).checked = a.contains(elementValue(ele))
        }
      }

      def read(ele: HTMLElement, current: List[String], trim: Boolean): Option[List[String]] = {
        if (isSelect(ele)) {
          Some(options(ele).filter(_.selected.asInstanceOf[Boolean]).map(o => String.valueOf(o.value)))
        } else {
          val v = elementValue(ele)
          if (checked(ele)) Some(if (current.contains(v)) current else current :+ v)
          else Some(current.filterNot(_ == v))
        }
      }

    }

  }

  /**
   * input/textarea/selectの値とRVを双方向に同期します.
   * チェックボックス・ラジオボタン・select・lazy指定の場合はchange、それ以外はinputイベントで反映します.
   * IME変換中の入力は確定(compositionend)まで反映しません.
   */
  def model[A](rv: RV[A],
               lazyUpdate: Boolean = false,
               trim: Boolean = false)
              (using b: ModelBinding[A],
               cm: ComponentManager): ATTR = {
    val sync: HTMLElement => AttrController = ele => AttrController { ac =>
      ac.element = Some(ele)
      b.write(ele, rv.v, trim)
      ac.watchInfos = List(rv.addWatcher { case (_, n) => b.write(ele, n, trim) })
      Right(ac)
    }
    def update(ele: HTMLElement)(e: Event): Unit = {
      val composing = e.asInstanceOf[js.Dynamic].isComposing.asInstanceOf[js.UndefOr[Boolean]].getOrElse(false)
      val compositionEnd = e.`type` == "compositionend"
      if (lazyUpdate && compositionEnd) ()
      else if (!composing || compositionEnd) {
        b.read(ele, rv.v, trim).foreach(n => if (n != rv.v) rv := n)
      }
    }
    val listen: HTMLElement => EventController = ele => {
      val usesChange = lazyUpdate || isCheckable(ele) || isSelect(ele)
      val et = if (usesChange) EventType.change else EventType.CustomEventType("input")
      EventController(ele, et, update(ele))
    }
    val listenComposition: HTMLElement => EventController = ele => {
      EventController(ele, EventType.CustomEventType("compositionend"), update(ele))
    }
    ComponentManager.el(sync, listen, listenComposition)
  }

  // <slot /> と、子コンポーネントへ渡す子要素

  def slotOf(f: ComponentManager => HTML): Slot = Slot(f)

  def slot(s: Slot)
          (fallback: ComponentManager => HTML)
          (using cm: ComponentManager): HTML = {
    s(fallback)
  }

  // <ChildComponent />

  def component(f: GenComponentF)
               (using cm: ComponentManager): HTML = {
    ComponentManager.c(f)
  }

}
