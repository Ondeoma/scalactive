package io.github.ondeoma.scalactive.models

import io.github.ondeoma.scalactive.enums.*
import io.github.ondeoma.scalactive.enums.SelectorAttributeOperator.Equal
import io.github.ondeoma.scalactive.enums.SelectorRelationCombinator.*
import Selector.*
import io.github.ondeoma.scalactive.enums.{SelectorAttributeOperator, SelectorRelationCombinator}

sealed trait Selector {
  val selector: String

  override def toString: String = selector.trim

  def &(s: Selector): AndSelector = {
    AndSelector(this, s)
  }

  def |(s: Selector): OrSelector = {
    OrSelector(this, s)
  }

  def +(s: Selector): RelationSelector = {
    RelationSelector(this, AdjacentSibling, s)
  }

  def ~(s: Selector): RelationSelector = {
    RelationSelector(this, GeneralSibling, s)
  }

  def >(s: Selector): RelationSelector = {
    RelationSelector(this, Child, s)
  }

  def >>(s: Selector): RelationSelector = {
    RelationSelector(this, Descendant, s)
  }

}

object Selector {

  class IdSelector(id: String) extends Selector {
    val selector: String = s"#$id"
  }

  class ClassSelector(cls: String) extends Selector {
    val selector: String = s".$cls"
  }

  class TagSelector(tag: String) extends Selector {
    val selector: String = tag
  }

  object WildcardSelector extends Selector {
    val selector: String = "*"
  }

  class RelationSelector(parent: Selector,
                         relation: SelectorRelationCombinator,
                         descendant: Selector) extends Selector {
    val selector: String = s"$parent $relation $descendant"
  }

  class AttrSelector(attr: String,
                     ope: SelectorAttributeOperator,
                     value: String = "") extends Selector {
    val selector: String = s"[$attr${ope.selector(value)}]"
  }

  // TODO: 疑似クラス

  // TODO: 疑似要素

  class NotSelector(s: Selector) extends Selector {
    val selector: String = s":not($s)"
  }

  class AndSelector(ss: Selector*) extends Selector {
    val selector: String = ss.map(_.toString).mkString
  }

  class OrSelector(ss: Selector*) extends Selector {
    val selector: String = ss.map(_.toString).mkString(", ")
  }

  class ClassesSelector(clss: String*) extends AndSelector(clss.map(ClassSelector(_)) *)

  class CustomSelector(val selector: String) extends Selector

}
