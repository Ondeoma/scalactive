package io.github.ondeoma.scalactive.reactive

import io.github.ondeoma.scalactive.components.ComponentManager
import io.github.ondeoma.scalactive.facades.Crypto.*
import io.github.ondeoma.scalactive.models.*
import io.github.ondeoma.scalactive.reactive.RVList.ext.*
import io.github.ondeoma.scalactive.utils.TypeAlias.*

// Scala 3.4.0~
// import scala.annotation.publicInBinary
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

// class RVList[A] @publicInBinary(private var value: List[RV[A]]) extends Reactive[List[A]] {
class RVList[A](private var value: List[RV[A]]) extends Reactive[List[A]] {

  val rowLevelWatchingIdPrefix = "RLW-"

  private val innerV: ListBuffer[RV[A]] = ListBuffer.from(value)
  private val watchInfos: mutable.Map[Int, WatchInfo] = mutable.Map()

  // 子要素ウォッチャーとして登録
  innerV.zipWithIndex.foreach { (rv, i) =>
    watchInfos.addOne(i -> rv.addWatcher(_ => updated(true)))
  }

  def addWatcherRowLevel(w: Watcher): WatchInfo = {
    val id = rowLevelWatchingIdPrefix + randomUUID()
    watchers.addOne(id -> w)
    WatchInfo(this, id)
  }

  private var oldV = fix(innerV)

  private def fix(v: ListBuffer[RV[A]]) = v.toList.map(_.v)

  override def v: List[A] = fix(innerV)

  def rv: List[RV[A]] = innerV.toList

  def updated(isDetail: Boolean): Unit = {
    watchers.foreach { (id, wf) =>
      if (isDetail && id.startsWith(rowLevelWatchingIdPrefix)) ()
      else wf(oldV, v)
    }
    // TODO?: 行レベルの前値を別管理？
    oldV = v
  }

  def apply(newV: List[RV[A]]): Unit = {
    val nowL = innerV.length
    val newL = newV.length

    // 更新分
    val forUpdate = newV.take(nowL)
    forUpdate.indices.foreach { i =>
      innerV(i) := forUpdate(i).v
    }

    // 追加分
    val forAdd = newV.drop(nowL)
    forAdd.foreach { a =>
      innerV.addOne(a)
      // 子要素に変化があってもウォッチャーに通知
      watchInfos.addOne((innerV.length - 1) -> a.addWatcher(_ => updated(true)))
    }

    // 削除分
    val forDel = innerV.drop(nowL - newL)
    forDel.zipWithIndex.foreach { (rv, i) =>
      val idx = i + newL
      // ウォッチ解除
      watchInfos.get(idx).foreach(_.abort())
    }
    watchInfos.filterInPlace((idx, _) => idx < newL)
    if (nowL - newL >= 1) {
      innerV.remove(newV.length, nowL - newL)
    }

    updated(false)
  }

  def :=(v: List[RV[A]]): Unit = {
    apply(v)
  }

  def v_=(v: List[RV[A]]): Unit = {
    apply(v)
  }

  def ::=(v: List[A]): Unit = {
    apply(v.toRVs)
  }

  def update(i: Int,
             v: A): Unit = {
    innerV(i) := v
    updated(true)
  }

  def add(rv: RV[A]): Unit = {
    innerV.addOne(rv)
    watchInfos.addOne((innerV.length - 1) -> rv.addWatcher(_ => updated(true)))
    updated(false)
  }

  def rm(idx: Int): Unit = {
    innerV.lift(idx).foreach { rv =>
      // ウォッチ解除
      watchInfos.get(idx).foreach(_.abort())
      watchInfos.remove(idx)
      watchInfos.filter((cidx, _) => cidx > idx).foreach { (cidx, id) =>
        watchInfos.remove(cidx)
        watchInfos.addOne(cidx - 1 -> id)
      }
      innerV.remove(idx)
    }
    updated(false)
  }

  override def abort(): Unit = {
    watchers.clear()
    innerV.clear()
    watchInfos.foreach(_._2.abort())
    watchInfos.clear()
  }

}

object RVList {

  import Reactive.*

  inline def apply[A](value: List[RV[A]]): RVList[A] = {
    val rv = new RVList[A](value)
    registerCM(rv)
    rv
  }

  def toRVs[A](as: List[A]): List[RV[A]] = as.map(a => RV(a))

  def toRVList[A](as: List[A]): RVList[A] = RVList(toRVs(as))

  object ext {
    extension [A](list: List[A]) {
      def toRVs: List[RV[A]] = list.map(a => RV(a))
      def toRVList: RVList[A] = RVList(toRVs)
    }
  }
}
