package io.github.ondeoma.scalactive.reactive

import io.github.ondeoma.scalactive.models.*
import io.github.ondeoma.scalactive.reactive.RVList.ext.*
import io.github.ondeoma.scalactive.utils.ListBufferUtil.*

import scala.annotation.targetName

// Scala 3.4.0~
// import scala.annotation.publicInBinary
import scala.collection.mutable.ArrayBuffer

// class RVList[A] @publicInBinary(private var value: List[RV[A]]) extends Reactive[List[A]] {
class RVList[A](private var value: List[RV[A]]) extends Reactive[List[A]] {

  val rowLevelWatchingIdPrefix = "RLW-"

  // 添字で読み書きするのでArrayBuffer(ListBufferは添字アクセスが先頭からたどるO(n))
  private val innerV: ArrayBuffer[RV[A]] = ArrayBuffer.from(value)

  /**
   * 各要素(RV)への監視. innerVと同じ順序・同じ長さに保ちます.
   * (以前は添字をキーにしたMapで、削除や並べ替えの後に別の要素の監視を解除したり、解除し損ねたりしていました)
   */
  private val watchInfos: ArrayBuffer[WatchInfo] = ArrayBuffer.from(innerV.view.map(watchRow))

  /** apply/updateで要素をまとめて書き換えている間は、要素ごとの通知をしない(最後に1回だけ通知する) */
  private var batching = false

  /** 子要素に変化があってもウォッチャーに通知する */
  private def watchRow(rv: RV[A]): WatchInfo = rv.addWatcher(_ => if (!batching) updated(true))

  private def inBatch(f: => Unit): Unit = {
    val outer = batching
    batching = true
    try f
    finally batching = outer
  }

  def addWatcherRowLevel(w: Watcher): WatchInfo = {
    val id = rowLevelWatchingIdPrefix + io.github.ondeoma.scalactive.utils.IdGen.next()
    watchers.addOne(id -> w)
    WatchInfo(this, id)
  }

  private var oldV = fix(innerV)

  private def fix(v: ArrayBuffer[RV[A]]): List[A] = v.iterator.map(_.v).toList

  // 行のRVは依存に入れない(このリストが行の変化も通知するため)
  override def v: List[A] = {
    Tracking.record(this)
    Tracking.untracked(fix(innerV))
  }

  def rv: List[RV[A]] = innerV.toList

  private def updated(isDetail: Boolean): Unit = {
    val newV = v
    watchers.foreach { (id, wf) =>
      if (isDetail && id.startsWith(rowLevelWatchingIdPrefix)) ()
      else wf(oldV, newV)
    }
    // TODO?: 行レベルの前値を別管理？
    oldV = newV
  }

  def apply(newV: List[RV[A]]): Unit = {
    val nowL = innerV.length
    val newL = newV.length

    // 更新分
    // (Listの添字アクセスは先頭からたどるので、順に読む)
    // 要素ごとの通知はせず、最後に1回だけ通知する(以前はn件の更新でn回通知していました)
    inBatch {
      newV.iterator.take(nowL).zipWithIndex.foreach { (rv, i) =>
        innerV(i) := rv.v
      }
    }

    // 追加分
    val forAdd = newV.drop(nowL)
    forAdd.foreach { a =>
      innerV.addOne(a)
      watchInfos.addOne(watchRow(a))
    }

    // 削除分(newL番目以降)の監視を解除
    if (nowL > newL) {
      (newL until nowL).foreach(i => watchInfos(i).abort())
      watchInfos.remove(newL, nowL - newL)
      innerV.remove(newL, nowL - newL)
    }

    updated(false)
  }
  
  def apply(newV: List[A]): List[RV[A]] = {
    val rvs = newV.toRVs 
    apply(rvs)
    rvs
  }
  
  def applyV(i: Int): A = {
    v(i)
  }

  def liftV(i: Int): Option[A] = {
    v.lift(i)
  }

  @targetName("applyByRVs")
  def :=(v: List[RV[A]]): Unit = {
    apply(v)
  }

  @targetName("applyByRaws")
  def :=(v: List[A]): Unit = {
    apply(v)
  }
  
  def v_=(v: List[RV[A]]): Unit = {
    apply(v)
  }
  
  def v_=(v: List[A]): List[RV[A]] = {
    apply(v)
  }

  def update(i: Int,
             v: A): Unit = {
    // 要素の監視からの通知と合わせて2回通知しないようにする
    inBatch(innerV(i) := v)
    updated(true)
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

  def add(rv: RV[A]): Unit = {
    innerV.addOne(rv)
    watchInfos.addOne(watchRow(rv))
    updated(false)
  }

  def add(a: A): RV[A] = {
    val rv = RV(a)
    add(rv)
    rv
  }
  
  def rm(idx: Int): Unit = {
    if (idx >= 0 && idx < innerV.length) {
      watchInfos(idx).abort()
      watchInfos.remove(idx)
      innerV.remove(idx)
    }
    updated(false)
  }

  override def abort(): Unit = {
    watchers.clear()
    innerV.clear()
    watchInfos.foreach(_.abort())
    watchInfos.clear()
  }

}

object RVList {

  import Reactive.*

  @targetName("applyByRVs")
  inline def apply[A](value: List[RV[A]]): RVList[A] = {
    val rv = new RVList[A](value)
    registerCM(rv)
    rv
  }

  @targetName("applyByRaws")
  inline def apply[A](value: List[A]): RVList[A] = {
    apply(toRVs(value))
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
