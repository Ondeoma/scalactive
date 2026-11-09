package io.github.ondeoma.scalactive.components

import io.github.ondeoma.scalactive.reactive.Reactive

import scala.collection.mutable.ListBuffer
import scala.scalajs.js

/**
 * 1つのComponentManager(コンポーネントの描画1回分)のライフサイクルフック.
 *
 * ComponentManagerの一時リアクティブ値として登録されるので、コンポーネントが片付けられる(clear)と
 * abortが呼ばれ、onUnmountedのフックを実行します. forCの行・ifCの中身なども同じ経路で片付けられます.
 */
private[scalactive] final class LifecycleHooks extends Reactive[Unit] {

  val mounted: ListBuffer[() => Unit] = ListBuffer()
  val unmounted: ListBuffer[() => Unit] = ListBuffer()

  /** 片付けられていなければtrue */
  var alive: Boolean = true

  /** 作られたときのエラーの境界. onMountedの中の例外はここへ知らせます. */
  private val boundary = ErrorBoundaryContext.current

  override def v: Unit = ()

  def runMounted(): Unit = {
    if (alive) {
      // onMountedの中で作ったwatch・computedなどは、このコンポーネントが片付けられたときに止める
      val prev = LifecycleHooks.running
      LifecycleHooks.running = this
      try ErrorBoundaryContext.guard(boundary)(mounted.foreach(_()))
      finally LifecycleHooks.running = prev
    }
  }

  /** リアクティブ値を、このコンポーネントが片付けられたときに止めるものとして登録します. */
  def adopt(r: Reactive[?]): Unit = {
    if (alive) unmounted += (() => r.abort())
    else r.abort()
  }

  override def abort(): Unit = {
    if (alive) {
      alive = false
      unmounted.foreach(_())
    }
    super.abort()
  }

}

private[scalactive] object LifecycleHooks {

  /** 実行中のonMountedのフック(実行中でなければnull) */
  var running: LifecycleHooks = null

}

/**
 * onMountedのフックを、DOMへの挿入が終わった後(現在の処理の直後のマイクロタスク)にまとめて実行します.
 * 子コンポーネントの描画は親より先に終わるので、子のフックから順に実行されます.
 */
private[scalactive] object MountQueue {

  private val queue = ListBuffer[LifecycleHooks]()
  private var scheduled = false

  def enqueue(h: LifecycleHooks): Unit = {
    queue += h
    if (!scheduled) {
      scheduled = true
      js.Dynamic.global.queueMicrotask(() => flush())
    }
  }

  /** 実行を待っているonMountedがなければtrue */
  def isIdle: Boolean = queue.isEmpty

  private def flush(): Unit = {
    scheduled = false
    val hs = queue.toList
    queue.clear()
    hs.foreach(_.runMounted())
    io.github.ondeoma.scalactive.controllers.NextTick.check()
  }

}
