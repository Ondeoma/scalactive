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
 *  - 使い回せる行がなければ、全体をまとめて作り直します.
 *
 * 行はDOM上で連続して並んでおり、行のノードは「行の先頭のノードから、次の行の先頭(または末尾の目印)の手前まで」です.
 * (行の中の子コンポーネントが後から足したノードも含みます)
 * 行ごとにノードのリストを持ち続けると、保持する参照が増えてDOM操作まで遅くなるため、先頭のノードだけを覚えます.
 */
object ForGeneralComponent extends BaseComponent {

  private final class Row(val key: Any,
                          val snapshot: Any,
                          var index: Int,
                          val first: Node,
                          val ownLast: Node,
                          val children: ChildrenComponents,
                          val tmpReactives: TemporaryReactiveValues,
                          val eventHandlers: EventHandlerIds) {

    /**
     * 行のHTML自身のノード(firstからownLastまで)を取り除く. 間に子コンポーネントのノードがあれば一緒に取り除く.
     * 末尾の目印に頼らないので、親が目印を先に取り除いていても、後ろの無関係なノードを取り除くことはない.
     * (ownLastより後ろに子コンポーネントが足したノードは、子コンポーネント自身のclearで取り除かれる)
     */
    def removeOwnNodes(): Unit = {
      var n = first
      var done = false
      while (n != null && !done) {
        val next = n.nextSibling
        done = n eq ownLast
        ClearScope.remove(n)
        n = next
      }
    }

    /** 行のノード以外(子コンポーネントなど)を片付ける */
    def disposeRest(): Unit = {
      children.foreach(_.clear())
      tmpReactives.foreach(_.abort())
      eventHandlers.foreach(EventManager.rmHandler)
    }

  }

