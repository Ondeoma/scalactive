package io.github.ondeoma.scalactive.st

import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager}
import io.github.ondeoma.scalactive.components.ComponentManager as CM
import io.github.ondeoma.scalactive.controllers.NodesComponentController
import io.github.ondeoma.scalactive.reactive.{Reactive, Tracking}
import io.github.ondeoma.scalactive.utils.TypeAlias.*

/**
 * propsの式でリアクティブ値を読んでいる子コンポーネント(`<Child :title="s.item.v.name" />`).
 * 読んだ値が変わったら、propsを計算し直して子を作り直します(子の状態は初期化されます. Vueで :key が変わった場合と同じ).
 * ただし、propsの値がすべて前と同じ(==)なら作り直しません(例: `:item="s.items.v.find(_.id == id)"` で、ほかの行だけが変わった場合).
 * 作り直す場合も、描き直しの前にフォーカスがあった入力欄には、描き直しの後にフォーカスを戻します(ReloadScheduler).
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
    apply(first, firstDeps, make, attrs, -1, None)
  }

  /**
   * @param propCount `__st.prop` で包んだpropsの数(-1は不明)
   * @param firstKeys 最初に作ったときのpropsの値(数がpropCountと合うときだけ)
   */
  def apply(first: GenComponentF,
            firstDeps: List[Reactive[?]],
            make: ComponentManager => GenComponentF,
            attrs: Seq[ElementEffectF],
            propCount: Int,
            firstKeys: Option[List[Any]]): GenComponentF = {
    var pending: Option[(GenComponentF, List[Reactive[?]], Option[List[Any]])] = Some((first, firstDeps, firstKeys))
    (root, am) => NodesComponentController { c =>
      val parent = c.parent.getOrElse(root)
      var deps: List[Reactive[?]] = Nil
      var keys: Option[List[Any]] = None
      var lastCm: ComponentManager = null

      def validKeys(ks: List[Any]): Option[List[Any]] = if (propCount > 0 && ks.length == propCount) Some(ks) else None

      def subscribe(ds: List[Reactive[?]]): Unit = {
        c.watchInfos.foreach(_.abort())
        deps = ds
        c.watchInfos = ds.map(_.addWatcher(_ => onChange()))
      }

      // 読んだ値が変わったら、propsの値だけを計算し直し、前と同じなら作り直さない(監視する値だけ集め直す)
      def onChange(): Unit = {
        keys match {
          case Some(old) if lastCm != null =>
            val col = new Tracking.Collector
            val next = scala.util.Try(PropKeys.keysOnly(Tracking.collect(col)(make(lastCm)))._2).toOption.flatMap(validKeys)
            if (next.contains(old)) subscribe(col.deps.toList)
            else c.reload()
          case _ => c.reload()
        }
      }

      for {
        t4 <- ComponentManager { cm =>
          lastCm = cm
          val f = pending match {
            case Some((f0, d0, k0)) =>
              pending = None
              deps = d0
              keys = k0
              f0
            case None =>
              val col = new Tracking.Collector
              val (f1, ks) = PropKeys.collect(Tracking.collect(col)(make(cm)))
              deps = col.deps.toList
              keys = validKeys(ks)
              f1
          }
          CM.c(f, attrs *)(using cm)
        }
        // Scala 3.4.0~
        // (ns, children, tmpRs, eIds) = t4
        (ns, children, tmpRs, eIds) = t4
        _ <- addNodes(parent)(am, ns *).toRight(addNodesErrorMessage)
      } yield {
        c.watchInfos = Nil
        subscribe(deps)
        c.nodes = ns
        c.children = children
        c.tmpReactives = tmpRs
        c.eventHandlers = eIds
        c
      }
    }
  }

}
