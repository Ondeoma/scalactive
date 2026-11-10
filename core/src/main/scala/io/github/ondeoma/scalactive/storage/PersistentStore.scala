package io.github.ondeoma.scalactive.storage

import io.github.ondeoma.scalactive.reactive.*
import io.github.ondeoma.scalactive.utils.ConsoleUtil
import org.scalajs.dom

import scala.collection.mutable
import scala.scalajs.js

/** 保存先 */
enum StorageKind {

  /**
   * localStorage(ブラウザを閉じても残り、同じサイトの全てのタブで共有されます).
   *
   * @param sync trueの場合、他のタブでの変更をすぐに受け取って反映します
   */
  case Local(sync: Boolean = false)

  /** sessionStorage(タブごと. 再読み込みしても残り、タブを閉じると消えます). 他のタブとは共有しません. */
  case Session

}

/** ストレージの読み書き(テストではメモリ上のものに差し替えます) */
private[scalactive] trait StorageBackend {
  def get(key: String): Option[String]

  def set(key: String, value: String): Unit

  def remove(key: String): Unit

  def keys: List[String]

  /** storageイベントのstorageAreaと比べるための、元のStorage(なければnull) */
  def area: dom.Storage
}

private[scalactive] final class DomStorageBackend(val area: dom.Storage) extends StorageBackend {
  def get(key: String): Option[String] = Option(area.getItem(key))

  def set(key: String, value: String): Unit = area.setItem(key, value)

  def remove(key: String): Unit = area.removeItem(key)

  def keys: List[String] = (0 until area.length).flatMap(i => Option(area.key(i))).toList
}

/**
 * localStorage・sessionStorageに保存するリアクティブな値をまとめて定義します.
 *
 * {{{
 * object AppStore extends PersistentStore("app", StorageKind.Local(sync = true)) {
 *   val theme = persist("theme", "light")                  // RV[String]
 *   val todos = persistList("todos", List.empty[Todo])     // RVList[Todo]
 *   val history = persist("history", List.empty[String], sync = false) // この値だけ他のタブと共有しない
 * }
 * }}}
 *
 * - 値は `接頭辞.名前` のキーで、JSONとして保存します(型ごとの変換は [[StorageCodec]]).
 * - 作るときにストレージから読み込みます. 無い・読めない場合は初期値を使います.
 * - 変更は、同じ処理の中の変更をまとめて、現在の処理の直後(マイクロタスク)に書き込みます.
 * - ストレージが使えない場合(プライベートモードなど)は、保存せずにメモリ上だけで動きます.
 *
 * @param prefix  キーの接頭辞
 * @param kind    保存先
 * @param version 保存する形式のバージョン. 保存されているものと違う場合は、この接頭辞の値を全て消して初期値から始めます
 */
