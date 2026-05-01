package io.github.ondeoma.scalactive.facades

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

@js.native
@JSGlobal("crypto")
object Crypto extends js.Object {
  def randomUUID(): String = js.native
}
