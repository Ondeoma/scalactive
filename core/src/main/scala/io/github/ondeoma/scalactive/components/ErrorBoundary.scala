package io.github.ondeoma.scalactive.components

import io.github.ondeoma.scalactive.controllers.NodesComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.utils.ConsoleUtil
import io.github.ondeoma.scalactive.utils.DomUtil.*
import org.scalajs.dom.*

import scala.util.control.NonFatal

/**
 * エラーの境界で受け止めたエラー.
 *
 * @param retry 中身を描き直します(もう1度試す)
 */
final case class ErrorInfo(error: Throwable,
                           retry: () => Unit) {

  def message: String = Option(error.getMessage).getOrElse(error.toString)

}

/** エラーの境界への通知先 */
private[scalactive] final class BoundaryHandle(val capture: Throwable => Unit)

/**
 * 描画中のエラーの境界. コントローラーやライフサイクルフックは、作られたときの境界を覚えておき、
 * 後で(描き直し・onMountedで)起きた例外をその境界に知らせます.
 */
private[scalactive] object ErrorBoundaryContext {

  private var current0: Option[BoundaryHandle] = None

  def current: Option[BoundaryHandle] = current0

  /** fの間、境界をhにする(fの中で作られたコントローラーはhを覚える) */
  def within[A](h: Option[BoundaryHandle])(f: => A): A = {
    val saved = current0
    current0 = h
    try f
    finally current0 = saved
  }

  /** fを実行し、例外が起きたら境界に知らせる(境界がなければログに出す) */
  def guard(h: Option[BoundaryHandle])(f: => Unit): Unit = {
    try within(h)(f)
    catch {
      case NonFatal(e) =>
        h match {
          case Some(b) => b.capture(e)
          case None => ConsoleUtil.error(e)
        }
    }
  }

}

/**
 * 子孫のコンポーネントで起きたエラーを受け止め、代わりの表示(fallback)に切り替えるコンポーネント
 * (ReactのError Boundary、VueのerrorCaptured).
 *
 * 次の例外を受け止めます.
 *  - 中身を描く途中の例外(子コンポーネントの初期化を含む)
 *  - 後から起きる子孫の描き直し(ifC・forC・リアクティブな値の変化など)の中の例外
 *  - 子孫のonMountedの中の例外
 * イベントハンドラーの中の例外は受け止めません.
 *
 * .stでは `<ErrorBoundary>...<template #fallback="err">...</template></ErrorBoundary>`、
 * Scalaでは `errorBoundaryC(err => cm => ...) { cm => ... }` と書きます.
 */
object ErrorBoundary extends BaseComponent {

  private def defaultLog(e: Throwable): Unit = ConsoleUtil.error(e)

  /**
   * @param fallback エラーのときの表示. 受け取ったErrorInfoでエラーの内容や再試行を使えます
   * @param onError  エラーを受け止めたときに呼ばれます(既定ではコンソールに出力)
   * @param slot     中身
   */
  def apply(fallback: ScopedSlot[ErrorInfo] = ScopedSlot.empty,
            onError: Throwable => Unit = defaultLog,
            slot: Slot = Slot.empty)
           (implicit parent: HTMLElement,
            am: AddMethod): NodesComponentController = {
    var error: Option[Throwable] = None
    var controller: NodesComponentController = null
    val handle = new BoundaryHandle(e => {
      error = Some(e)
      onError(e)
      controller.reload()
    })
    val retry = () => {
      error = None
      controller.reload()
    }

    def renderFallback(e: Throwable) = {
      ComponentManager { cm =>
        fallback(ErrorInfo(e, retry)) { _ =>
          s"""<div class="scalactive-error">Error: ${io.github.ondeoma.scalactive.st.StRuntime.escape(ErrorInfo(e, retry).message)}</div>"""
        }(using cm)
      }
    }

    controller = NodesComponentController { c =>
      val root = c.parent.getOrElse(parent)
      val result = error match {
        case Some(e) => renderFallback(e)
        case None =>
          try {
            ErrorBoundaryContext.within(Some(handle))(ComponentManager(cm => slot()(using cm))) match {
              case Left(message) => throw new IllegalStateException(message)
              case r => r
            }
          } catch {
            case NonFatal(e) =>
              error = Some(e)
              onError(e)
              renderFallback(e)
          }
      }
      for {
        t4 <- result
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
    controller
  }

}
