package io.github.ondeoma.scalactive.reactive

import io.github.ondeoma.scalactive.models.WatchInfo

import scala.collection.mutable.ListBuffer

/**
 * 通知の途中で監視が追加・削除されても、他の監視が飛ばされないことのテスト.
 * (以前は監視のMapをforeachしながら呼んでいたため、監視の中で監視を足してハッシュ表が広がると、
 * 残りの監視が呼ばれないことがありました)
 */
class NotifyWhileWatchingSuite extends munit.FunSuite {

  /** ハッシュ表が広がるだけの監視を足す数 */
  private val manyWatchers = 64

  /** n個の監視を付け、呼ばれた監視の番号を記録する */
  private def watchN(r: Reactive[?], n: Int, called: ListBuffer[Int]): List[WatchInfo] =
    List.tabulate(n)(i => r.addWatcher(_ => called += i))

  test("RV: 監視の中で同じ値に監視をたくさん足しても、他の監視はすべて呼ばれる") {
    val rv = new RV(0)
    val called = ListBuffer[Int]()
    var added = 0
    rv.addWatcher { _ =>
      (1 to manyWatchers).foreach(_ => rv.addWatcher(_ => added += 1))
    }
    watchN(rv, 10, called)

    rv := 1
    assertEquals(called.sorted.toList, (0 until 10).toList)
    // 通知中に足した監視は、その回には呼ばない
    assertEquals(added, 0)

    called.clear()
    rv := 2
    assertEquals(called.sorted.toList, (0 until 10).toList)
    assertEquals(added, manyWatchers)
  }

  test("RV: computedの中でmapCを呼ぶように、通知のたびに新しいCRVが監視を足しても飛ばされない") {
    val rv = new RV(0)
    var total = -1
    // 通知のたびに、同じrvを監視するCRVを作る(purchaseの/cartで起きた書き方)
    rv.addWatcher { _ =>
      (1 to manyWatchers).foreach(_ => rv.mapC(_ * 2))
    }
    val sum = rv.mapC(_ * 10)
    sum.addWatcher { case (_, n) => total = n }

    (1 to 5).foreach { i =>
      rv := i
      assertEquals(total, i * 10, s"${i}回目の変更")
    }
  }

  test("RV: 監視の中で削除された監視は、その回には呼ばない") {
    val rv = new RV(0)
    var called = 0
    var infos: List[WatchInfo] = Nil
    // どれが先に呼ばれても、最初に呼ばれた監視が他の監視をすべて削除する(Mapの順序に依存しない)
    infos = List.fill(10)(rv.addWatcher { _ =>
      called += 1
      infos.foreach(_.abort())
    })

    rv := 1
    assertEquals(called, 1)
    assertEquals(rv.numWatchers, 0)
  }

  test("RV: 監視の中で自分自身を削除しても、他の監視は呼ばれる") {
    val rv = new RV(0)
    val called = ListBuffer[Int]()
    var self: WatchInfo = null
    self = rv.addWatcher(_ => self.abort())
    watchN(rv, 10, called)

    rv := 1
    assertEquals(called.sorted.toList, (0 until 10).toList)
    assertEquals(rv.numWatchers, 10)
  }

  test("RV.trigger: 監視の中で監視を足しても、他の監視はすべて呼ばれる") {
    val rv = new RV(0)
    val called = ListBuffer[Int]()
    rv.addWatcher(_ => (1 to manyWatchers).foreach(_ => rv.addWatcher(_ => ())))
    watchN(rv, 10, called)

    rv.trigger()
    assertEquals(called.sorted.toList, (0 until 10).toList)
  }

  test("CRV: 監視の中で同じCRVに監視を足しても、他の監視はすべて呼ばれる") {
    val rv = new RV(0)
    val crv = rv.mapC(_ + 1)
    val called = ListBuffer[Int]()
    crv.addWatcher(_ => (1 to manyWatchers).foreach(_ => crv.addWatcher(_ => ())))
    watchN(crv, 10, called)

    rv := 1
    assertEquals(called.sorted.toList, (0 until 10).toList)
  }

  test("ReactiveList: 監視の中で監視を足しても、他の監視はすべて呼ばれる") {
    val list = new ReactiveList(List(1))
    val called = ListBuffer[Int]()
    list.addWatcher(_ => (1 to manyWatchers).foreach(_ => list.addWatcher(_ => ())))
    watchN(list, 10, called)

    list(List(1, 2))
    assertEquals(called.sorted.toList, (0 until 10).toList)
  }

  test("RVList: 要素の変更の通知中に監視を足しても飛ばされず、行レベルの監視は呼ばない") {
    val rvs = List(new RV(1), new RV(2))
    val list = RVList(rvs)
    val called = ListBuffer[Int]()
    var rowLevel = 0
    list.addWatcherRowLevel(_ => rowLevel += 1)
    list.addWatcher(_ => (1 to manyWatchers).foreach(_ => list.addWatcher(_ => ())))
    watchN(list, 10, called)

    rvs.head := 5
    assertEquals(called.sorted.toList, (0 until 10).toList)
    assertEquals(rowLevel, 0)
  }

}
