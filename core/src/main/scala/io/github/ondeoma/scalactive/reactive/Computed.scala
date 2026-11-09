package io.github.ondeoma.scalactive.reactive

import io.github.ondeoma.scalactive.models.WatchInfo

import scala.scalajs.js

/**
 * 計算中に読んだリアクティブ値を自動で監視するCRV(`computed { ... }` で作ります).
 *
 * 計算し直すたびに依存を集め直すので、条件によって読む値が変わる場合も、その時点で読んだものだけを監視します.
 * 計算し直した値が前の値と等しい(==)場合は通知しません(CRVと同じ).
 */
private[scalactive] final class Computed[R](calc: () => R) extends CRV[R] {

  private var aborted = false

  /** 監視している依存(前回の計算で読んだ順)と、その監視 */
  private var deps: js.Array[Reactive[?]] = js.Array()
  private var infos: js.Array[WatchInfo] = js.Array()

  protected var value: R = run()

  private def run(): R = {
    val c = new Tracking.Collector
    val r = Tracking.collect(c)(calc())
    if (!aborted) resubscribe(c)
    r
  }

  /** 前回の依存と比べて、読まなくなったものの監視を解除し、新しく読んだものを監視します. */
  private def resubscribe(c: Tracking.Collector): Unit = {
    val nextDeps = c.deps
    val nextInfos = js.Array[WatchInfo]()
    var i = 0
    while (i < deps.length) {
      if (!c.contains(deps(i))) infos(i).abort()
      i += 1
    }
    val prev = js.Map[Reactive[?], WatchInfo]()
    i = 0
    while (i < deps.length) {
      prev.update(deps(i), infos(i))
      i += 1
    }
    nextDeps.foreach { d =>
      nextInfos.push(prev.getOrElse(d, d.addWatcher(_ => changed())))
    }
    deps = nextDeps
    infos = nextInfos
  }

  private def changed(): Unit = {
    if (!aborted) update(run())
  }

  override def abort(): Unit = {
    aborted = true
    infos.foreach(_.abort())
    deps = js.Array()
    infos = js.Array()
    super.abort()
  }

}

object Computed {

  /** computedの実体を作ります(ComponentManagerへの登録は呼び出し側で行います). */
  def make[R](calc: () => R): CRV[R] = new Computed(calc)

}
