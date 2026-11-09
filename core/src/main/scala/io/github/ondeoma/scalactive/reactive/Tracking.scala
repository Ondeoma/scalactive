package io.github.ondeoma.scalactive.reactive

import scala.scalajs.js

/**
 * computedの計算中に読まれたリアクティブ値(依存)を集めます.
 *
 * リアクティブ値の `v` は、読まれたときに [[record]] を呼びます. computedの計算中でなければ何もしません
 * (nullの確認1回だけなので、ふだんの読み取りはほとんど遅くなりません).
 */
private[scalactive] object Tracking {

  /** 1回の計算で読まれたリアクティブ値(読まれた順. 同じものは1回だけ) */
  final class Collector {
    val deps: js.Array[Reactive[?]] = js.Array()
    private val seen = js.Set[Reactive[?]]()

    def add(r: Reactive[?]): Unit = {
      if (!seen.contains(r)) {
        seen.add(r)
        deps.push(r)
      }
    }

    def contains(r: Reactive[?]): Boolean = seen.contains(r)
  }

  private var current: Collector = null

  def record(r: Reactive[?]): Unit = {
    if (current != null) current.add(r)
  }

  /** bodyの中で読まれたリアクティブ値をcに集めます. */
  def collect[T](c: Collector)(body: => T): T = {
    val prev = current
    current = c
    try body
    finally current = prev
  }

  /** bodyの中で読まれたリアクティブ値を、依存として集めません. */
  def untracked[T](body: => T): T = {
    if (current == null) body
    else {
      val prev = current
      current = null
      try body
      finally current = prev
    }
  }

}
