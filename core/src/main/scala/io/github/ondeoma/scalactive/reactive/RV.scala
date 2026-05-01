package io.github.ondeoma.scalactive.reactive

import io.github.ondeoma.scalactive.components.ComponentManager
import scala.annotation.publicInBinary

class RV[A] @publicInBinary(private var value: A) extends Reactive[A] {

  override def v: A = value

  def apply(v: A): Unit = {
    val old = value
    value = v
    watchers.foreach(_._2(old, v))
  }

  def :=(v: A): Unit = {
    apply(v)
  }

  def v_=(v: A): Unit = {
    apply(v)
  }

}

object RV {

  import Reactive.*

  inline def apply[A](a: A): RV[A] = {
    val rv = new RV[A](a)
    registerCM(rv)
    rv
  }

}

