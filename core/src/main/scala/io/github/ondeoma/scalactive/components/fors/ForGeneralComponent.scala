package io.github.ondeoma.scalactive.components.fors

import cats.syntax.all.*
import io.github.ondeoma.scalactive.components.BaseComponent
import io.github.ondeoma.scalactive.controllers.ClearScope
import io.github.ondeoma.scalactive.events.EventManager
import io.github.ondeoma.scalactive.utils.ConsoleUtil
import org.scalajs.dom.*

import scala.collection.mutable

/**
 * リストレンダリングの本体.
 *
 * リストが変わったときは全行を作り直さず、行をキーで対応付けて差分だけをDOMに反映します.
 *  - 消えた行は片付け、新しい行は作り、残った行はDOMノードごと使い回します.
 *  - 使い回す行でも、値(snapshotOf)が変わっていた行と、usesIndexのときに位置が変わった行は作り直します
 *    (行のHTMLに埋め込まれた値や添字が古くならないように).
 *  - 並び順は、最長増加部分列に含まれない行だけを動かして合わせます.
 */
object ForGeneralComponent extends BaseComponent {

  private final class Row(val key: Any,
                          val snapshot: Any,
                          var index: Int,
                          val nodes: List[Node],
                          val children: ChildrenComponents,
                          val tmpReactives: TemporaryReactiveValues,
                          val eventHandlers: EventHandlerIds) {

    def dispose(): Unit = ClearScope.run {
      nodes.foreach(ClearScope.remove)
      children.foreach(_.clear())
      tmpReactives.foreach(_.abort())
      eventHandlers.foreach(EventManager.rmHandler)
    }

  }

