package io.github.ondeoma.scalactive.events


import io.github.ondeoma.scalactive.facades.Crypto.randomUUID
import io.github.ondeoma.scalactive.utils.DomUtil.*
import io.github.ondeoma.scalactive.utils.TypeAlias.*

import scala.collection.mutable

object EventManager {

  trait TypedEvent

  type Handler = PartialFunction[TypedEvent, Unit]

  private object State {
    val handlers: mutable.Map[ID, Handler] = mutable.Map()
  }

  def fire[A](ev: TypedEvent): Unit = {
    State.handlers.foreach { (_, h) =>
      h.applyOrElse(ev, _ => ())
    }
  }

  inline def registerCM(id: ID): Unit = {
    import io.github.ondeoma.scalactive.components.ComponentManager

    import scala.compiletime.summonFrom

    summonFrom {
      case cm: ComponentManager => cm.addEventHandlers(id)
      case _ => ()
    }
  }

  def addHandler(h: Handler): ID = {
    val id = randomUUID()
    State.handlers.addOne(id -> h)
    registerCM(id)
    id
  }

  def rmHandler(id: ID): Unit = {
    State.handlers.remove(id)
  }

}
