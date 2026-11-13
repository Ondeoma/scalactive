package io.github.ondeoma.scalactive.st

import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager}
import io.github.ondeoma.scalactive.components.ComponentManager as CM
import io.github.ondeoma.scalactive.controllers.NodesComponentController
import io.github.ondeoma.scalactive.reactive.{Reactive, Tracking}
import io.github.ondeoma.scalactive.utils.TypeAlias.*

/**
 * propsの式でリアクティブ値を読んでいる子コンポーネント(`<Child :title="s.item.v.name" />`).
 * 読んだ値が変わったら、propsを計算し直して子を作り直します(子の状態は初期化されます. Vueで :key が変わった場合と同じ).
 *
 * @param first     親の描画で最初に作った子と、そのときに読んだ値(1回目はこれを使い、propsを2回計算しない)
 * @param make      propsを計算して子を作る関数. 作り直すときは、このコンポーネントのComponentManagerで呼びます
 *                  (Reactive型のpropsのcomputedが、作り直すたびに片付けられるように)
 * @param attrs     子のルート要素に付け加える属性(属性の引き継ぎ)
 */
private[scalactive] object PropsComponent extends BaseComponent {

  def apply(first: GenComponentF,
            firstDeps: List[Reactive[?]],
            make: ComponentManager => GenComponentF,
            attrs: Seq[ElementEffectF]): GenComponentF = {
    var pending: Option[(GenComponentF, List[Reactive[?]])] = Some((first, firstDeps))
    (root, am) => NodesComponentController { c =>
      val parent = c.parent.getOrElse(root)
      var deps: List[Reactive[?]] = Nil
      for {
        t4 <- ComponentManager { cm =>
          val f = pending match {
            case Some((f0, d0)) =>
              pending = None
              deps = d0
              f0
            case None =>
              val col = new Tracking.Collector
              val f1 = Tracking.collect(col)(make(cm))
              deps = col.deps.toList
              f1
          }
          CM.c(f, attrs *)(using cm)
        }
        // Scala 3.4.0~
        // (ns, children, tmpRs, eIds) = t4
        (ns, children, tmpRs, eIds) = t4
        _ <- addNodes(parent)(am, ns *).toRight(addNodesErrorMessage)
      } yield {
        c.watchInfos = deps.map(_.addWatcher(_ => c.reload()))
        c.nodes = ns
        c.children = children
        c.tmpReactives = tmpRs
        c.eventHandlers = eIds
        c
      }
    }
  }

}
