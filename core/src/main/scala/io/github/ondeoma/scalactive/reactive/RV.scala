package io.github.ondeoma.scalactive.reactive

import io.github.ondeoma.scalactive.components.ComponentManager
// Scala 3.4.0~
// import scala.annotation.publicInBinary

// class RV[A] @publicInBinary(private var value: A) extends Reactive[A] {
class RV[A](private var value: A) extends Reactive[A] {

  override def v: A = {
    Tracking.record(this)
    value
  }

  /**
   * 値を更新します. 前の値と等しい(==)場合はウォッチャーに通知しません.
   * (以前は同じ値でも通知し、ifCなどが不要に描き直されていました)
   */
  def apply(v: A): Unit = {
    val old = value
    value = v
    if (old != v) fire(old, v)
  }

  /**
   * 値を変えずにウォッチャーに通知します.
   * 可変なオブジェクトの中身を直接書き換えた後に、画面へ反映させたい場合などに使います.
   */
  def trigger(): Unit = {
    fire(value, value)
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

