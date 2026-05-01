package io.github.ondeoma.scalactive.reactive

import io.github.ondeoma.scalactive.facades.Crypto.*
import io.github.ondeoma.scalactive.utils.TypeAlias.ID
import io.github.ondeoma.scalactive.models.WatchInfo

import scala.collection.mutable

abstract class Reactive[A] {

  protected type OldV = A
  protected type NewV = A
  protected type Watcher = PartialFunction[(OldV, NewV), Unit]

  protected val watchers: mutable.Map[ID, Watcher] = mutable.Map()

  def v: A

  def apply: A = v

  def addWatcher(w: Watcher): WatchInfo = {
    val id = randomUUID()
    watchers.addOne(id -> w)
    WatchInfo(this, id)
  }

  def rmWatcher(id: ID): Unit = {
    watchers.remove(id)
  }

  def mapC[B](f: A => B): CRV[B] = {
    CRV(this, f)
  }

  def abort(): Unit = {
    watchers.clear()
  }

}

object Reactive {
  inline def registerCM(rv: Reactive[?]): Unit = {
    import io.github.ondeoma.scalactive.components.ComponentManager
    import scala.compiletime.summonFrom

    summonFrom {
      case cm: ComponentManager => cm.addTmpReactive(rv)
      case _ => ()
    }
  }
}
