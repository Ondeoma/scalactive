package io.github.ondeoma.scalactive.controllers

import org.scalajs.dom.{Element, HTMLElement, document}

import scala.scalajs.js

/**
 * 描き直しの前にフォーカスがあった要素を覚えておき、描き直しでその要素が作り直されたら、
 * 同じ位置に作られた同じ種類の要素へフォーカスと選択範囲を戻します.
 * (入力のたびに入力欄を含む部分が描き直されても、入力を続けられるように)
 */
private[scalactive] object FocusKeeper {

  final case class Snapshot(element: Element,
                            id: String,
                            path: List[Int],
                            tag: String,
                            selection: Option[(Int, Int, String)])

  private def dyn(e: Element): js.Dynamic = e.asInstanceOf[js.Dynamic]

  /** 文書の中の要素の位置(body からの子要素の番号の並び) */
  private def pathOf(e: Element): Option[List[Int]] = {
    var cur: Element = e
    var path = List.empty[Int]
    while (cur != null && (cur ne document.body)) {
      val p = cur.parentNode match {
        case pe: Element => pe
        case _ => null
      }
      if (p == null) return None
      val cs = p.children
      var i = 0
      while (i < cs.length && (cs(i) ne cur)) i += 1
      path = i :: path
      cur = p
    }
    if (cur eq document.body) Some(path) else None
  }

  private def selectionOf(e: Element): Option[(Int, Int, String)] = {
    scala.util.Try {
      val d = dyn(e)
      val start = d.selectionStart
      if (js.isUndefined(start) || start == null) None
      else Some((start.asInstanceOf[Int], d.selectionEnd.asInstanceOf[Int], String.valueOf(d.selectionDirection)))
    }.toOption.flatten
  }

  def snapshot(): Option[Snapshot] = {
    Option(document.activeElement).filter(e => (e ne document.body) && (e ne document.documentElement)).flatMap { e =>
      pathOf(e).map(p => Snapshot(e, e.id, p, e.tagName, selectionOf(e)))
    }
  }

  def restore(snap: Snapshot): Unit = {
    val s = snap
    // 要素が残っていれば何もしない(フォーカスは元のまま)
    if (!s.element.isConnected) {
      val byId = Option(s.id).filter(_.nonEmpty).flatMap(id => Option(document.getElementById(id)))
      val target = byId.orElse {
        s.path.foldLeft(Option[Element](document.body)) { (cur, i) =>
          cur.flatMap(c => if (i >= 0 && i < c.children.length) Option(c.children(i)) else None)
        }
      }.filter(e => e.tagName == snap.tag)
      target.foreach {
        case h: HTMLElement =>
          scala.util.Try {
            h.asInstanceOf[js.Dynamic].focus(js.Dynamic.literal(preventScroll = true))
            s.selection.foreach { (a, b, dir) =>
              h.asInstanceOf[js.Dynamic].setSelectionRange(a, b, dir)
            }
          }
        case _ => ()
      }
    }
  }

}
