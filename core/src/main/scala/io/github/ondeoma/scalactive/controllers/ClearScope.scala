package io.github.ondeoma.scalactive.controllers

import org.scalajs.dom.Node

import scala.scalajs.js

/**
 * clearの連鎖(親→子)の間、DOMから取り除いたノードを覚えておき、
 * その内側にある子孫コンポーネントのノードを1つずつ取り除く手間を省きます.
 * (取り除いた要素の内側のノードは、要素と一緒にDOMから外れているため)
 */
private[scalactive] object ClearScope {

  private val removed = new js.Set[Node]()
  private var depth = 0

  /** fの間に取り除いたノードを覚えておく. 一番外側のclearが終わったら忘れます. */
  def run[A](f: => A): A = {
    depth += 1
    try f
    finally {
      depth -= 1
      if (depth == 0) removed.clear()
    }
  }

  /**
   * nを親から取り除きます. ただし、このclearの連鎖で取り除いた要素の内側にあれば何もしません.
   */
  def remove(n: Node): Unit = {
    val p = n.parentNode
    if (p != null && !insideRemoved(p)) {
      p.removeChild(n)
      if (depth > 0) removed.add(n)
    }
  }

  private def insideRemoved(from: Node): Boolean = {
    if (removed.size == 0) false
    else {
      var top = from
      while (top.parentNode != null) top = top.parentNode
      removed.contains(top)
    }
  }

}
