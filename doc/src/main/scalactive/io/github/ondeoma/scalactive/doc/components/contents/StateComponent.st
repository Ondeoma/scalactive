<script lang="scala">
import Router.*
import io.github.ondeoma.scalactive.doc.components.contents.state.*

val storeCode = // language=scala
  """package io.github.ondeoma.scalactive.doc.components.contents.state
     |
     |import io.github.ondeoma.scalactive.reactive.*
     |
     |// 複数のコンポーネントで共有する状態
     |object CountStore {
     |
     |  val count: RV[Int] = RV(0)
     |
     |  def increment(): Unit = count := count.v + 1
     |
     |}
     |""".stripMargin

val prefsStoreCode = // language=scala
  """package io.github.ondeoma.scalactive.doc.components.contents.state
     |
     |import io.github.ondeoma.scalactive.components.ComponentManager.*
     |
     |case class Memo(text: String, done: Boolean = false) derives StorageCodec
     |
     |// localStorageに保存し、他のタブでの変更もすぐに反映する
     |object PrefsStore extends PersistentStore("scalactive-doc.prefs", StorageKind.Local(sync = true)) {
     |  val name: RV[String] = persist("name", "")
     |  val memos: RVList[Memo] = persistList("memos", Nil)
     |}
     |
     |// sessionStorageに保存する(タブごと. 再読み込みしても残る)
     |object DraftStore extends PersistentStore("scalactive-doc.draft", StorageKind.Session) {
     |  val draft: RV[String] = persist("draft", "")
     |}
     |""".stripMargin

val persistDemoCode = // language=html
  """<template>
     |  <div>
     |    <p>名前(localStorage. 他のタブにもすぐ反映): <input type="text" v-model="PrefsStore.name"></p>
     |    <p>下書き(sessionStorage. このタブだけ): <input type="text" v-model="DraftStore.draft"></p>
     |    <ul>
     |      <li v-for="memo in PrefsStore.memos">{{ memo.mapC(_.text) }}</li>
     |    </ul>
     |    <button @click='PrefsStore.memos.add(Memo("メモ " + (PrefsStore.memos.v.size + 1)))'>メモを追加</button>
     |    <button @click="PrefsStore.clear(); DraftStore.clear()">保存した値を消す</button>
     |  </div>
     |</template>
     |""".stripMargin

val persistApiCode = // language=scala
  """object AppStore extends PersistentStore("app", StorageKind.Local(sync = true), version = 1) {
     |  val theme   = persist("theme", "light")                         // RV[String]
     |  val count   = persist("count", 0, sync = false)                 // この値だけ他のタブと共有しない
     |  val todos   = persistList("todos", List.empty[Todo])            // RVList[Todo]
     |  val ids     = persistReactiveList("ids", List.empty[Long])      // ReactiveList[Long]
     |  val profile = persistModel("profile", Profile("taro", Nil))     // ProfileRM(ReactiveModel)
     |  val members = persistModelList("members", List.empty[Profile]) // RMList[Profile, ProfileRM]
     |}
     |
     |AppStore.reset(AppStore.theme) // 1つの値を初期値に戻す
     |AppStore.clear()               // このストアの値を全て初期値に戻し、保存した値を消す
     |""".stripMargin

val viewCode = // language=html
  """<template>
     |  <p>count: {{ CountStore.count }}</p>
     |</template>
     |""".stripMargin

val buttonCode = // language=html
  """<template>
     |  <button @click="CountStore.increment()">+1</button>
     |</template>
     |""".stripMargin
</script>

<template>
  <h1>状態管理</h1>

  <ul class="toc">
    <li><a @click='setHash("share")'>コンポーネント間での共有</a></li>
    <li><a @click='setHash("persist")'>保存とタブ間共有</a></li>
  </ul>

  <p>
    任意のobjectにReactive系の値を定義して、<br>
    各コンポーネントから参照する方法を推奨します.<br>
    値をlocalStorage・sessionStorageに保存したい場合は、<a @click='setHash("persist")'>保存とタブ間共有</a> を使います.
  </p>

  <h2 id="share">コンポーネント間での共有</h2>

  <p>
    以下は、objectに定義したRVを2つの.stで共有する例です.<br>
    objectの値はコンポーネントの外で作られるので、<br>
    コンポーネントが片付けられても残ります.
  </p>

  <div class="code-file">CountStore.scala</div>
  <pre><code class="language-scala">{{ storeCode }}</code></pre>

  <div class="code-file">CountView.st</div>
  <pre><code class="language-st">{{ viewCode }}</code></pre>

  <div class="code-file">CountButton.st</div>
  <pre><code class="language-st">{{ buttonCode }}</code></pre>

  <div class="rendered">
    <CountView />
    <CountButton />
    <CountView />
  </div>

  <p>
    どちらのCountViewも、同じRVを表示しています.<br>
    ボタンを押すと、両方が更新されます.
  </p>

  <h2 id="persist">保存とタブ間共有</h2>

  <p>
    <code>PersistentStore</code> を継承したobjectに値を定義すると、<br>
    localStorage・sessionStorageに保存され、再読み込みしても残ります.<br>
    保存先と、他のタブとの共有の有無はストアごとに指定し、値ごとに上書きすることもできます.
  </p>

  <div class="code-file">PrefsStore.scala</div>
  <pre><code class="language-scala">{{ prefsStoreCode }}</code></pre>

  <div class="code-file">PersistDemo.st</div>
  <pre><code class="language-st">{{ persistDemoCode }}</code></pre>

  <div class="rendered">
    <PersistDemo />
  </div>

  <p>
    このページを別のタブでも開くと、名前とメモは両方のタブにすぐ反映され、<br>
    下書きはタブごとに別々に保存されます.
  </p>

  <h3>保存先</h3>

  <ul>
    <li>
      <code>StorageKind.Local(sync = true)</code> : localStorageに保存します.<br>
      <code>sync = true</code> の場合は、他のタブでの変更をすぐに受け取って反映します.<br>
      <code>sync = false</code> の場合は、再読み込みした時に最新の値になります.
    </li>
    <li>
      <code>StorageKind.Session</code> : sessionStorageに保存します.<br>
      タブごとの値で、他のタブとは共有しません.
    </li>
  </ul>

  <h3>定義できる値</h3>

  <pre><code class="language-scala">{{ persistApiCode }}</code></pre>

  <ul>
    <li>
      値は <code>接頭辞.名前</code> のキーで、JSONとして保存します.<br>
      作る時に読み込み、保存されていない・読めない場合は初期値を使います.
    </li>
    <li>
      変更は、同じ処理の中の変更をまとめて、現在の処理の直後に書き込みます.<br>
      リストの行やReactiveModelの項目の変更も保存されます.
    </li>
    <li>
      String・数値・Boolean・Option・List・Vector・Set・Map[String, A]はそのまま保存できます.<br>
      case class・enumは <code>derives StorageCodec</code> を付けてください(項目の型も自動で変換します).<br>
      それ以外の型は <code>StorageCodec.from</code> / <code>StorageCodec.map</code> で変換を定義します.
    </li>
    <li>
      保存する形式を変えた場合は <code>version</code> を上げてください.<br>
      保存されているバージョンと違う場合は、その接頭辞の値を全て消して初期値から始めます.
    </li>
    <li>
      ストレージが使えない場合(プライベートモードなど)は、保存せずにメモリ上だけで動きます.
    </li>
  </ul>
</template>
