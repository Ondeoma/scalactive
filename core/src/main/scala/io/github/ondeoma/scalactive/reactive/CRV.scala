package io.github.ondeoma.scalactive.reactive

import Reactive.registerCM
import io.github.ondeoma.scalactive.syntax.All.*
import io.github.ondeoma.scalactive.models.WatchInfo

trait CRV[R] extends Reactive[R] {

  protected var value: R

  protected var watchInfos: List[WatchInfo] = Nil

  private def update(newV: R): Unit = {
    val old = value
    value = newV
    watchers.foreach(_._2(old, newV))
  }

  override def abort(): Unit = {
    watchInfos.foreach(_.abort())
    watchInfos = Nil
    watchers.clear()
  }

}

object CRV {

  private def _apply[A, R](r1: Reactive[A],
                           f: A => R): CRV[R] = {
    val calc = () => f(r1.v)
    val r = new CRV[R] {
      var value: R = calc()

      override def v: R = value
    }
    r.watchInfos = List(
      r1.addWatcher(_ => r.update(calc()))
    )
    r
  }

  inline def apply[A, R](r1: Reactive[A],
                         f: A => R): CRV[R] = {
    val crv = _apply(r1, f)
    registerCM(crv)
    crv
  }


  private def apply_[A, B, R](r1: Reactive[A],
                              r2: Reactive[B],
                              f: (A, B) => R): CRV[R] = {
    val calc = () => f(r1.v, r2.v)
    val r = new CRV[R] {
      var value: R = calc()

      override def v: R = value
    }
    r.watchInfos = List(r1, r2).map {
      _.addWatcher(_ => r.update(calc()))
    }
    r
  }

  inline def apply[A, B, R](r1: Reactive[A],
                            r2: Reactive[B],
                            f: (A, B) => R): CRV[R] = {
    val crv = apply_(r1, r2, f)
    registerCM(crv)
    crv
  }

  private def apply_[A, B, C, R](r1: Reactive[A],
                                 r2: Reactive[B],
                                 r3: Reactive[C],
                                 f: (A, B, C) => R): CRV[R] = {
    val calc = () => f(r1.v, r2.v, r3.v)
    val r = new CRV[R] {
      var value: R = calc()

      override def v: R = value
    }
    r.watchInfos = List(r1, r2, r3).map {
      _.addWatcher(_ => r.update(calc()))
    }
    r
  }

  inline def apply[A, B, C, R](r1: Reactive[A],
                               r2: Reactive[B],
                               r3: Reactive[C],
                               f: (A, B, C) => R): CRV[R] = {
    val crv = apply(r1, r2, r3, f)
    registerCM(crv)
    crv
  }

  private def apply_[A, B, C, D, R](r1: Reactive[A],
                                    r2: Reactive[B],
                                    r3: Reactive[C],
                                    r4: Reactive[D],
                                    f: (A, B, C, D) => R): CRV[R] = {
    val calc = () => f(r1.v, r2.v, r3.v, r4.v)
    val r = new CRV[R] {
      var value: R = calc()

      override def v: R = value
    }
    r.watchInfos = List(r1, r2, r3, r4).map {
      _.addWatcher(_ => r.update(calc()))
    }
    r
  }

  inline def apply[A, B, C, D, R](r1: Reactive[A],
                                  r2: Reactive[B],
                                  r3: Reactive[C],
                                  r4: Reactive[D],
                                  f: (A, B, C, D) => R): CRV[R] = {
    val crv = apply_(r1, r2, r3, r4, f)
    registerCM(crv)
    crv
  }

  private def apply_[A, B, C, D, E, R](r1: Reactive[A],
                                       r2: Reactive[B],
                                       r3: Reactive[C],
                                       r4: Reactive[D],
                                       r5: Reactive[E],
                                       f: (A, B, C, D, E) => R): CRV[R] = {
    val calc = () => f(r1.v, r2.v, r3.v, r4.v, r5.v)
    val r = new CRV[R] {
      var value: R = calc()

      override def v: R = value
    }
    r.watchInfos = List(r1, r2, r3, r4, r5).map {
      _.addWatcher(_ => r.update(calc()))
    }
    r
  }

  inline def apply[A, B, C, D, E, R](r1: Reactive[A],
                                     r2: Reactive[B],
                                     r3: Reactive[C],
                                     r4: Reactive[D],
                                     r5: Reactive[E],
                                     f: (A, B, C, D, E) => R): CRV[R] = {
    val crv = apply_(r1, r2, r3, r4, r5, f)
    registerCM(crv)
    crv
  }

  private def apply_[A, B, C, D, E, F, R](r1: Reactive[A],
                                          r2: Reactive[B],
                                          r3: Reactive[C],
                                          r4: Reactive[D],
                                          r5: Reactive[E],
                                          r6: Reactive[F],
                                          f: (A, B, C, D, E, F) => R): CRV[R] = {
    val calc = () => f(r1.v, r2.v, r3.v, r4.v, r5.v, r6.v)
    val r = new CRV[R] {
      var value: R = calc()

      override def v: R = value
    }
    r.watchInfos = List(r1, r2, r3, r4, r5, r6).map {
      _.addWatcher(_ => r.update(calc()))
    }
    r
  }

  inline def apply[A, B, C, D, E, F, R](r1: Reactive[A],
                                        r2: Reactive[B],
                                        r3: Reactive[C],
                                        r4: Reactive[D],
                                        r5: Reactive[E],
                                        r6: Reactive[F],
                                        f: (A, B, C, D, E, F) => R): CRV[R] = {
    val crv = apply_(r1, r2, r3, r4, r5, r6, f)
    registerCM(crv)
    crv
  }

}

