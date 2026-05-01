package io.github.ondeoma.scalactive.enums

enum SelectorRelationCombinator(val v: String) {

  override def toString: String = v

  /**
   * 隣接兄弟
   */
  case AdjacentSibling extends SelectorRelationCombinator("+")

  /**
   * 兄弟
   */
  case GeneralSibling extends SelectorRelationCombinator("~")

  /**
   * 子
   */
  case Child extends SelectorRelationCombinator(">")

  /**
   * 子孫
   */
  case Descendant extends SelectorRelationCombinator(" ")

}

