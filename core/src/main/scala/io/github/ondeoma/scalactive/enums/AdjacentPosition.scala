package io.github.ondeoma.scalactive.enums

import io.github.ondeoma.scalactive.enums.EventType.*

enum AdjacentPosition {

  /**
   * 要素の前。要素が DOM ツリー内にあり、親要素がある場合にのみ有効。
   */
  case beforebegin

  /**
   * 要素のすぐ内側、最初の子の前。
   */
  case afterbegin

  /**
   * 要素のすぐ内側、最後の子の後。
   */
  case beforeend

  /**
   * 要素の後。要素が DOM ツリー内にあり、親要素がある場合にのみ有効。
   */
  case afterend

}

