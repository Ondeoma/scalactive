package io.github.ondeoma.scalactive.reactive

import io.github.ondeoma.scalactive.models.WatchInfo
import io.github.ondeoma.scalactive.reactive.RMCompatible.ext.*
import io.github.ondeoma.scalactive.utils.ListBufferUtil.*

import scala.annotation.targetName

// Scala 3.4.0~
// import scala.annotation.publicInBinary
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

// class RMList[Org, RM <: ReactiveModel[A, RM]] @publicInBinary(private var value: List[RM]) extends Reactive[List[A]] {
class RMList[Org, RM <: ReactiveModel[Org, RM]](private var value: List[RM]) extends Reactive[List[Org]] {

  val rowLevelWatchingIdPrefix = "RLW-"

  private val innerV: ListBuffer[RM] = ListBuffer.from(value)
  /** 各要素への監視. innerVと同じ順序・同じ長さに保ちます. */
  private val watchInfos: ArrayBuffer[WatchInfo] = ArrayBuffer()

  watchAll()

  def addWatcherRowLevel(w: Watcher): WatchInfo = {
    val id = rowLevelWatchingIdPrefix + io.github.ondeoma.scalactive.utils.IdGen.next()
    watchers.addOne(id -> w)
    WatchInfo(this, id)
  }

  private var oldV = fix(innerV)

  private def fix(v: ListBuffer[RM]) = v.toList.map(_.toOrigin)

  // 行のモデルは依存に入れない(このリストが行の変化も通知するため)
  override def v: List[Org] = {
    Tracking.record(this)
    Tracking.untracked(fix(innerV))
  }

  def rv: List[RM] = innerV.toList

  private def watchAll(): Unit = {
    watchInfos.addAll(innerV.view.map(watchRow))
  }

  /** 子要素に変化があってもウォッチャーに通知する */
  private def watchRow(rm: RM): WatchInfo = rm.addWatcher(_ => updated(true))

  def updated(isDetail: Boolean): Unit = {
    fire(oldV, v, id => isDetail && id.startsWith(rowLevelWatchingIdPrefix))
    // TODO?: 行レベルの前値を別管理？
    oldV = v
  }

  def apply(newRM: List[RM]): Unit = {
    watchInfos.foreach(_.abort())
    watchInfos.clear()

    innerV.clear()
    innerV.addAll(ListBuffer.from(newRM))

    watchAll()
    updated(false)
  }

  def apply(news: List[Org])
           (using rmc: RMCompatible[Org, RM]): List[RM] = {
    val rms = news.toRMs
    apply(rms)
    rms
  }

  def applyV(i: Int): Org = {
    v(i)
  }

  def liftV(i: Int): Option[Org] = {
    v.lift(i)
  }

  def apply(i: Int): RM = {
    rv(i)
  }

  def lift(i: Int): Option[RM] = {
    rv.lift(i)
  }

  def :=(v: List[RM]): Unit = {
    apply(v)
  }

  def :=(v: List[Org])
        (using rmc: RMCompatible[Org, RM]): List[RM] = {
    apply(v)
  }
  
  def v_=(v: List[RM]): Unit = {
    apply(v)
  }

  def v_=(v: List[Org])
         (using rmc: RMCompatible[Org, RM]): List[RM] = {
    apply(v)
  }

  def update(idx: Int,
             rm: RM): Unit = {
    watchInfos(idx).abort()
    innerV(idx) = rm
    watchInfos(idx) = watchRow(rm)
    updated(true)
  }

  def update(idx: Int,
             org: Org)
            (using rmc: RMCompatible[Org, RM]): RM = {
    val rm = org.toRM
    update(idx, rm)
    rm
  }

  def up(idx: Int): Unit = {
    innerV.up(idx)
    watchInfos.up(idx)
    updated(false)
  }

  def down(idx: Int): Unit = {
    innerV.down(idx)
    watchInfos.down(idx)
    updated(false)
  }

  def add(rm: RM): Unit = {
    innerV.addOne(rm)
    watchInfos.addOne(watchRow(rm))
    updated(false)
  }

  def add(org: Org)
         (using rmc: RMCompatible[Org, RM]): RM = {
    val rm = RMCompatible.toRM(org)
    add(rm)
    rm
  }

  def rm(idx: Int): Unit = {
    if (idx >= 0 && idx < innerV.length) {
      watchInfos(idx).abort()
      watchInfos.remove(idx)
      innerV.remove(idx)
    }
    updated(false)
  }

  def length: Int = {
    innerV.length
  }

  override def abort(): Unit = {
    watchers.clear()
    innerV.clear()
    watchInfos.foreach(_.abort())
    watchInfos.clear()
  }

}

object RMList {

  import Reactive.*

  @targetName("applyByRVs")
  inline def apply[Org, RM <: ReactiveModel[Org, RM]](value: List[RM]): RMList[Org, RM] = {
    val rv = new RMList[Org, RM](value)
    registerCM(rv)
    rv
  }

  @targetName("applyByRaws")
  inline def apply[Org, RM <: ReactiveModel[Org, RM]](values: List[Org])
                                                     (using RMCompatible[Org, RM]): RMList[Org, RM] = {
    apply(values.map(RMCompatible.toRM))
  }

}

