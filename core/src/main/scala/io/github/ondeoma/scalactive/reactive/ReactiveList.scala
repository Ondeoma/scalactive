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

// class ReactiveList[A] @publicInBinary(private var value: List[RV[A]]) extends Reactive[List[A]] {
class ReactiveList[A](private var value: List[A]) extends Reactive[List[A]] {
  
  private val innerV: ListBuffer[A] = ListBuffer.from(value)
  
  private var oldV = innerV.toList

  override def v: List[A] = innerV.toList

  def updated(): Unit = {
    watchers.foreach(_._2(oldV, v))
    oldV = v
  }

  def apply(newV: List[A]): Unit = {
    innerV.clear()
    innerV.addAll(newV) 
    updated()
  }

  def :=(v: List[A]): Unit = {
    apply(v)
  }

  def v_=(v: List[A]): Unit = {
    apply(v)
  }
  
  def update(i: Int,
             v: A): Unit = {
    innerV(i) = v
    updated()
  }

  def add(v: A): Unit = {
    innerV.addOne(v)
    updated()
  }

  def rm(idx: Int): Unit = {
    innerV.lift(idx).foreach( _ => innerV.remove(idx))
    updated()
  }

  override def abort(): Unit = {
    watchers.clear()
    innerV.clear()
  }

}

object ReactiveList {

  import Reactive.*

  inline def apply[A](value: List[A]): ReactiveList[A] = {
    val rv = new ReactiveList[A](value)
    registerCM(rv)
    rv
  }
  
}