  /** firstからendの手前までのノード(endに届かなければ最後の兄弟まで) */
  private def nodesBetween(first: Node, end: Node): List[Node] = {
    val buf = List.newBuilder[Node]
    var n = first
    while (n != null && !(n eq end)) {
      buf += n
      n = n.nextSibling
    }
    buf.result()
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

    /** 行を作る. 作った行のノード(子コンポーネントが足したものを含む)も返す */
    def render(item: B, i: Int): Either[String, (Row, List[Node])] = {
      io.github.ondeoma.scalactive.components.ComponentManager.genWithOwnLast(genHtml(_, item, i)).map { (t4, ownLast) =>
        // Scala 3.4.0~
        // (ns, children, tmpRs, eIds) = t4
        val (ns, children, tmpRs, eIds) = t4
        (new Row(keyOf(item), snapshotOf(item), i, ns.head, ownLast, children, tmpRs, eIds), ns)
      }
    }

    /** 今の行それぞれのノード(DOMを触る前に求めておく) */
    def currentNodes(): Array[List[Node]] = {
      val result = new Array[List[Node]](rows.length)
      var i = 0
      while (i < rows.length) {
        val end = if (i + 1 < rows.length) rows(i + 1).first else anchor
        result(i) = nodesBetween(rows(i).first, end)
        i += 1
      }
      result
    }

    /** すべての行を片付ける(clearのとき) */
    def disposeAll(): Unit = ClearScope.run {
      val parent = if (anchor == null) null else anchor.parentNode
      if (rows.nonEmpty && parent != null && (rows.head.first.parentNode eq parent)) {
        // 行は最初の行から末尾の目印の手前まで連続しているので、Rangeでまとめて取り除く(1行ずつより速い)
        if (!ClearScope.isRemoved(parent)) {
          val nodes = currentNodes()
          val range = document.createRange()
          range.setStartBefore(rows.head.first)
          range.setEndBefore(anchor)
          range.deleteContents()
          // 子コンポーネントが、取り除いた行の内側のノードを個別に取り除かないようにする
          nodes.foreach(_.foreach(ClearScope.markRemoved))
        }
      } else {
        // 親に目印を先に取り除かれた場合など
        rows.foreach(_.removeOwnNodes())
      }
      rows.foreach(_.disposeRest())
      rows = Vector.empty
    }

    /**
     * 後ろから順に、動かさない行(古い位置の最長増加部分列)以外を正しい位置に入れる.
     * 続けて入れる行は、DocumentFragmentにまとめて1回で入れる.
     */
    def place(parent: Node, nodesOf: Array[List[Node]], oldPos: Array[Int]): Unit = {
      val stay = longestIncreasing(oldPos)
      var ref: Node = anchor
      // 入れる行の連なり(先頭から順)
      var run: List[List[Node]] = Nil

      def flushRun(): Unit = {
        if (run.nonEmpty) {
          val frag = document.createDocumentFragment()
          run.foreach(_.foreach(node => frag.appendChild(node)))
          parent.insertBefore(frag, ref)
          run.iterator.flatMap(_.headOption).nextOption().foreach(h => ref = h)
          run = Nil
        }
      }

      var i = nodesOf.length - 1
      while (i >= 0) {
        if (stay(i)) {
          flushRun()
          nodesOf(i).headOption.foreach(h => ref = h)
        } else {
          run = nodesOf(i) :: run
        }
        i -= 1
      }
      flushRun()
    }

    /**
     * 全体を作り直す. 古い行への参照を先に手放す.
     */
    def fullRebuild(c: NodesComponentController): Unit = {
      c.clear()
      c.init()
    }

    def diff(c: NodesComponentController, parent: Node): Either[String, Unit] = {
      val items = toList(values).toVector
      val n = items.length
      val m = rows.length

      // 1. 各行について、使い回す古い行を決める(まだDOMは触らない)
      val matched = Array.fill(n)(-1)
      var keepCount = 0

      def tryKeep(j: Int, p: Int): Unit = {
        val r = rows(p)
        if ((!usesIndex || r.index == j) && r.snapshot == snapshotOf(items(j))) {
          matched(j) = p
          keepCount += 1
        }
      }

      // 先頭と末尾からキーが同じ行を突き合わせる(代入・追加・途中の削除などはここで済む)
      var start = 0
      while (start < n && start < m && rows(start).key == keyOf(items(start))) {
        tryKeep(start, start)
        start += 1
      }
      var endNew = n - 1
      var endOld = m - 1
      while (endNew >= start && endOld >= start && rows(endOld).key == keyOf(items(endNew))) {
        tryKeep(endNew, endOld)
        endNew -= 1
        endOld -= 1
      }

      // 残りの中間部分は、古い行をキーごとに(元の位置と一緒に)並べて対応付ける. 同じキーが複数あれば前から順に
      if (start <= endNew && start <= endOld) {
        val pool = mutable.HashMap[Any, mutable.Queue[Int]]()
        (start to endOld).foreach(p => pool.getOrElseUpdate(rows(p).key, mutable.Queue()).enqueue(p))
        (start to endNew).foreach { j =>
          pool.get(keyOf(items(j))).filter(_.nonEmpty).map(_.dequeue()).foreach(p => tryKeep(j, p))
        }
      }

      if (keepCount == 0) {
        // 使い回せる行がなければ、全体を作り直すほうが速い(まとめて取り除き、まとめて入れる)
        Right(fullRebuild(c))
      } else {
        // 2. 使い回さない古い行を片付ける
        val oldNodes = currentNodes()
        val kept = new Array[Boolean](rows.length)
        matched.foreach(p => if (p >= 0) kept(p) = true)
        ClearScope.run {
          rows.indices.foreach { p =>
            if (!kept(p)) {
              oldNodes(p).foreach(ClearScope.remove)
              rows(p).disposeRest()
            }
          }
        }

        // 3. 新しい行を作る
        val next = new Array[Row](n)
        val nodesOf = new Array[List[Node]](n)
        var error: Option[String] = None
        var i = 0
        while (i < n && error.isEmpty) {
          val p = matched(i)
          if (p >= 0) {
            val r = rows(p)
            r.index = i
            next(i) = r
            nodesOf(i) = oldNodes(p)
          } else {
            render(items(i), i) match {
              case Right((r, ns)) =>
                next(i) = r
                nodesOf(i) = ns
              case Left(e) => error = Some(e)
            }
          }
          i += 1
        }

        error match {
          case Some(e) =>
            // 使い回す予定だった行と作れた行を片付けてから、呼び出し元で全体を作り直す
            ClearScope.run {
              rows.indices.foreach { p =>
                if (kept(p)) {
                  oldNodes(p).foreach(ClearScope.remove)
                  rows(p).disposeRest()
                }
              }
              next.indices.foreach { k =>
                if (next(k) != null && matched(k) < 0) {
                  nodesOf(k).foreach(ClearScope.remove)
                  next(k).disposeRest()
                }
              }
            }
            rows = Vector.empty
            Left(e)
          case None =>
            place(parent, nodesOf, matched)
            rows = next.toVector
            Right(())
        }
      }
    }

    def refresh(c: NodesComponentController): Unit = {
      val parent = Option(anchor).map(_.parentNode).orNull
      if (parent == null) {
        // 目印がDOMにない(まだ描いていない等)場合は作り直す
        fullRebuild(c)
      } else {
        diff(c, parent).left.foreach { e =>
          // 行を作れなかった場合は全体を作り直す
          ConsoleUtil.error(e)
          fullRebuild(c)
        }
      }
    }

    val controller = NodesComponentController { c =>
      for {
        rendered <- toList(values).zipWithIndex.traverse((item, i) => render(item, i))
        a = new Comment
        _ <- addNodes(c.parent.getOrElse(root))(am, (rendered.flatMap(_._2) :+ a) *).toRight(addNodesErrorMessage)
      } yield {
        rows = rendered.iterator.map(_._1).toVector
        anchor = a
        // コントローラーには目印だけを持たせ、行はclearのとき(onClear)に片付ける
        c.nodes = List(a)
        c.watchInfos = watch(c)
        c
      }
    }
    controller.onRefresh = Some(refresh)
    controller.onClear = Some(_ => disposeAll())
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
