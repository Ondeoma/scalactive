package io.github.ondeoma.scalactive.models

import io.github.ondeoma.scalactive.utils.TypeAlias.*
import org.scalajs.dom.*

sealed trait AddMethod

object AddMethod {

  sealed trait AddMethodByNode extends AddMethod

  sealed trait AddMethodBySelector extends AddMethod

  sealed trait AddMethodByComment extends AddMethod

  /**
   * 子要素の末尾
   */
  case class append(parent: Element) extends AddMethodByNode

  /**
   * 子要素の先頭
   */
  case class prepend(parent: Element) extends AddMethodByNode

  /**
   * 要素の前
   */
  case class before(element: Element) extends AddMethodByNode

  /**
   * 要素の後
   */
  case class after(element: Element) extends AddMethodByNode

  /**
   * 子要素targetの前
   */
  case class insertBefore(parent: Node,
                          target: Node) extends AddMethodByNode

  /**
   * 子要素targetと入替
   */
  case class replaceChild(parent: Node,
                          target: Node) extends AddMethodByNode

  /**
   * 要素の入れ替え
   */
  case class replaceWith(element: Element) extends AddMethodByNode

  /**
   * 子要素の末尾
   */
  case class appendS(selector: Selector) extends AddMethodBySelector

  /**
   * 子要素の先頭
   */
  case class prependS(selector: Selector) extends AddMethodBySelector

  /**
   * 要素の前
   */
  case class beforeS(selector: Selector) extends AddMethodBySelector

  /**
   * 要素の後
   */
  case class afterS(selector: Selector) extends AddMethodBySelector

  /**
   * 子要素targetの前
   */
  case class insertBeforeS(selector: Selector,
                           target: Selector) extends AddMethodBySelector

  /**
   * 子要素targetと入替
   */
  case class replaceChildS(selector: Selector,
                           target: Selector) extends AddMethodBySelector

  /**
   * 要素の入れ替え
   */
  case class replaceWithS(selector: Selector) extends AddMethodBySelector

  /**
   * コメントの前
   */
  case class beforeC(commentId: ID) extends AddMethodByComment

  /**
   * コメントの後
   */
  case class afterC(commentId: ID) extends AddMethodByComment

}

