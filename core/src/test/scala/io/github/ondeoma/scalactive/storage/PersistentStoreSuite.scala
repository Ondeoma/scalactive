package io.github.ondeoma.scalactive.storage

import io.github.ondeoma.scalactive.reactive.*
import org.scalajs.dom

import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

/** メモリ上の保存先 */
final class MemoryBackend extends StorageBackend {
  val data: mutable.LinkedHashMap[String, String] = mutable.LinkedHashMap()

  def get(key: String): Option[String] = data.get(key)

  def set(key: String, value: String): Unit = data.update(key, value)

  def remove(key: String): Unit = data.remove(key)

  def keys: List[String] = data.keys.toList

  def area: dom.Storage = null
}

case class Todo(title: String, done: Boolean = false) derives StorageCodec

enum Shape derives StorageCodec {
  case Circle(r: Double)
  case Square(side: Double)
  case Empty
}

case class Profile(name: String, tags: List[String], age: Option[Int]) derives StorageCodec

case class ProfileRM(name: RV[String], tags: RV[List[String]], age: RV[Option[Int]])
  extends ReactiveModelOf[Profile, ProfileRM]

object ProfileRM extends ReactiveModelOf.Companion[Profile, ProfileRM]

class LocalStore(prefix: String = "t") extends PersistentStore(prefix, StorageKind.Local(sync = true)) {
  val theme: RV[String] = persist("theme", "light")
  val count: RV[Int] = persist("count", 0)
  val local: RV[Int] = persist("local", 0, sync = false)
  val todos: RVList[Todo] = persistList("todos", List(Todo("a")))
  val ids: ReactiveList[Long] = persistReactiveList("ids", Nil)
  val profile: ProfileRM = persistModel("profile", Profile("taro", Nil, None))
  val profiles: RMList[Profile, ProfileRM] = persistModelList("profiles", Nil)
  val shape: RV[Shape] = persist("shape", Shape.Empty)
}

class SessionStore extends PersistentStore("s", StorageKind.Session) {
  val draft: RV[String] = persist("draft", "")
}

class VersionedStore(v: Int) extends PersistentStore("v", StorageKind.Local(), version = v) {
  val n: RV[Int] = persist("n", 0)
}

class PersistentStoreSuite extends munit.FunSuite {

  private var backend = new MemoryBackend
  private val saved = PersistentStore.backendOf

  override def beforeEach(context: BeforeEach): Unit = {
    backend = new MemoryBackend
    val b = backend
    PersistentStore.backendOf = _ => Some(b)
  }

  override def afterAll(): Unit = {
    PersistentStore.backendOf = saved
  }

  private def tick(): Future[Unit] = js.Promise.resolve[Unit](()).toFuture

  private def fireStorage(key: String | Null, newValue: String | Null): Unit = {
    val init = js.Dynamic.literal(key = key.asInstanceOf[js.Any], newValue = newValue.asInstanceOf[js.Any])
    val e = js.Dynamic.newInstance(js.Dynamic.global.StorageEvent)("storage", init).asInstanceOf[dom.Event]
    dom.window.dispatchEvent(e)
  }

  // ---- StorageCodec ----

  private def roundTrip[A](a: A)(using c: StorageCodec[A]): Either[String, A] = {
    c.decode(js.JSON.parse(js.JSON.stringify(c.encode(a))))
  }

  test("標準の型と、derivesしたcase class・enumを変換できる") {
    assertEquals(roundTrip("あ"), Right("あ"))
    assertEquals(roundTrip(42), Right(42))
    assertEquals(roundTrip(Long.MaxValue), Right(Long.MaxValue))
    assertEquals(roundTrip(1.5), Right(1.5))
    assertEquals(roundTrip(true), Right(true))
    assertEquals(roundTrip(Option(3)), Right(Some(3)))
    assertEquals(roundTrip(Option.empty[Int]), Right(None))
    assertEquals(roundTrip(Vector(1, 2)), Right(Vector(1, 2)))
    assertEquals(roundTrip(Set("a")), Right(Set("a")))
    assertEquals(roundTrip(Map("x" -> List(1))), Right(Map("x" -> List(1))))
    assertEquals(roundTrip(Profile("a", List("t"), Some(3))), Right(Profile("a", List("t"), Some(3))))
    assertEquals(roundTrip[Shape](Shape.Circle(2)), Right(Shape.Circle(2)))
    assertEquals(roundTrip[Shape](Shape.Empty), Right(Shape.Empty))
  }

  test("形式が違う値はLeftになり、Optionの項目は無くてもよい") {
    val c = StorageCodec[Profile]
    assert(c.decode(js.JSON.parse("""{"name": 1, "tags": []}""")).isLeft)
    assertEquals(c.decode(js.JSON.parse("""{"name": "a", "tags": []}""")), Right(Profile("a", Nil, None)))
    assert(StorageCodec[Int].decode(1.5).isLeft)
  }

  // ---- PersistentStore ----

