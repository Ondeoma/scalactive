package io.github.ondeoma.scalactive.reactive

/**
 * RVList/RMListが各要素に付ける監視の管理のテスト.
 * リストから外れた要素の監視は解除され、残った要素はちょうど1つ監視されている必要があります.
 */
class ListWatchSuite extends munit.FunSuite {

  /** 監視の数を数えられるRV */
  private final class Counted(a: Int) extends RV[Int](a) {
    def watcherCount: Int = watchers.size
  }

  private def counted(n: Int): List[Counted] = List.tabulate(n)(new Counted(_))

  private def assertWatched(list: List[Counted], removed: List[Counted])(implicit loc: munit.Location): Unit = {
    assertEquals(list.map(_.watcherCount), list.map(_ => 1), "リストにある要素")
    assertEquals(removed.map(_.watcherCount), removed.map(_ => 0), "リストから外れた要素")
  }

  /** リスト全体への通知の回数を数える */
  private def notifications(list: Reactive[?]): () => Int = {
    var count = 0
    list.addWatcher(_ => count += 1)
    () => count
  }

  test("RVList: 代入で短くすると、外れた要素の監視をすべて解除する") {
    val rvs = counted(5)
    val list = RVList(rvs)
    list := rvs.take(2).map(rv => new RV(rv.v))
    assertWatched(rvs.take(2), rvs.drop(2))
    assertEquals(list.v, List(0, 1))
  }

  test("RVList: 外れた要素を変更してもリストに通知しない") {
    val rvs = counted(5)
    val list = RVList(rvs)
    list := List(new RV(10))
    val count = notifications(list)
    rvs.drop(1).foreach(_ := 99)
    assertEquals(count(), 0)
    rvs.head := 11
    assertEquals(count(), 1)
  }

  test("RVList: 短くしてから長くしても監視は1つずつ") {
    val rvs = counted(3)
    val list = RVList(rvs)
    list := List(new RV(0))
    val added = counted(4)
    list := (rvs.head :: added)
    // 先頭は値の更新のみ、後ろの4件は新しく監視する
    assertWatched(rvs.head :: added, rvs.drop(1))
    assertEquals(list.v, List(0, 0, 1, 2, 3))
  }

  test("RVList: 並べ替えた後のrmは、その位置の要素の監視を解除する") {
    val rvs = counted(3)
    val Seq(a, b, c) = rvs: @unchecked
    val list = RVList(rvs)
    list.up(2) // a, c, b
    list.rm(1) // a, b
    assertEquals(list.v, List(0, 1))
    assertWatched(List(a, b), List(c))
    list.down(0) // b, a
    list.rm(0) // a
    assertWatched(List(a), List(b, c))
  }

  test("RVList: 何度rmしても、残った要素の監視は1つずつ") {
    val rvs = counted(60)
    val list = RVList(rvs)
    val random = new scala.util.Random(1)
    var rest = rvs
    while (rest.size > 5) {
      val i = random.nextInt(rest.size)
      list.rm(i)
      rest = rest.patch(i, Nil, 1)
    }
    assertEquals(list.v, rest.map(_.v))
    assertWatched(rest, rvs.diff(rest))
  }

  test("RVList: 範囲外のrmは何もしない") {
    val rvs = counted(2)
    val list = RVList(rvs)
    list.rm(5)
    list.rm(-1)
    assertWatched(rvs, Nil)
  }

  test("RVList: abortですべての監視を解除する") {
    val rvs = counted(3)
    val list = RVList(rvs)
    list.abort()
    assertWatched(Nil, rvs)
  }

  test("RVList: 代入はリスト全体への通知を1回にまとめる(行ごとの監視には通知する)") {
    val rvs = counted(100)
    val list = RVList(rvs)
    val count = notifications(list)
    var rowLevel = 0
    list.addWatcherRowLevel(_ => rowLevel += 1)
    var rowChanges = 0
    rvs.foreach(_.addWatcher(_ => rowChanges += 1))
    list := List.tabulate(100)(_ + 1)
    assertEquals(count(), 1)
    assertEquals(rowLevel, 1)
    assertEquals(rowChanges, 100, "各行の値の監視(行の表示など)には通知する")
    assertEquals(list.v, List.tabulate(100)(_ + 1))
  }

  test("RVList: updateは1回だけ通知し、行レベルの監視には通知しない") {
    val list = RVList(counted(3))
    val count = notifications(list)
    var rowLevel = 0
    list.addWatcherRowLevel(_ => rowLevel += 1)
    list.update(1, 10)
    assertEquals(count(), 1)
    assertEquals(rowLevel, 0)
    assertEquals(list.v, List(0, 10, 2))
  }

  test("RVList: 要素のRVを直接変更すると1回通知し、行レベルの監視には通知しない") {
    val rvs = counted(3)
    val list = RVList(rvs)
    val count = notifications(list)
    var rowLevel = 0
    list.addWatcherRowLevel(_ => rowLevel += 1)
    rvs(2) := 5
    assertEquals(count(), 1)
    assertEquals(rowLevel, 0)
  }

  // ---- 同じ値の代入 ----

