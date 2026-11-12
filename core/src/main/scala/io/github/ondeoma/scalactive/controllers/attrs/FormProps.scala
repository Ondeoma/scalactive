package io.github.ondeoma.scalactive.controllers.attrs

import org.scalajs.dom.*

/**
 * フォーム部品の属性に合わせて、表示に使われるDOMプロパティも書き換えます.
 *
 * input の value・checked などの属性は「初期値」で、利用者が一度入力・クリックすると、
 * 属性を変えても表示(プロパティ)は変わらなくなります. そのため :value・:checked・:selected のバインドでは
 * プロパティも合わせて書き換えます. 値が同じなら代入しないため、入力中のカーソル位置は飛びません.
 */
object FormProps {

  /**
   * @param attr  属性名
   * @param value 属性の値(Noneは属性を外した場合)
   */
  def sync(ele: Element, attr: String, value: Option[String]): Unit = {
    attr.toLowerCase match {
      case "value" =>
        val v = value.getOrElse("")
        ele match {
          // file の value には空文字しか代入できない
          case i: HTMLInputElement => if (i.`type` != "file" && i.value != v) i.value = v
          case t: HTMLTextAreaElement => if (t.value != v) t.value = v
          case s: HTMLSelectElement => if (s.value != v) s.value = v
          case _ => ()
        }
      case "checked" =>
        ele match {
          case i: HTMLInputElement => if (i.checked != value.isDefined) i.checked = value.isDefined
          case _ => ()
        }
      case "selected" =>
        ele match {
          case o: HTMLOptionElement => if (o.selected != value.isDefined) o.selected = value.isDefined
          case _ => ()
        }
      case _ => ()
    }
  }

  /** 属性を付けて、プロパティも合わせます */
  def set(ele: Element, attr: String, value: String): Unit = {
    ele.setAttribute(attr, value)
    sync(ele, attr, Some(value))
  }

  /** 属性を外して、プロパティも合わせます */
  def remove(ele: Element, attr: String): Unit = {
    ele.removeAttribute(attr)
    sync(ele, attr, None)
  }

}
