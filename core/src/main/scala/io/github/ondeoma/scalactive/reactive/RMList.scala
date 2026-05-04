package io.github.ondeoma.scalactive.reactive

import io.github.ondeoma.scalactive.facades.Crypto.*
import io.github.ondeoma.scalactive.models.WatchInfo
import io.github.ondeoma.scalactive.reactive.RMCompatible.ext.toRMs
import io.github.ondeoma.scalactive.utils.ListBufferUtil.*
import io.github.ondeoma.scalactive.utils.TypeAlias.*

// Scala 3.4.0~
// import scala.annotation.publicInBinary
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

// class RMList[A, RM <: ReactiveModel[A, RM]] @publicInBinary(private var value: List[RM]) extends Reactive[List[A]] {
class RMList[A, RM <: ReactiveModel[A, RM]](private var value: List[RM]) extends Reactive[List[A]] {

  val rowLevelWatchingIdPrefix = "RLW-"

  private val innerV: ListBuffer[RM] = ListBuffer.from(value)
  private val watchInfos: mutable.Map[Int, WatchInfos] = mutable.Map()

  watchAll()

  def addWatcherRowLevel(w: Watcher): WatchInfo = {
    val id = rowLevelWatchingIdPrefix + randomUUID()
    watchers.addOne(id -> w)
    WatchInfo(this, id)
  }

  private var oldV = fix(innerV)

  private def fix(v: ListBuffer[RM]) = v.toList.map(_.toOrigin)

  override def v: List[A] = fix(innerV)

  def rv: List[RM] = innerV.toList

  private def watchAll(): Unit = {
    // 子要素に変化があってもウォッチャーに通知
    innerV.zipWithIndex.foreach { (rmw, i) =>
      watchInfos.addOne(i -> rmw.addWatcher(() => updated(true)))
    }
  }

  def updated(isDetail: Boolean): Unit = {
    watchers.foreach { (id, wf) =>
      if (isDetail && id.startsWith(rowLevelWatchingIdPrefix)) ()
      else wf(oldV, v)
    }
    // TODO?: 行レベルの前値を別管理？
    oldV = v
  }

  def apply(newRM: List[RM]): Unit = {
    watchInfos.foreach(_._2.foreach(_.abort()))
    watchInfos.clear()

    innerV.clear()
    innerV.addAll(ListBuffer.from(newRM))

    watchAll()
    updated(false)
  }

  def applyV(i: Int): A = {
    v(i)
  }

  def liftV(i: Int): Option[A] = {
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

  def ::=(v: List[A])
         (using rmc: RMCompatible[A, RM]): Unit = {
    apply(v.toRMs)
  }

  def v_=(v: List[RM]): Unit = {
    apply(v)
  }

  def update(idx: Int,
             rm: RM): Unit = {
    watchInfos.get(idx).foreach(_.foreach(_.abort()))
    innerV.remove(idx)
    innerV.insert(idx, rm)
    watchInfos.addOne(idx -> rm.addWatcher(() => updated(true)))
    updated(true)
  }

  def up(idx: Int): Unit = {
    innerV.up(idx)
    updated(false)
  }

  def down(idx: Int): Unit = {
    innerV.down(idx)
    updated(false)
  }

  def add(rm: RM): Unit = {
    innerV.addOne(rm)
    watchInfos.addOne((innerV.length - 1) -> rm.addWatcher(() => updated(true)))
    updated(false)
  }

  def add(a: A)
         (using rmc: RMCompatible[A, RM]): RM = {
    val rm = RMCompatible.toRM(a)
    add(rm)
    rm
  }

  def rm(idx: Int): Unit = {
    innerV.lift(idx).foreach { rv =>
      // ウォッチ解除
      watchInfos.get(idx).foreach(_.foreach(_.abort()))
      watchInfos.remove(idx)
      watchInfos.filter((cidx, _) => cidx > idx).foreach { (cidx, id) =>
        watchInfos.remove(cidx)
        watchInfos.addOne(cidx - 1 -> id)
      }
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
    watchInfos.foreach(_._2.foreach(_.abort()))
    watchInfos.clear()
  }

}

object RMList {

  import Reactive.*

  inline def apply[A, RM <: ReactiveModel[A, RM]](value: List[RM]): RMList[A, RM] = {
    val rv = new RMList[A, RM](value)
    registerCM(rv)
    rv
  }

}

