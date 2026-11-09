package io.github.ondeoma.scalactive.bench

import io.github.ondeoma.scalactive.reactive.{RV, RVList, ReactiveList}

import scala.util.Random

final case class Row(id: Int, label: String)

/** 監視(ウォッチャー)の数を数えられるRV. 解除漏れの検出に使います. */
final class CountingRV[A](a: A) extends RV[A](a) {
  def watcherCount: Int = watchers.size
}

/** 監視の数を数えられるRVList */
final class CountingRVList[A] extends RVList[A](Nil) {
  def watcherCount: Int = watchers.size
}

/** 監視の数を数えられるReactiveList */
final class CountingReactiveList[A] extends ReactiveList[A](Nil) {
  def watcherCount: Int = watchers.size
}

/**
 * ベンチマークの対象になる状態. コンポーネントの外で作るので、コンポーネントをクリアしても残ります.
 */
object Store {

  val rows: CountingRVList[Row] = new CountingRVList[Row]
  /** impl=lrで使う、ReactiveList版の行 */
  val lrows: CountingReactiveList[Row] = new CountingReactiveList[Row]
  /** trueならlrowsを使う(impl=lr) */
  var useLR: Boolean = false
  val selected: CountingRV[Int] = new CountingRV[Int](-1)

  private var nextId = 1
  private val random = new Random(42)

  private val adjectives = Vector("pretty", "large", "big", "small", "tall", "short", "long", "handsome", "plain",
    "quaint", "clean", "elegant", "easy", "angry", "crazy", "helpful", "mushy", "odd", "unsightly", "adorable",
    "important", "inexpensive", "cheap", "expensive", "fancy")
  private val colours = Vector("red", "yellow", "blue", "green", "pink", "brown", "purple", "brown", "white", "black",
    "orange")
  private val nouns = Vector("table", "chair", "house", "bbq", "desk", "car", "pony", "cookie", "sandwich", "burger",
    "pizza", "mouse", "keyboard")

  private def pick(v: Vector[String]): String = v(random.nextInt(v.size))

  def build(n: Int): List[Row] = {
    List.tabulate(n) { _ =>
      val id = nextId
      nextId += 1
      Row(id, s"${pick(adjectives)} ${pick(colours)} ${pick(nouns)}")
    }
  }

  /** 今の行の値 */
  def rowsV: List[Row] = if (useLR) lrows.v else rows.v

  def assign(v: List[Row]): Unit = if (useLR) lrows := v else rows := v

  def updateRow(i: Int, row: Row): Unit = if (useLR) lrows.update(i, row) else rows.update(i, row)

  def rmRow(i: Int): Unit = if (useLR) lrows.rm(i) else rows.rm(i)

  def rowsWatcherCount: Int = if (useLR) lrows.watcherCount else rows.watcherCount

  def reset(): Unit = {
    assign(Nil)
    selected := -1
  }

}
