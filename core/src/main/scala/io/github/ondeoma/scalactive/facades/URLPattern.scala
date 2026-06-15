package io.github.ondeoma.scalactive.facades

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

type URL = String

@js.native
trait URLPatternInput extends js.Object {
  val protocol: js.UndefOr[String] = js.native
  val username: js.UndefOr[String] = js.native
  val password: js.UndefOr[String] = js.native
  val hostname: js.UndefOr[String] = js.native
  val port: js.UndefOr[String] = js.native
  val pathname: js.UndefOr[String] = js.native
  val search: js.UndefOr[String] = js.native
  val hash: js.UndefOr[String] = js.native
  val baseURL: js.UndefOr[String] = js.native
}

object URLPatternInput {
  def apply(protocol: js.UndefOr[String] = js.undefined,
            username: js.UndefOr[String] = js.undefined,
            password: js.UndefOr[String] = js.undefined,
            hostname: js.UndefOr[String] = js.undefined,
            port: js.UndefOr[String] = js.undefined,
            pathname: js.UndefOr[String] = js.undefined,
            search: js.UndefOr[String] = js.undefined,
            hash: js.UndefOr[String] = js.undefined,
            baseURL: js.UndefOr[String] = js.undefined): URLPatternInput = {
    js.Dynamic.literal(
      protocol = protocol,
      username = username,
      password = password,
      hostname = hostname,
      port = port,
      pathname = pathname,
      search = search,
      hash = hash,
      baseURL = baseURL
    ).asInstanceOf[URLPatternInput]
  }
}


@js.native
trait URLPatternOptions extends js.Object {
  val ignoreCase: js.UndefOr[Boolean] = js.native
}

object URLPatternOptions {
  def apply(ignoreCase: js.UndefOr[Boolean] = js.undefined): URLPatternOptions = {
    js.Dynamic.literal(
      ignoreCase = ignoreCase,
    ).asInstanceOf[URLPatternOptions]
  }
}

@js.native
trait URLPatternComponentResult extends js.Object {
  val input: String = js.native
  val groups: js.Dictionary[String] = js.native
}

@js.native
trait URLPatternResult extends js.Object {
  val inputs: js.Array[js.Any] = js.native
  val protocol: URLPatternComponentResult = js.native
  val username: URLPatternComponentResult = js.native
  val password: URLPatternComponentResult = js.native
  val hostname: URLPatternComponentResult = js.native
  val port: URLPatternComponentResult = js.native
  val pathname: URLPatternComponentResult = js.native
  val search: URLPatternComponentResult = js.native
  val hash: URLPatternComponentResult = js.native
}


/**
 * https://developer.mozilla.org/ja/docs/Web/API/URLPattern/URLPattern
 */
@js.native
@JSGlobal
class URLPattern(init: URL | URLPatternInput,
                 baseURL: js.UndefOr[String] = js.undefined,
                 options: js.UndefOr[URLPatternOptions] = js.undefined) extends js.Object {

  val protocol: String = js.native
  val username: String = js.native
  val password: String = js.native
  val hostname: String = js.native
  val port: String = js.native
  val pathname: String = js.native
  val search: String = js.native
  val hash: String = js.native
  val hasRegExpGroups: Boolean = js.native

  /**
   * https://developer.mozilla.org/ja/docs/Web/API/URLPattern/test
   */
  def test(input: URL | URLPatternInput,
           baseURL: js.UndefOr[String] = js.undefined): Boolean = js.native

  /**
   * https://developer.mozilla.org/ja/docs/Web/API/URLPattern/exec
   */
  def exec(input: URL | URLPatternInput,
           baseURL: js.UndefOr[String] = js.undefined): js.UndefOr[URLPatternResult] = js.native
}
