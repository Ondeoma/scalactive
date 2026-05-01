package io.github.ondeoma.scalactive.enums

enum SelectorCaseInsensitive(val v: String) {

  case CaseSensitive extends SelectorCaseInsensitive("s")

  case CaseInsensitive extends SelectorCaseInsensitive("i")

}

