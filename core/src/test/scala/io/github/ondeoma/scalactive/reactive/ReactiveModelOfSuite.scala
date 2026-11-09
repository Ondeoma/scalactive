package io.github.ondeoma.scalactive.reactive

import scala.concurrent.{ExecutionContext, Future, Promise}

object ReactiveModelOfSuite {

  case class Address(city: String)
  case class AddressRM(city: RV[String]) extends ReactiveModelOf[Address, AddressRM]
  object AddressRM extends ReactiveModelOf.Companion[Address, AddressRM]

  case class Pet(name: String)
  case class PetRM(name: RV[String]) extends ReactiveModelOf[Pet, PetRM]
  object PetRM extends ReactiveModelOf.Companion[Pet, PetRM]

  case class Person(name: String, age: Int, address: Address, tags: List[String], pets: List[Pet])
  case class PersonRM(name: RV[String],
                      age: RV[Int],
                      address: AddressRM,
                      tags: RVList[String],
                      pets: RMList[Pet, PetRM]) extends ReactiveModelOf[Person, PersonRM]
  object PersonRM extends ReactiveModelOf.Companion[Person, PersonRM]

  /** コンパニオンを書かない場合(型を明示すれば変換できる) */
  case class Point(x: Int)
  case class PointRM(x: RV[Int]) extends ReactiveModelOf[Point, PointRM]

}

class ReactiveModelOfSuite extends munit.FunSuite {

  import ReactiveModelOfSuite.*

  private given ExecutionContext = scala.scalajs.concurrent.JSExecutionContext.queue

  /** マイクロタスク(ReactiveModelの通知)を待つ */
  private def flush(): Future[Unit] = {
    val p = Promise[Unit]()
    scala.scalajs.js.Dynamic.global.setTimeout(() => p.success(()), 0)
    p.future
  }

  private val taro = Person("Taro", 10, Address("Tokyo"), List("a"), List(Pet("Pochi")))

  test("ReactiveModelOf: case classと相互に変換できる") {
    val rm: PersonRM = RMCompatible.toRM(taro)
    assertEquals(rm.name.v, "Taro")
    assertEquals(rm.address.city.v, "Tokyo")
    assertEquals(rm.pets.v, List(Pet("Pochi")))
    assertEquals(rm.toOrigin, taro)
    assertEquals(rm.v, taro)
    assertEquals(RMCompatible.toRMs[Person, PersonRM](List(taro)).map(_.v), List(taro))
    assertEquals(PersonRM.from(taro).v, taro)
    val list: RMList[Person, PersonRM] = RMCompatible.toRMList(List(taro))
    assertEquals(list.v, List(taro))
  }

  test("ReactiveModelOf: := でまとめて書き換える") {
    val rm: PersonRM = RMCompatible.toRM(taro)
    val jiro = Person("Jiro", 20, Address("Osaka"), List("b", "c"), List(Pet("Tama"), Pet("Mike")))
    rm := jiro
    assertEquals(rm.v, jiro)
    assertEquals(rm.address.city.v, "Osaka")
  }

  test("ReactiveModelOf: フィールドと入れ子のモデルを集める") {
    val rm: PersonRM = RMCompatible.toRM(taro)
    assertEquals(rm.reactives.size, 5)
    assertEquals(rm.reactiveModels, List(rm.address))
  }

  test("ReactiveModelOf: フィールド・入れ子のモデルの変更を通知する") {
    val rm: PersonRM = RMCompatible.toRM(taro)
    var seen = List.empty[(Person, Person)]
    var beforeAbort = 0
    rm.addWatcher { case (o, n) => seen = seen :+ (o -> n) }
    rm.name := "Hanako"
    rm.address.city := "Kyoto"
    flush().flatMap { _ =>
      // 入れ子のモデルは自分の通知を1つ後のマイクロタスクで送るので、2回に分かれることがある
      assertEquals(seen.head._1, taro)
      assertEquals(seen.last._2, taro.copy(name = "Hanako", address = Address("Kyoto")))
      beforeAbort = seen.size
      rm.abort()
      rm.age := 99
      flush()
    }.map { _ =>
      assertEquals(seen.size, beforeAbort, "abortの後は通知しない")
    }
  }

  test("ReactiveModelOf: コンパニオンがなくても、変換は使える") {
    val rm = PointRM(new RV(1))
    assertEquals(rm.v, Point(1))
    rm := Point(2)
    assertEquals(rm.x.v, 2)
    assertEquals(summon[RMConversion[Point, PointRM]].toReactiveModel(Point(3)).v, Point(3))
  }

}
