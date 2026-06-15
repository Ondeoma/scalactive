package io.github.ondeoma.scalactive.facades

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import scala.scalajs.js.typedarray.TypedArray

/**
 * https://developer.mozilla.org/ja/docs/Web/API/Crypto
 */
@js.native
@JSGlobal("crypto")
object Crypto extends js.Object {

  /**
   * https://developer.mozilla.org/ja/docs/Web/API/Crypto/getRandomValues
   */
  def getRandomValues[T, Repr](arr: TypedArray[T, Repr]): TypedArray[T, Repr] = js.native

  /**
   * https://developer.mozilla.org/ja/docs/Web/API/Crypto/randomUUID
   */
  def randomUUID(): String = js.native

}