abstract class PersistentStore(prefix: String,
                               kind: StorageKind,
                               version: Int = 1) {

  private final class Slot(val key: String,
                           val reactive: Reactive[?],
                           val sync: Boolean,
                           val write: () => Unit,
                           val receive: Option[String] => Unit,
                           val reset: () => Unit)

  private val backend: Option[StorageBackend] = PersistentStore.backendOf(kind)
  private val slots = mutable.LinkedHashMap[String, Slot]()
  private val dirty = mutable.LinkedHashSet[Slot]()
  private var flushQueued = false

  /** このストアの値の既定の、他のタブとの共有 */
  protected val defaultSync: Boolean = kind match {
    case StorageKind.Local(s) => s
    case StorageKind.Session => false
  }

  checkVersion()

  private def fullKey(key: String): String = s"$prefix.$key"

  private def versionKey: String = s"$prefix.__version"

  private def checkVersion(): Unit = backend.foreach { b =>
    safely("read") {
      b.get(versionKey) match {
        case Some(v) if v == version.toString => ()
        case saved =>
          // 形式が変わった場合は古い値を捨てる(初めての場合は消すものがない)
          if (saved.nonEmpty) b.keys.filter(_.startsWith(prefix + ".")).foreach(b.remove)
          b.set(versionKey, version.toString)
      }
    }
  }

  private def safely(what: String)(f: => Unit): Unit = {
    try f
    catch {
      case e: Throwable => ConsoleUtil.warn(s"PersistentStore($prefix): failed to $what storage: ${e.getMessage}")
    }
  }

  private def load[A](key: String, codec: StorageCodec[A]): Option[A] = {
    var r: Option[A] = None
    safely("read") {
      r = backend.flatMap(_.get(fullKey(key))).flatMap(raw => decodeRaw(key, raw, codec))
    }
    r
  }

  private def decodeRaw[A](key: String, raw: String, codec: StorageCodec[A]): Option[A] = {
    val parsed: Either[String, A] =
      try codec.decode(js.JSON.parse(raw))
      catch {
        case e: Throwable => Left(e.getMessage)
      }
    parsed match {
      case Right(a) => Some(a)
      case Left(e) =>
        ConsoleUtil.warn(s"PersistentStore: ignored the saved value of '${fullKey(key)}' ($e)")
        None
    }
  }

  private def markDirty(slot: Slot): Unit = {
    dirty += slot
    if (!flushQueued) {
      flushQueued = true
      js.Dynamic.global.queueMicrotask(() => flush())
    }
  }

  private def flush(): Unit = {
    flushQueued = false
    val ss = dirty.toList
    dirty.clear()
    ss.foreach(s => safely("write")(s.write()))
  }

  /** 保存する値を登録します(各persistの共通部分). */
  private def register[A, R <: Reactive[?]](key: String,
                                            default: A,
                                            sync: Boolean,
                                            codec: StorageCodec[A])
                                           (make: A => R)
                                           (get: R => A,
                                            set: (R, A) => Unit): R = {
    if (slots.contains(key)) ConsoleUtil.warn(s"PersistentStore($prefix): '$key' is defined twice")
    val canSync = sync && (kind match {
      case StorageKind.Local(_) => true
      case StorageKind.Session =>
        ConsoleUtil.warn(s"PersistentStore($prefix): '$key' cannot sync with other tabs (sessionStorage)")
        false
    })
    val r = make(load(key, codec).getOrElse(default))
    // 他のタブから受け取った値・clearで戻した値は書き戻さない
    var quiet = false

    def setQuietly(a: A): Unit = {
      quiet = true
      try set(r, a)
      finally quiet = false
    }

    lazy val slot: Slot = new Slot(
      key,
      r,
      canSync,
      () => backend.foreach(_.set(fullKey(key), js.JSON.stringify(codec.encode(get(r))))),
      {
        case Some(raw) => decodeRaw(key, raw, codec).foreach(setQuietly)
        case None => setQuietly(default)
      },
      () => setQuietly(default)
    )
    r.addWatcher(_ => if (!quiet) markDirty(slot))
    slots.update(key, slot)
    if (canSync) PersistentStore.listen(this)
    r
  }

  /** 値を保存するRVを作ります. */
  protected def persist[A](key: String,
                           default: A,
                           sync: Boolean = defaultSync)
                          (using codec: StorageCodec[A]): RV[A] = {
    register(key, default, sync, codec)(a => new RV(a))(_.v, _ := _)
  }

  /** リストを保存するRVListを作ります. */
  protected def persistList[A](key: String,
                               default: List[A],
                               sync: Boolean = defaultSync)
                              (using codec: StorageCodec[List[A]]): RVList[A] = {
    register(key, default, sync, codec)(as => new RVList(as.map(a => new RV(a))))(_.v, (r, as) => r := as)
  }

  /** リストを保存するReactiveListを作ります. */
  protected def persistReactiveList[A](key: String,
                                       default: List[A],
                                       sync: Boolean = defaultSync)
                                      (using codec: StorageCodec[List[A]]): ReactiveList[A] = {
    register(key, default, sync, codec)(as => new ReactiveList(as))(_.v, _ := _)
  }

  /** case classを保存するReactiveModelを作ります(保存するのは元のcase class). */
  protected def persistModel[Org, RM <: ReactiveModel[Org, RM]](key: String,
                                                                default: Org,
                                                                sync: Boolean = defaultSync)
                                                               (using rmc: RMCompatible[Org, RM],
                                                                codec: StorageCodec[Org]): RM = {
    register(key, default, sync, codec)(o => rmc.toReactiveModel(o))(_.v, _ := _)
  }

  /** case classのリストを保存するRMListを作ります. */
  protected def persistModelList[Org, RM <: ReactiveModel[Org, RM]](key: String,
                                                                    default: List[Org],
                                                                    sync: Boolean = defaultSync)
                                                                   (using rmc: RMCompatible[Org, RM],
                                                                    codec: StorageCodec[List[Org]]): RMList[Org, RM] = {
    register(key, default, sync, codec)(os => new RMList[Org, RM](os.map(rmc.toReactiveModel)))(_.v, _ := _)
  }

  /** 値を初期値に戻します(保存している値も初期値になります). */
  def reset(r: Reactive[?]): Unit = {
    slots.values.find(_.reactive eq r) match {
      case Some(s) =>
        s.reset()
        markDirty(s)
      case None => ConsoleUtil.warn(s"PersistentStore($prefix): the value is not defined in this store")
    }
  }

  /** このストアの値を全て初期値に戻し、保存している値を消します. */
  def clear(): Unit = {
    slots.values.foreach { s =>
      dirty -= s
      s.reset()
      backend.foreach(b => safely("write")(b.remove(fullKey(s.key))))
    }
  }

  /** storageイベント(他のタブでの変更)を受け取ります. */
  private[storage] def onStorage(e: dom.StorageEvent): Unit = {
    val fromThisArea = e.storageArea == null || backend.forall(b => b.area == null || (b.area eq e.storageArea))
    if (fromThisArea) {
      if (e.key == null) {
        // 他のタブでclear()された
        slots.values.filter(_.sync).foreach(_.receive(None))
      } else if (e.key.startsWith(prefix + ".")) {
        val key = e.key.stripPrefix(prefix + ".")
        slots.get(key).filter(_.sync).foreach(_.receive(Option(e.newValue)))
      }
    }
  }

}

object PersistentStore {

  private val listening = mutable.LinkedHashSet[PersistentStore]()
  private var handlerInstalled = false

  private[storage] def listen(store: PersistentStore): Unit = {
    listening += store
    if (!handlerInstalled) {
      handlerInstalled = true
      dom.window.addEventListener("storage", (e: dom.StorageEvent) => listening.foreach(_.onStorage(e)))
    }
  }

  /** 保存先(テストではメモリ上のものに差し替えます). 使えない場合はNone */
  private[scalactive] var backendOf: StorageKind => Option[StorageBackend] = kind => {
    try {
      val s = kind match {
        case StorageKind.Local(_) => dom.window.localStorage
        case StorageKind.Session => dom.window.sessionStorage
      }
      // プライベートモードなどで使えない場合は、ここで例外になる
      s.getItem("__scalactive_probe")
      Some(new DomStorageBackend(s))
    } catch {
      case e: Throwable =>
        ConsoleUtil.warn(s"PersistentStore: storage is not available (${e.getMessage}). Values are kept only in memory.")
        None
    }
  }

}