  test("RV: 同じ値の代入では通知せず、triggerで明示的に通知できる") {
    val rv = new Counted(1)
    var calls = List.empty[(Int, Int)]
    rv.addWatcher { case (o, n) => calls = calls :+ (o, n) }
    rv := 1
    assertEquals(calls, Nil)
    rv := 2
    rv := 2
    assertEquals(calls, List((1, 2)))
    rv.trigger()
    assertEquals(calls, List((1, 2), (2, 2)))
  }

  test("CRV: 計算結果が変わらなければ通知しない") {
    val rv = new RV(0)
    val big = CRV(rv, (_: Int) > 3)
    var calls = 0
    big.addWatcher(_ => calls += 1)
    (1 to 3).foreach(rv := _)
    assertEquals(calls, 0)
    rv := 4
    rv := 5
    assertEquals(calls, 1)
    assertEquals(big.v, true)
  }

  test("RVList: 同じ値の代入では変わった行にだけ通知する") {
    val rvs = counted(3)
    val list = RVList(rvs)
    var rowChanges = 0
    rvs.foreach(_.addWatcher(_ => rowChanges += 1))
    list := List(0, 10, 2)
    assertEquals(rowChanges, 1)
  }

  // ---- RMList ----

  private final case class Item(n: Int)

  private final case class ItemRM(n: RV[Int]) extends ReactiveModel[Item, ItemRM] {
    val reactives: List[Reactive[?]] = List(n)
    val reactiveModels: List[ReactiveModel[?, ?]] = Nil

    def reload(o: Item): Unit = n := o.n

    def toOrigin: Item = Item(n.v)

    def watcherCount: Int = watchers.size
  }

  private final case class PairRM(a: ItemRM, b: RV[String]) extends ReactiveModel[(Item, String), PairRM] {
    val reactives: List[Reactive[?]] = List(b)
    val reactiveModels: List[ReactiveModel[?, ?]] = List(a)

    def reload(o: (Item, String)): Unit = { a.reload(o._1); b := o._2 }

    def toOrigin: (Item, String) = (a.toOrigin, b.v)
  }

  /** マイクロタスク(ReactiveModelの通知)を待つ */
  private def flush(): scala.concurrent.Future[Unit] = {
    val p = scala.concurrent.Promise[Unit]()
    scala.scalajs.js.Dynamic.global.setTimeout(() => p.success(()), 0)
    p.future
  }

  test("ReactiveModel: 作った直後に監視して変更しても、待たずに通知する") {
    implicit val ec: scala.concurrent.ExecutionContext = scala.scalajs.concurrent.JSExecutionContext.queue
    val m = ItemRM(new RV(1))
    var seen = List.empty[(Item, Item)]
    m.addWatcher { case (o, n) => seen = seen :+ (o -> n) }
    m.n := 2
    m.n := 3
    flush().map { _ =>
      assertEquals(seen, List(Item(1) -> Item(3)), "まとめて1回、変更前と変更後の値で通知する")
    }
  }

  test("ReactiveModel: 入れ子のモデルの変更も通知し、abortで監視を解除する") {
    implicit val ec: scala.concurrent.ExecutionContext = scala.scalajs.concurrent.JSExecutionContext.queue
    val inner = new RV(1)
    val m = PairRM(ItemRM(inner), new RV("x"))
    var count = 0
    m.addWatcher(_ => count += 1)
    inner := 5
    flush().flatMap { _ =>
      assertEquals(count, 1)
      assertEquals(m.v, (Item(5), "x"))
      m.abort()
      inner := 6
      flush()
    }.map { _ =>
      assertEquals(count, 1, "abortの後は通知しない")
      assertEquals(m.a.watcherCount, 0, "入れ子のモデルの監視も解除する")
    }
  }

  private def items(n: Int): List[ItemRM] = List.tabulate(n)(i => ItemRM(new RV(i)))

  private def assertWatchedRM(list: List[ItemRM], removed: List[ItemRM])(implicit loc: munit.Location): Unit = {
    assertEquals(list.map(_.watcherCount), list.map(_ => 1), "リストにある要素")
    assertEquals(removed.map(_.watcherCount), removed.map(_ => 0), "リストから外れた要素")
  }

  test("RMList: 並べ替えた後のrmは、その位置の要素の監視を解除する") {
    val rms = items(3)
    val Seq(a, b, c) = rms: @unchecked
    val list = new RMList[Item, ItemRM](rms)
    list.up(2) // a, c, b
    list.rm(1) // a, b
    assertEquals(list.v, List(Item(0), Item(1)))
    assertWatchedRM(List(a, b), List(c))
  }

  test("RMList: 何度rmしても、残った要素の監視は1つずつ") {
    val rms = items(60)
    val list = new RMList[Item, ItemRM](rms)
    val random = new scala.util.Random(2)
    var rest = rms
    while (rest.size > 5) {
      val i = random.nextInt(rest.size)
      list.rm(i)
      rest = rest.patch(i, Nil, 1)
    }
    assertEquals(list.v, rest.map(_.toOrigin))
    assertWatchedRM(rest, rms.diff(rest))
  }

  test("RMList: updateで置き換えた要素の監視を解除する") {
    val rms = items(3)
    val list = new RMList[Item, ItemRM](rms)
    val next = ItemRM(new RV(10))
    list.update(1, next)
    assertWatchedRM(List(rms(0), next, rms(2)), List(rms(1)))
  }

}
