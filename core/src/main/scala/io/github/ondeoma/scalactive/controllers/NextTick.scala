package io.github.ondeoma.scalactive.controllers

import io.github.ondeoma.scalactive.components.MountQueue

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Future, Promise}
import scala.scalajs.js

/**
 * 予約された描き直しとonMountedが、すべて終わるのを待ちます(VueのnextTick).
 *
 * 描き直し(ReloadScheduler)とonMounted(MountQueue)が終わるたびに [[check]] が呼ばれ、
 * どちらにも待っているものがなくなった時点で、待っているFutureを完了させます.
 * (reloadTimerLazyDurationで描き直しを遅らせている場合は、その描き直しが終わるまで待ちます)
 */
private[scalactive] object NextTick {

  private val waiting = ListBuffer[Promise[Unit]]()
  private var checkQueued = false

  def apply(): Future[Unit] = {
    val p = Promise[Unit]()
    waiting += p
    check()
    p.future
  }

  /** 次のマイクロタスクで、待っているものがなければ完了させます. */
  def check(): Unit = {
    if (waiting.nonEmpty && !checkQueued) {
      checkQueued = true
      js.Dynamic.global.queueMicrotask(() => {
        checkQueued = false
        // 待っているものがあれば、それが終わったときにもう一度checkされる
        if (ReloadScheduler.isIdle && MountQueue.isIdle) {
          val ps = waiting.toList
          waiting.clear()
          ps.foreach(_.trySuccess(()))
        }
      })
    }
  }

}
