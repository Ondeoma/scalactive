package io.github.ondeoma.scalactive.bench

import io.github.ondeoma.scalactive.reactive.{CRV, RV, RVList, ReactiveList}
import org.scalajs.dom.*

/**
 * 1回分の操作. actionを実行してから、doneがtrueになる(画面に反映される)までを計測します.
 * 入力データはStepを作る時点(計測の前)に用意し、actionの中では作らないようにします.
 */
final case class Step(action: () => Unit, done: () => Boolean)

/**
 * @param kind       "dom": 描画を含めて非同期に計測, "sync": actionの同期的な実行時間だけを計測
 * @param iterations 計測回数の既定値(重いシナリオは少なめ)
 */
final case class Scenario(name: String,
                          description: String,
                          kind: String,
                          iterations: Int,
                          setup: () => List[Step],
                          measured: () => Step)

object Scenarios {

  private def tbody: HTMLElement = document.getElementById("tbody").asInstanceOf[HTMLElement]

  private def trs: HTMLCollection[Element] = {
    val t = tbody
    if (t == null) document.createElement("tbody").children else t.children
  }

  private def rowCount: Int = trs.length

  /** 表示中のn番目の行のid(行がなければ-1) */
  private def idAt(n: Int): Int = {
    val ts = trs
    if (n < 0 || n >= ts.length) -1
    else Option(ts(n).querySelector(".id")).map(_.textContent.trim.toInt).getOrElse(-1)
  }

  private def labelAt(n: Int): String = {
    val ts = trs
    if (n < 0 || n >= ts.length) ""
    else Option(ts(n).querySelector(".label")).map(_.textContent).getOrElse("")
  }

  /** 空の表にする */
  private val clear: Step = Step(() => Store.reset(), () => rowCount == 0)

  /** n行を作る(空の状態から) */
  private def create(n: Int): Step = {
    val rows = Store.build(n)
    Step(() => Store.assign(rows), () => rowCount == n && idAt(n - 1) == rows.last.id && idAt(0) == rows.head.id)
  }

  private def withRows(n: Int): () => List[Step] = () => List(clear, create(n))

  private def dom(name: String, description: String, iterations: Int = 10)
                 (setup: () => List[Step])
                 (measured: => Step): Scenario = {
    Scenario(name, description, "dom", iterations, setup, () => measured)
  }

  private def sync(name: String, description: String, iterations: Int = 10)
                  (prepare: => (() => Unit)): Scenario = {
    Scenario(name, description, "sync", iterations, () => Nil, () => {
      val action = prepare
      Step(action, () => true)
    })
  }

  val all: List[Scenario] = List(
    dom("create-1k", "空の表に1,000行を作る")(() => List(clear))(create(1000)),
    dom("replace-1k", "1,000行をすべて別の1,000行に置き換える")(withRows(1000)) {
      val rows = Store.build(1000)
      Step(() => Store.assign(rows), () => idAt(0) == rows.head.id && idAt(999) == rows.last.id)
    },
    dom("update-10th", "1,000行のうち10行に1行のラベルを書き換える")(withRows(1000)) {
      Step(
        () => Store.rowsV.zipWithIndex.foreach { (row, i) =>
          if (i % 10 == 0) Store.updateRow(i, row.copy(label = row.label + " !!!"))
        },
        () => labelAt(990).endsWith(" !!!") && labelAt(0).endsWith(" !!!")
      )
    },
    dom("select", "1,000行のうち1行を選択する(各行のclassが選択状態を監視)")(withRows(1000)) {
      val id = Store.rowsV(500).id
      Step(() => Store.selected := id, () => Option(trs(500)).exists(_.classList.contains("danger")))
    },
    dom("swap", "1,000行のうち2行を入れ替える")(withRows(1000)) {
      val a = Store.rowsV(1)
      val b = Store.rowsV(998)
      Step(
        () => Store.assign(Store.rowsV.updated(1, b).updated(998, a)),
        () => idAt(1) == b.id && idAt(998) == a.id
      )
    },
    dom("remove", "1,000行から1行を削除する")(withRows(1000)) {
      val next = Store.rowsV(501).id
      Step(() => Store.rmRow(500), () => rowCount == 999 && idAt(500) == next)
    },
    dom("append-1k", "1,000行の後ろに1,000行を追加する")(withRows(1000)) {
      val rows = Store.build(1000)
      Step(() => Store.assign(Store.rowsV ++ rows), () => rowCount == 2000 && idAt(1999) == rows.last.id)
    },
    dom("clear-1k", "1,000行をすべて削除する")(withRows(1000))(clear),
    dom("create-10k", "空の表に10,000行を作る", iterations = 5)(() => List(clear))(create(10000)),

    sync("rv-fanout", "1,000個の計算値(mapC)が監視するRVを100回更新する") {
      val rv = new RV(0)
      val crvs = List.fill(1000)(CRV(rv, (_: Int) + 1))
      () => {
        (1 to 100).foreach(i => rv := i)
        crvs.foreach(_.abort())
      }
    },
    sync("rv-same-value", "1,000個の計算値が監視するRVに同じ値を100回代入する") {
      val rv = new RV(0)
      val crvs = List.fill(1000)(CRV(rv, (_: Int) + 1))
      () => {
        (1 to 100).foreach(_ => rv := 0)
        crvs.foreach(_.abort())
      }
    },
    sync("crv-chain", "1,000段つながった計算値の根元を100回更新する") {
      val rv = new RV(0)
      val chain = (1 to 1000).scanLeft(rv: io.github.ondeoma.scalactive.reactive.Reactive[Int]) { (r, _) =>
        CRV(r, (_: Int) + 1)
      }
      () => {
        (1 to 100).foreach(i => rv := i)
        chain.foreach(_.abort())
      }
    },
    sync("rvlist-assign", "リスト全体を監視される1,000件のRVListに1,000件を代入する") {
      val list = RVList(List.tabulate(1000)(identity))
      val size = CRV(list, (_: List[Int]).sum)
      val next = List.tabulate(1000)(_ + 1)
      () => {
        list := next
        size.abort()
      }
    },
    sync("reactivelist-assign", "リスト全体を監視される1,000件のReactiveListに1,000件を代入する") {
      val list = ReactiveList(List.tabulate(1000)(identity))
      val size = CRV(list, (_: List[Int]).sum)
      val next = List.tabulate(1000)(_ + 1)
      () => {
        list := next
        size.abort()
      }
    },
    sync("rvlist-update", "リスト全体を監視される1,000件のRVListの1件を100回書き換える") {
      val list = RVList(List.tabulate(1000)(identity))
      val size = CRV(list, (_: List[Int]).sum)
      () => {
        (1 to 100).foreach(i => list.update(i, -i))
        size.abort()
      }
    },
    sync("reactivelist-update", "リスト全体を監視される1,000件のReactiveListの1件を100回書き換える") {
      val list = ReactiveList(List.tabulate(1000)(identity))
      val size = CRV(list, (_: List[Int]).sum)
      () => {
        (1 to 100).foreach(i => list.update(i, -i))
        size.abort()
      }
    },
  )

  def find(name: String): Option[Scenario] = all.find(_.name == name)

}
