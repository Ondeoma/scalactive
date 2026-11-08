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

  override def v: Unit = ()

  def runMounted(): Unit = {
    if (alive) mounted.foreach(_())
  }

  override def abort(): Unit = {
    if (alive) {
      alive = false
      unmounted.foreach(_())
    }
    super.abort()
  }

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

  private def flush(): Unit = {
    scheduled = false
    val hs = queue.toList
    queue.clear()
    hs.foreach(_.runMounted())
  }

}
