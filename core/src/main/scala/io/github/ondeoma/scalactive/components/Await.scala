package io.github.ondeoma.scalactive.components

import io.github.ondeoma.scalactive.controllers.NodesComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.utils.DomUtil.*
import io.github.ondeoma.scalactive.utils.TypeAlias.GenComponentF
import org.scalajs.dom.*

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

/**
 * 非同期の処理(Future)の状態に応じて表示を切り替えるコンポーネント(VueのSuspense).
 *
 * 描画されたときにloadを呼び、完了するまでpending、完了したらslot(結果を受け取るスコープ付きスロット)、
 * 失敗したらerror(ErrorInfo. retryでloadをもう1度呼ぶ)を表示します.
 * 完了前に片付けられた場合は、完了しても描き直しません.
 *
 * .stでは `<Await :load="() => fetchUser(id)">` と `<template #pending>` / `<template #default="user">` / `<template #error="err">`、
 * Scalaでは `awaitC(fetchUser(id))(user => cm => ...)` と書きます.
 */
object Await extends BaseComponent {

  private sealed trait State[+A]

  private case object Pending extends State[Nothing]

  private final case class Done[A](result: Try[A]) extends State[A]

  def apply[A](load: () => Future[A],
               pending: Slot = Slot.empty,
               error: ScopedSlot[ErrorInfo] = ScopedSlot.empty,
               slot: ScopedSlot[A] = ScopedSlot.empty)
              (implicit parent: HTMLElement,
               am: AddMethod): NodesComponentController = {
    var state: State[A] = Pending
    // 読み込みのたびに増やし、古い読み込みの結果を捨てる
    var generation = 0
    // 描画されている間だけtrue(片付けられた後に完了しても描き直さない)
    var active = false
    var controller: NodesComponentController = null

    def start(): Unit = {
      generation += 1
      val g = generation
      state = Pending
      val f = try load() catch { case NonFatal(e) => Future.failed(e) }
      f.onComplete { r =>
        if (g == generation) {
          state = Done(r)
          if (active) controller.reload()
        }
      }
    }

    val retry = () => {
      start()
      controller.reload()
    }

    controller = NodesComponentController { c =>
      val root = c.parent.getOrElse(parent)
      active = true
      val content: ComponentManager => HTML = state match {
        case Pending => cm => pending()(using cm)
        case Done(Success(a)) => cm => slot(a)()(using cm)
        case Done(Failure(e)) => cm =>
          val info = ErrorInfo(e, retry)
          error(info) { _ =>
            s"""<div class="scalactive-error">Error: ${io.github.ondeoma.scalactive.st.StRuntime.escape(info.message)}</div>"""
          }(using cm)
      }
      for {
        t4 <- ComponentManager(content)
        // Scala 3.4.0~
        // (ns, children, tmpRs, eIds) = t4
        (ns, children, tmpRs, eIds) = t4
        fixedNs = ns.orDummyNode
        _ <- addNodes(root)(am, fixedNs *).toRight(addNodesErrorMessage)
      } yield {
        c.nodes = fixedNs
        c.children = children
        c.tmpReactives = tmpRs
        c.eventHandlers = eIds
        c
      }
    }
    controller.onClear = Some(_ => active = false)
    start()
    controller
  }

  /**
   * 読み込みが終わってから描画するコンポーネント(遅延読み込み).
   * Scala.jsの `js.dynamicImport` と組み合わせると、コンポーネントのコードを別のファイルに分割できます.
   * 例: `asyncComponent(() => js.dynamicImport(HeavyComponent()).toFuture)`
   */
  def component(load: () => Future[GenComponentF],
                pending: Slot = Slot.empty,
                error: ScopedSlot[ErrorInfo] = ScopedSlot.empty): GenComponentF = {
    apply[GenComponentF](load, pending, error, ScopedSlot(f => cm => io.github.ondeoma.scalactive.components.ComponentManager.c(f)(using cm)))(_, _)
  }

}
