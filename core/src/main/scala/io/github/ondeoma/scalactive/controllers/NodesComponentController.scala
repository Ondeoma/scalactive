package io.github.ondeoma.scalactive.controllers

import io.github.ondeoma.scalactive.reactive.*
import io.github.ondeoma.scalactive.utils.TypeAlias.*
import org.scalajs.dom.*

class NodesComponentController(val initF: NodesComponentController => Either[String, NodesComponentController],
                               var parent: Option[HTMLElement] = None,
                               var nodes: List[Node] = Nil,
                               var children: ChildrenComponents = Nil,
                               var watchInfos: WatchInfos = Nil,
                               var abortControllers: List[AbortController] = Nil,
                               var eventHandlers: List[ID] = Nil,
                               var tmpReactives: List[Reactive[?]] = Nil,
                               var onInit: NodesComponentController => Unit = _ => ()) extends NodesController[NodesComponentController] {

  /** 設定されていれば、描き直し(refresh)でclear + initの代わりに呼ばれます(forCの差分更新など) */
  var onRefresh: Option[NodesComponentController => Unit] = None

  /** 設定されていれば、clearのときに呼ばれます(forCの行の片付けなど) */
  var onClear: Option[NodesComponentController => Unit] = None

  override protected def clearExtra(): Unit = onClear.foreach(_(this))

  override def refresh(): Unit = {
    onRefresh match {
      case Some(f) => f(this)
      case None => super.refresh()
    }
  }

  /** initのたびに(onInitの後に)呼ばれる処理. 親が付け加えた属性(属性の引き継ぎ)などに使います. */
  var afterInit: List[NodesComponentController => Unit] = Nil

  def init(): Either[String, NodesComponentController] = {
    initF(this).map { c =>
      onInit(c)
      afterInit.foreach(_(c))
      c
    }
  }

}
