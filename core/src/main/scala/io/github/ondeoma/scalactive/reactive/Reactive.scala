package io.github.ondeoma.scalactive.reactive

import io.github.ondeoma.scalactive.models.WatchInfo
import io.github.ondeoma.scalactive.utils.TypeAlias.ID

import scala.collection.mutable

abstract class Reactive[A] {

  protected type OldV = A
  protected type NewV = A
  protected type Watcher = PartialFunction[(OldV, NewV), Unit]

  protected val watchers: mutable.Map[ID, Watcher] = mutable.Map()

  def v: A

  def apply(): A = v

  def addWatcher(w: Watcher): WatchInfo = {
    val id = io.github.ondeoma.scalactive.utils.IdGen.next()
    watchers.addOne(id -> w)
    WatchInfo(this, id)
  }

  /** 監視している数(Devtools.watcherCountで使います) */
  private[scalactive] def numWatchers: Int = watchers.size

  def rmWatcher(id: ID): Unit = {
    watchers.remove(id)
  }

  // inlineにすることで、呼び出し側のComponentManagerに一時リアクティブ値として登録され、
  // コンポーネントのクリア時に監視が解除されます.
  inline def mapC[B](f: A => B): CRV[B] = {
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
