package io.github.ondeoma.scalactive.routes

import io.github.ondeoma.scalactive.ScalactiveConfig
import io.github.ondeoma.scalactive.reactive.RV
import org.scalajs.dom.window.{history, location}
import org.scalajs.dom.{HTMLElement, document, window}

import scala.concurrent.duration.DurationInt
import scala.scalajs.js
import scala.util.Try

object Router {

  val href = RV(location.href)

  window.addEventListener("popstate", _ => href := location.href)

  def go(url: String): Try[Unit] = {
    Try {
      val now = location.href
      val fixedUrl = fixUrl(url)
      window.scrollTo(0, 0)
      history.pushState(js.Object.apply(), "", fixedUrl)
      startCheckHref(now)
    }
  }

  def fixUrl(url: String): String = {
    lazy val relPath = !url.startsWith("http")
    lazy val isSameOrigin = url.startsWith(location.origin)
    lazy val base = ScalactiveConfig.fixedBasePath
    if (relPath) joinSlash(base, url)
    else if (isSameOrigin) joinSlash(base, url.replace(location.origin, ""))
    else url
  }

  def joinSlash(a: String,
                b: String): String = {
    if (a.endsWith("/") && b.startsWith("/")) s"$a${b.tail}"
    else if (!a.endsWith("/") && !b.startsWith("/")) s"$a/$b"
    else s"$a$b"
  }

  def setHash(hash: String): Try[Unit] = {
    Try {
      val to = location.origin + location.pathname + location.search + "#" + hash
      history.replaceState(js.Object.apply(), "", to)
      scrollToHash(None)
    }
  }

  def scrollToHash(target: Option[HTMLElement]): Unit = {
    val hash = location.hash
    if (hash.nonEmpty && hash.startsWith("#")) {
      val id = js.Dynamic.global.decodeURIComponent(hash.drop(1)).asInstanceOf[String]
      Option(document.getElementById(id)).foreach(_.scrollIntoView())
    } else {
      target.foreach { e =>
        e.scrollTop = 0
        e.scrollLeft = 0
      }
    }
  }

  /**
   * pushStateは非同期処理なので完了後にhref値を書き換えるための措置
   */
  def startCheckHref(old: String): Unit = {

    val maxCount = 10
    val interval = 50.milliseconds

    def check(count: Int = 1): Unit = {
      window.setTimeout(() => {
        if (old != location.href) {
          href := location.href
        } else if (count < maxCount) {
          check(count + 1)
        }
      }, interval.toMillis.toDouble)
    }

    check()
  }

}
