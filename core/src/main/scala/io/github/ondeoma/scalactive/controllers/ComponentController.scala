package io.github.ondeoma.scalactive.controllers

import io.github.ondeoma.scalactive.ScalactiveConfig
import org.scalajs.dom.{document, window}

import java.time.LocalDateTime
import scala.concurrent.duration.*
import scala.scalajs.js

trait ComponentController[CC <: ComponentController[?]] {

  def init(): Either[String, CC]

  def clear(): Unit

  private var scrollX: Double = 0
  private var scrollY: Double = 0
  protected var reloadTimer = 0

  def reload(): Unit = {
    if (reloadTimer != 0) {
      window.clearTimeout(reloadTimer)
      reloadTimer = 0
    }
    reloadTimer = window.setTimeout(() => {
      backupScrollPosition()
      clear()
      init()
      restoreScrollPosition()
    }, ScalactiveConfig.reloadTimerLazyDuration.toMillis.toDouble)
  }

  def backupScrollPosition(): Unit = {
    if (ScalactiveConfig.keepWindowScrollPositionWhenReload) {
      scrollX = window.scrollX
      scrollY = window.scrollY
    }
  }

  def restoreScrollPosition(): Unit = {
    if (ScalactiveConfig.keepWindowScrollPositionWhenReload) {
      window.scrollTo(scrollX.toInt, scrollY.toInt)
    }
  }


}
