package io.github.ondeoma.scalactive.controllers.events

import cats.syntax.all.*
import io.github.ondeoma.scalactive.controllers.ComponentController
import io.github.ondeoma.scalactive.enums.EventType
import io.github.ondeoma.scalactive.reactive.*
import io.github.ondeoma.scalactive.utils.DomUtil.*
import io.github.ondeoma.scalactive.utils.TypeAlias.*
import org.scalajs.dom.*

import scala.util.control.NonFatal

class EventController(val initF: EventController => Either[String, EventController],
                      var element: Option[HTMLElement] = None,
                      var abortController: Option[AbortController] = None) extends ComponentController[EventController] {

  def init(): Either[String, EventController] = initF(this)

  /** リスナーを外す処理(EventController.applyで登録したもの) */
  private[scalactive] var removeListener: Option[HTMLElement => Unit] = None

  def clear(): Unit = {
    // このclearの連鎖でDOMから取り除いた要素なら、リスナーを外さない(要素ごと捨てられるため).
    // 1,000行の全削除では、リスナーを外す処理(AbortController.abort)が時間の8割ほどを占めていました.
    val detached = element.exists(io.github.ondeoma.scalactive.controllers.ClearScope.isRemoved)
    if (!detached) {
      for {
        e <- element
        r <- removeListener
      } r(e)
      abortController.foreach(_.abort())
    }
    element = None
    abortController = None
    removeListener = None
  }

}

object EventController {

  def apply(ele: HTMLElement,
            et: EventType,
            handler: Event => Unit): EventController = {
    new EventController(ec =>
      // ハンドラーの中の例外は、エラーの境界(なければScalactiveConfig.errorHandler)へ
      val guarded: Event => Unit = e => {
        try handler(e)
        catch {
          case NonFatal(ex) => io.github.ondeoma.scalactive.components.ErrorBoundaryContext.report(ec.boundary, ex)
        }
      }
      // AbortControllerを作らず、外すときはremoveEventListenerで(作る・外す手間を減らすため)
      val listener: scala.scalajs.js.Function1[Event, Unit] = guarded
      val name = et.toString
      ele.addEventListener(name, listener)
      ec.element = ele.some
      ec.removeListener = Some(e => e.removeEventListener(name, listener))
      Right(ec)
    )
  }

  def apply(et: EventType,
            handler: Event => Unit): HTMLElement => EventController = {
    apply(_, et, handler)
  }

}
