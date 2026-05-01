package io.github.ondeoma.scalactive.utils

import org.scalajs.dom.console

object ConsoleUtil {

  enum LogLeve(val n: Int) {
    case Debug extends LogLeve(1)
    case Info extends LogLeve(2)
    case Warn extends LogLeve(3)
    case Error extends LogLeve(4)
    case Silent extends LogLeve(5)
  }

  import LogLeve.*

  var level: LogLeve = Info

  def debug(m: Any,
            ps: Any*): Unit = {
    if (level.n <= Debug.n) {
      console.debug(m, ps)
    }
  }

  def info(m: Any,
           ps: Any*): Unit = {
    if (level.n <= Info.n) {
      console.log(m, ps)
    }
  }

  def warn(m: Any,
           ps: Any*): Unit = {
    if (level.n <= Warn.n) {
      console.warn(m, ps)
    }
  }

  def error(m: Any,
            ps: Any*): Unit = {
    if (level.n <= Error.n) {
      console.error(m, ps)
      console.trace()
    }
  }

  def count(label: String): Unit = {
    if (level.n <= Debug.n) {
      console.count(label)
    }
  }

  def countReset(label: String): Unit = {
    if (level.n <= Debug.n) {
      console.countReset(label)
    }
  }

}
