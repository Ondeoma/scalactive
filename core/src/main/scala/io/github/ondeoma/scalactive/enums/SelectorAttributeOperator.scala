package io.github.ondeoma.scalactive.enums

sealed trait SelectorAttributeOperator(val ope: String) {
  def selector(v: String): String = s"$ope$v"
}

object SelectorAttributeOperator {

  case object Has extends SelectorAttributeOperator("") {
    override def selector(v: String): String = s"$ope"
  }

  case object Equal extends SelectorAttributeOperator("=")

  case object Contain extends SelectorAttributeOperator("*=")

  case object EqualOrStartHyphenWith extends SelectorAttributeOperator("|=")

  case object StartWith extends SelectorAttributeOperator("^=")

  case object EndWith extends SelectorAttributeOperator("$=")

  case object ContainSplitSpaceWord extends SelectorAttributeOperator("~=")

}