  /**
   * @param keyOf      行を対応付けるキー. 既定は要素そのもの(RV/ReactiveModelは同一のオブジェクト、それ以外は等しい値)
   * @param snapshotOf 行を描いたときの値. 使い回すときに変わっていれば作り直す(RVの行ならrv.v)
   * @param usesIndex  genHtmlが添字を使うか. trueなら位置が変わった行を作り直す
   */
  def apply[A, B](root: HTMLElement,
                  am: AddMethod,
                  values: A,
                  toList: A => List[B],
                  watch: NodesComponentController => WatchInfos,
                  keyOf: B => Any = (b: B) => b,
                  snapshotOf: B => Any = (_: B) => (),
                  usesIndex: Boolean = true)
                 (genHtml: (CM, B, IDX) => HTML): NodesComponentController = {

    var rows: Vector[Row] = Vector.empty
    // 行の末尾の目印. 新しい行はこの前(または次の行の前)に入れる
    var anchor: Node = null

    def render(item: B, i: Int): Either[String, Row] = {
      ComponentManager(genHtml(_, item, i)).map { t4 =>
        // Scala 3.4.0~
        // (ns, children, tmpRs, eIds) = t4
        val (ns, children, tmpRs, eIds) = t4
        new Row(keyOf(item), snapshotOf(item), i, ns, children, tmpRs, eIds)
      }
    }

    def sync(c: NodesComponentController): Unit = {
      c.nodes = rows.iterator.flatMap(_.nodes).toList :+ anchor
      c.children = rows.iterator.flatMap(_.children).toList
      c.tmpReactives = rows.iterator.flatMap(_.tmpReactives).toList
      // 行の中で登録したEventManagerのハンドラーも、描き直し・片付けのときに解除する
      c.eventHandlers = rows.iterator.flatMap(_.eventHandlers).toList
    }

    /** 後ろから順に、動かさない行(古い位置の最長増加部分列)以外を正しい位置に入れる */
    def place(parent: Node, next: Array[Row], oldPos: Array[Int]): Unit = {
      val stay = longestIncreasing(oldPos)
      var ref: Node = anchor
      var i = next.length - 1
      while (i >= 0) {
        val r = next(i)
        if (!stay(i)) r.nodes.foreach(node => parent.insertBefore(node, ref))
        r.nodes.headOption.foreach(h => ref = h)
        i -= 1
      }
    }

    def diff(c: NodesComponentController, parent: Node): Either[String, Unit] = {
      val items = toList(values).toVector
      val n = items.length

      // 古い行をキーごとに(元の位置と一緒に)並べておく. 同じキーが複数あれば前から順に対応付ける
      val pool = mutable.HashMap[Any, mutable.Queue[(Row, Int)]]()
      rows.iterator.zipWithIndex.foreach { (r, i) =>
        pool.getOrElseUpdate(r.key, mutable.Queue()).enqueue((r, i))
      }

      val next = new Array[Row](n)
      // 使い回した行の古い位置(新しく作った行は-1)
      val oldPos = Array.fill(n)(-1)
      var error: Option[String] = None
      var i = 0
      while (i < n && error.isEmpty) {
        val item = items(i)
        val reused = pool.get(keyOf(item)).filter(_.nonEmpty).map(_.dequeue())
        reused match {
          case Some((r, p)) if (!usesIndex || r.index == i) && r.snapshot == snapshotOf(item) =>
            r.index = i
            next(i) = r
            oldPos(i) = p
          case other =>
            other.foreach(_._1.dispose())
            render(item, i) match {
              case Right(r) => next(i) = r
              case Left(e) => error = Some(e)
            }
        }
        i += 1
      }
      // 対応する行がなくなった古い行を片付ける
      pool.valuesIterator.foreach(_.foreach(_._1.dispose()))

      error match {
        case Some(e) =>
          // 作れた分だけ覚えておき、呼び出し元で全体を作り直す
          rows = next.iterator.filter(_ != null).toVector
          sync(c)
          Left(e)
        case None =>
          place(parent, next, oldPos)
          rows = next.toVector
          sync(c)
          Right(())
      }
    }

    def refresh(c: NodesComponentController): Unit = {
      val parent = Option(anchor).map(_.parentNode).orNull
      if (parent == null) {
        // 目印がDOMにない(まだ描いていない等)場合は作り直す
        c.clear()
        c.init()
      } else {
        diff(c, parent).left.foreach { e =>
          // 行を作れなかった場合は全体を作り直す
          ConsoleUtil.error(e)
          c.clear()
          c.init()
        }
      }
    }

    val controller = NodesComponentController { c =>
      for {
        rs <- toList(values).zipWithIndex.traverse((item, i) => render(item, i))
        a = new Comment
        _ <- addNodes(c.parent.getOrElse(root))(am, (rs.flatMap(_.nodes) :+ a) *).toRight(addNodesErrorMessage)
      } yield {
        rows = rs.toVector
        anchor = a
        sync(c)
        c.watchInfos = watch(c)
        c
      }
    }
    controller.onRefresh = Some(refresh)
    controller
  }

  /**
   * 値が0以上の要素のうち、値が増加する最長の部分列に含まれる位置をtrueにして返します(O(n log n)).
   */
  private[fors] def longestIncreasing(xs: Array[Int]): Array[Boolean] = {
    val n = xs.length
    val result = new Array[Boolean](n)
    // tails(k): 長さk+1の増加部分列のうち、末尾の値が最小のものの末尾の位置
    val tails = new Array[Int](n)
    val prev = Array.fill(n)(-1)
    var len = 0
    var i = 0
    while (i < n) {
      val x = xs(i)
      if (x >= 0) {
        var lo = 0
        var hi = len
        while (lo < hi) {
          val mid = (lo + hi) >>> 1
          if (xs(tails(mid)) < x) lo = mid + 1 else hi = mid
        }
        if (lo > 0) prev(i) = tails(lo - 1)
        tails(lo) = i
        if (lo == len) len += 1
      }
      i += 1
    }
    var k = if (len > 0) tails(len - 1) else -1
    while (k >= 0) {
      result(k) = true
      k = prev(k)
    }
    result
  }

}
