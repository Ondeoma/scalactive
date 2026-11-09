package io.github.ondeoma.scalactive.components

import io.github.ondeoma.scalactive.controllers.NodesComponentController
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.utils.DomUtil.*
import org.scalajs.dom.*

/**
 * 中身を別の要素(bodyなど)の末尾に描画するコンポーネント(VueのTeleport、ReactのPortal).
 * モーダル・トースト・ツールチップなどを、親要素のoverflowやz-indexの影響を受けない場所に描く場合に使います.
 *
 * 元の位置には目印のコメントだけを残します. 中身はこのコンポーネントのものとして扱われるので、
 * リアクティブな表示やイベントはそのまま動き、片付けられると描画先から取り除かれます.
 *
 * .stでは `<Teleport to="body">...</Teleport>`、Scalaでは `teleportC("body") { implicit cm => ... }` と書きます.
 */
object Teleport extends BaseComponent {

  /**
   * @param to       描画先のCSSセレクター(例: "body", "#modals")
   * @param disabled trueの場合は移動せず、元の位置に描画します
   */
  def apply(to: String,
            disabled: Boolean = false,
            slot: Slot = Slot.empty)
           (implicit parent: HTMLElement,
            am: AddMethod): NodesComponentController = {
    NodesComponentController { c =>
      val root = c.parent.getOrElse(parent)
      val placeholder = document.createComment("teleport")
      // 描画先を確かめてから中身を作る(見つからない場合に、作った子コンポーネントが残らないように)
      val target: Either[String, Option[Element]] =
        if (disabled) Right(None)
        else Option(document.querySelector(to)).map(Some(_)).toRight(s"Teleport: target '$to' was not found")
      for {
        t <- target
        t4 <- ComponentManager(cm => slot()(using cm))
        // Scala 3.4.0~
        // (ns, children, tmpRs, eIds) = t4
        (ns, children, tmpRs, eIds) = t4
        _ <- addNodes(root)(am, placeholder).toRight(addNodesErrorMessage)
      } yield {
        t match {
          case Some(e) => ns.foreach(n => e.appendChild(n))
          case None => ns.reverse.foreach(n => placeholder.parentNode.insertBefore(n, placeholder.nextSibling))
        }
        // 目印を先頭にして、描き直しのときに元の位置(親要素)を覚えられるようにする
        c.nodes = placeholder :: ns
        c.children = children
        c.tmpReactives = tmpRs
        c.eventHandlers = eIds
        c
      }
    }
  }

}