  test("保存されていなければ初期値を使い、変更をまとめて書き込む") {
    val s = new LocalStore
    assertEquals(s.theme.v, "light")
    assertEquals(backend.get("t.theme"), None)
    s.theme := "dark"
    s.theme := "blue"
    s.count := 3
    assertEquals(backend.get("t.theme"), None, "書き込みはマイクロタスクで行う")
    tick().map { _ =>
      assertEquals(backend.get("t.theme"), Some("\"blue\""))
      assertEquals(backend.get("t.count"), Some("3"))
      assertEquals(backend.get("t.__version"), Some("1"))
    }
  }

  test("保存されている値を読み込む(読めない値は初期値にする)") {
    backend.set("t.theme", "\"dark\"")
    backend.set("t.count", "\"oops\"")
    backend.set("t.todos", """[{"title":"x","done":true}]""")
    backend.set("t.profile", """{"name":"hanako","tags":["a"],"age":20}""")
    backend.set("t.shape", """{"$type":"Square","value":{"side":2}}""")
    val s = new LocalStore
    assertEquals(s.theme.v, "dark")
    assertEquals(s.count.v, 0)
    assertEquals(s.todos.v, List(Todo("x", done = true)))
    assertEquals(s.profile.v, Profile("hanako", List("a"), Some(20)))
    assertEquals(s.profile.name.v, "hanako")
    assertEquals(s.shape.v, Shape.Square(2))
  }

  test("リスト・モデルの変更(行・項目の変更を含む)も保存する") {
    val s = new LocalStore
    s.todos.add(Todo("b"))
    s.ids.add(10L)
    s.profile.name := "jiro"
    s.profiles.add(Profile("p", Nil, Some(1)))
    // ReactiveModelの項目の変化は、モデルからの通知の後に書き込まれる
    tick().flatMap(_ => tick()).map { _ =>
      assertEquals(backend.get("t.todos"), Some("""[{"title":"a","done":false},{"title":"b","done":false}]"""))
      assertEquals(backend.get("t.ids"), Some("""["10"]"""))
      assertEquals(backend.get("t.profile").map(js.JSON.parse(_).name.asInstanceOf[String]), Some("jiro"))
      assertEquals(backend.get("t.profiles"), Some("""[{"name":"p","tags":[],"age":1}]"""))
      s.todos.update(0, Todo("a2"))
    }.flatMap(_ => tick()).map { _ =>
      assert(backend.get("t.todos").exists(_.contains("a2")))
    }
  }

  test("他のタブでの変更を受け取り、書き戻さない. sync = false の値は受け取らない") {
    val s = new LocalStore("sync")
    var log = List.empty[String]
    s.theme.addWatcher { case (_, n) => log :+= n }
    fireStorage("sync.theme", "\"green\"")
    fireStorage("sync.local", "5")
    fireStorage("other.theme", "\"red\"")
    assertEquals(s.theme.v, "green")
    assertEquals(s.local.v, 0)
    assertEquals(log, List("green"))
    // 他のタブで消された値は初期値に戻す
    fireStorage("sync.theme", null)
    assertEquals(s.theme.v, "light")
    tick().map { _ =>
      assertEquals(backend.get("sync.theme"), None, "受け取った値は書き戻さない")
    }
  }

  test("他のタブでclearされたら、共有している値を初期値に戻す") {
    val s = new LocalStore("cl")
    s.count := 9
    fireStorage(null, null)
    assertEquals(s.count.v, 0)
  }

  test("sessionStorageの値は他のタブと共有しない") {
    val s = new SessionStore
    fireStorage("s.draft", "\"x\"")
    assertEquals(s.draft.v, "")
    s.draft := "書きかけ"
    tick().map(_ => assertEquals(backend.get("s.draft"), Some("\"書きかけ\"")))
  }

  test("resetは値を初期値に戻して保存し、clearは保存している値を消す") {
    val s = new LocalStore
    s.theme := "dark"
    s.count := 2
    tick().flatMap { _ =>
      s.reset(s.theme)
      assertEquals(s.theme.v, "light")
      tick()
    }.map { _ =>
      assertEquals(backend.get("t.theme"), Some("\"light\""))
      s.clear()
      assertEquals(s.count.v, 0)
      assertEquals(backend.get("t.count"), None)
      assertEquals(backend.get("t.theme"), None)
    }
  }

  test("バージョンが変わったら、古い値を捨てる") {
    backend.set("v.__version", "1")
    backend.set("v.n", "5")
    backend.set("other.n", "7")
    assertEquals(new VersionedStore(1).n.v, 5)
    assertEquals(new VersionedStore(2).n.v, 0)
    assertEquals(backend.get("v.__version"), Some("2"))
    assertEquals(backend.get("other.n"), Some("7"), "他の接頭辞の値は消さない")
  }

  test("ストレージが使えなければ、メモリ上だけで動く") {
    PersistentStore.backendOf = _ => None
    val s = new LocalStore("mem")
    s.theme := "dark"
    tick().map(_ => assertEquals(s.theme.v, "dark"))
  }

}
