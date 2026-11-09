<script lang="scala">
import Router.*
import io.github.ondeoma.scalactive.doc.components.contents.component.*

val helloCode = // language=html
  """<script lang="scala">
     |def greet(): Unit = window.alert("Hello!")
     |</script>
     |
     |<template>
     |  <div class="hello">
     |    <p>これは Hello です.</p>
     |    <button @click="greet()">Click</button>
     |  </div>
     |</template>
     |
     |<style scoped>
     |.hello p { color: seagreen; }
     |</style>
     |""".stripMargin

val counterCode = // language=html
  """<script lang="scala">
     |case class State(count: RV[Int] = RV(0))
     |
     |def initState() = State()
     |</script>
     |
     |<template>
     |  <div>
     |    <p>count: {{ s.count }}</p>
     |    <button @click="s.count := s.count.v + 1">+1</button>
     |  </div>
     |</template>
     |""".stripMargin

val stateNameCode = // language=html
  """<template state="counter">
     |  <div>
     |    <p>count: {{ counter.count }}</p>
     |    <button @click="counter.count := counter.count.v + 1">+1</button>
     |  </div>
     |</template>
     |""".stripMargin

val greetingCode = // language=html
  """<script lang="scala">
     |case class Props(userName: String, count: Reactive[Int] = RV(0))
     |</script>
     |
     |<template>
     |  <p>こんにちは、{{ props.userName }}さん. count = {{ props.count }}</p>
     |</template>
     |""".stripMargin

val greetingParentCode = // language=html
  """<script lang="scala">
     |case class State(count: RV[Int] = RV(0))
     |
     |def initState() = State()
     |</script>
     |
     |<template>
     |  <div>
     |    <Greeting user-name="Scala" />
     |    <Greeting user-name="Scalactive" :count="s.count" />
     |    <button @click="s.count := s.count.v + 1">+1</button>
     |  </div>
     |</template>
     |""".stripMargin

val closableCode = // language=html
  """<script lang="scala">
     |case class Props(message: String, onClose: Unit => Unit = _ => ())
     |</script>
     |
     |<template>
     |  <p>{{ props.message }} <button @click="props.onClose(())">×</button></p>
     |</template>
     |""".stripMargin

val closableParentCode = // language=html
  """<script lang="scala">
     |case class State(open: RV[Boolean] = RV(true))
     |
     |def initState() = State()
     |</script>
     |
     |<template>
     |  <div>
     |    <Closable v-if="s.open" message="×で閉じます." @close="s.open := false" />
     |    <button v-else @click="s.open := true">再表示</button>
     |  </div>
     |</template>
     |""".stripMargin

val nameFieldCode = // language=html
  """<script lang="scala">
     |case class Props(label: String, name: RV[String], note: RV[String] = RV(""))
     |</script>
     |
     |<template>
     |  <div>
     |    <label>{{ props.label }}: <input type="text" v-model="props.name"></label>
     |    <label>メモ: <input type="text" v-model="props.note"></label>
     |  </div>
     |</template>
     |""".stripMargin

val nameFieldParentCode = // language=html
  """<script lang="scala">
     |case class State(name: RV[String] = RV("Scala"), memo: RV[String] = RV(""))
     |
     |def initState() = State()
     |</script>
     |
     |<template>
     |  <div>
     |    <NameField label="名前" :name="s.name" :note="s.memo" />
     |    <p>名前: {{ s.name }} / メモ: {{ s.memo }}</p>
     |  </div>
     |</template>
     |""".stripMargin

val panelCode = // language=html
  """<script lang="scala">
     |case class Props(title: String,
     |                 slot: Slot = Slot.empty,
     |                 footer: Slot = Slot.empty)
     |</script>
     |
     |<template>
     |  <div class="panel">
     |    <b>{{ props.title }}</b>
     |    <slot><p>(内容がありません)</p></slot>
     |    <small><slot name="footer">(footerなし)</slot></small>
     |  </div>
     |</template>
     |
     |<style scoped>
     |.panel { border: 1px solid #ccc; border-radius: 6px; padding: 8px; margin: 4px 0; }
     |</style>
     |""".stripMargin

val panelParentCode = // language=html
  """<script lang="scala">
     |case class State(count: RV[Int] = RV(0))
     |
     |def initState() = State()
     |</script>
     |
     |<template>
     |  <div>
     |    <Panel title="お知らせ">
     |      <p>親の状態も使えます. count = {{ s.count }}</p>
     |      <button @click="s.count := s.count.v + 1">+1</button>
     |      <template #footer>名前付きスロット(#footer)の内容です.</template>
     |    </Panel>
     |    <Panel title="slotなし" />
     |  </div>
     |</template>
     |""".stripMargin

val itemListCode = // language=html
  """<script lang="scala">
     |case class Props(items: List[String],
     |                 item: ScopedSlot[(String, Int)] = ScopedSlot.empty)
     |</script>
     |
     |<template>
     |  <ul>
     |    <li v-for="(x, i) in props.items"><slot name="item" :value="(x, i)">{{ x }}</slot></li>
     |  </ul>
     |</template>
     |""".stripMargin

val itemListParentCode = // language=html
  """<script lang="scala">
     |val langs = List("Scala", "Scala.js", "Scalactive")
     |</script>
     |
     |<template>
     |  <div>
     |    <ItemList :items="langs" />
     |    <ItemList :items="langs">
     |      <template #item="(name, i)"><b>{{ i + 1 }}.</b> {{ name }}</template>
     |    </ItemList>
     |  </div>
     |</template>
     |""".stripMargin

val chipCode = // language=html
  """<script lang="scala">
     |case class Props(label: String)
     |</script>
     |
     |<template>
     |  <span class="chip">{{ props.label }}</span>
     |</template>
     |
     |<style scoped>
     |.chip { display: inline-block; padding: 0 8px; border-radius: 4px; background: #ddd; color: #222; }
     |</style>
     |""".stripMargin

val chipParentCode = // language=html
  """<script lang="scala">
     |case class State(on: RV[Boolean] = RV(false))
     |
     |def initState() = State()
     |</script>
     |
     |<template>
     |  <div>
     |    <Chip label="そのまま" />
     |    <Chip label="引き継ぎ" :style='Map("background" -> s.on.mapC(o => if (o) "gold" else "#ddd"))' data-demo="chip" />
     |    <button @click="s.on := !s.on.v">色を切り替え</button>
     |  </div>
     |</template>
     |""".stripMargin

val clockCode = // language=html
  """<script lang="scala">
     |case class State(now: RV[String] = RV(""), var timer: Int = 0)
     |
     |def initState() = State()
     |
     |def tick(s: State): Unit = s.now := new scala.scalajs.js.Date().toLocaleTimeString()
     |
     |// DOMに挿入された後に呼ばれる
     |def mounted(s: State): Unit = {
     |  tick(s)
     |  s.timer = window.setInterval(() => tick(s), 1000)
     |}
     |
     |// 片付けられた(DOMから取り除かれた)後に呼ばれる
     |def unmounted(s: State): Unit = window.clearInterval(s.timer)
     |</script>
     |
     |<template>
     |  <p>現在時刻: {{ s.now }}</p>
     |</template>
     |""".stripMargin

val focusInputCode = // language=html
  """<script lang="scala">
     |case class State(text: RV[String] = RV("Scalactive"),
     |                 input: ElementRef[HTMLInputElement] = ElementRef())
     |
     |def initState() = State()
     |</script>
     |
     |<template>
     |  <div>
     |    <input type="text" ref="s.input" v-model="s.text">
     |    <button @click="s.input.foreach(_.focus())">入力欄へ</button>
     |    <button @click="s.input.foreach(_.select())">全選択</button>
     |  </div>
     |</template>
     |""".stripMargin

val modalCode = // language=html
  """<script lang="scala">
     |case class State(open: RV[Boolean] = RV(false))
     |
     |def initState() = State()
     |</script>
     |
     |<template>
     |  <div>
     |    <button @click="s.open := true">モーダルを開く</button>
     |    <Teleport v-if="s.open" to="body">
     |      <div class="modal" @click.self="s.open := false">
     |        <div class="modal-body">
     |          <p>bodyの直下に描画されています.</p>
     |          <button @click="s.open := false">閉じる</button>
     |        </div>
     |      </div>
     |    </Teleport>
     |  </div>
     |</template>
     |
     |<style scoped>
     |.modal { position: fixed; inset: 0; background: rgba(0, 0, 0, 0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; }
     |.modal-body { background: #fff; color: #222; padding: 16px; border-radius: 6px; }
     |</style>
     |""".stripMargin

val boundaryCode = // language=html
  """<script lang="scala">
     |case class State(crash: RV[Boolean] = RV(false))
     |
     |def initState() = State()
     |
     |// 描画中に例外を起こす
     |def boom(): String = throw new RuntimeException("描画中にエラーが起きました.")
     |</script>
     |
     |<template>
     |  <div>
     |    <ErrorBoundary @error="println(event)">
     |      <p>中身: <span v-if="s.crash">{{ boom() }}</span><span v-else>正常です.</span></p>
     |      <button @click="s.crash := true">エラーを起こす</button>
     |      <template #fallback="err">
     |        <p>{{ err.message }} <button @click="s.crash := false; err.retry()">戻す</button></p>
     |      </template>
     |    </ErrorBoundary>
     |  </div>
     |</template>
     |""".stripMargin

val loaderCode = // language=html
  """<script lang="scala">
     |import scala.concurrent.{Future, Promise}
     |
     |case class State(fail: RV[Boolean] = RV(false), on: RV[Boolean] = RV(true))
     |
     |def initState() = State()
     |
     |// 0.8秒後に完了する非同期の処理
     |def fetchMessage(fail: Boolean): Future[String] = {
     |  val p = Promise[String]()
     |  window.setTimeout(() => {
     |    if (fail) p.failure(new RuntimeException("読み込みに失敗しました."))
     |    else p.success("読み込みました.")
     |  }, 800)
     |  p.future
     |}
     |</script>
     |
     |<template>
     |  <div>
     |    <label><input type="checkbox" v-model="s.fail"> 失敗させる</label>
     |    <button @click="s.on := false; s.on := true">再読み込み</button>
     |    <Await v-if="s.on" :load="() => fetchMessage(s.fail.v)">
     |      <template #pending><p>読み込み中...</p></template>
     |      <template #default="msg"><p>{{ msg }}</p></template>
     |      <template #error="err"><p>{{ err.message }} <button @click="err.retry()">再試行</button></p></template>
     |    </Await>
     |  </div>
     |</template>
     |""".stripMargin

val lazyLoadCode = // language=html
  """<script lang="scala">
     |import scala.scalajs.js
     |
     |// 描画されるときに初めてHelloのコードを読み込む
     |object LazyHello {
     |  def apply(): GenComponentF =
     |    asyncComponent(() => js.dynamicImport[GenComponentF](Hello()).toFuture, _ => "<p>読み込み中...</p>")
     |}
     |
     |case class State(show: RV[Boolean] = RV(false))
     |
     |def initState() = State()
     |</script>
     |
     |<template>
     |  <div>
     |    <button @click="s.show := true">読み込む</button>
     |    <LazyHello v-if="s.show" />
     |  </div>
     |</template>
     |""".stripMargin

val errorHandlerCode = // language=scala
  """// アプリの起動時(main)に設定する
     |ScalactiveConfig.errorHandler = e => {
     |  org.scalajs.dom.console.error(e.getMessage)
     |  // エラーの報告サービスへ送るなど
     |}
     |""".stripMargin

val lazyRouteCode = // language=html
  """<!-- /admin を開いたときに初めてAdmin.stのコードを読み込む -->
     |<script lang="scala">
     |import scala.scalajs.js
     |import io.github.ondeoma.scalactive.components.routes.RouterComponent
     |
     |val routes: PathPatternRoutes = List(
     |  ("/", _ => Home()),
     |  ("/admin", _ => asyncComponent(() => js.dynamicImport[GenComponentF](Admin()).toFuture, _ => "<p>読み込み中...</p>")),
     |)
     |</script>
     |
     |<template>
     |  <RouterComponent :routes="routes" />
     |</template>
     |""".stripMargin
</script>

<template>
  <h1>コンポーネント</h1>

  <p>
    コンポーネントはHTML＋処理＋状態のような概念です.<br>
    1つの <code>.st</code> ファイルに、1つのコンポーネントを書きます.<br>
    Vueの単一ファイルコンポーネントのように、テンプレート・Scalaコード・CSSを1つのファイルにまとめて記述します.<br>
    sbtプラグインの設定やファイルの置き場所は<a @click='go("/start")'>クイックスタート</a>を参照ください.
  </p>

  <ul class="toc">
    <li><a @click='setHash("format")'>ファイルの構成</a></li>
    <li><a @click='setHash("state")'>状態を持つコンポーネント</a></li>
    <li><a @click='setHash("props")'>props</a></li>
    <li><a @click='setHash("emit")'>子からのイベント</a></li>
    <li><a @click='setHash("model")'>双方向バインディング</a></li>
    <li><a @click='setHash("slot")'>slot</a></li>
    <li><a @click='setHash("attrs")'>属性の引き継ぎ</a></li>
    <li><a @click='setHash("lifecycle")'>ライフサイクルフック</a></li>
    <li><a @click='setHash("ref")'>テンプレート参照</a></li>
    <li><a @click='setHash("teleport")'>Teleport</a></li>
    <li><a @click='setHash("errorBoundary")'>エラーの境界</a></li>
    <li><a @click='setHash("await")'>非同期データ</a></li>
    <li><a @click='setHash("lazy")'>遅延読み込み</a></li>
    <li><a @click='setHash("directives")'>テンプレート構文の一覧</a></li>
    <li><a @click='setHash("limitations")'>制限事項</a></li>
  </ul>

  <h2 id="format">ファイルの構成</h2>

  <p>
    <code>.st</code> ファイルは3つのブロックから成ります.
  </p>

  <ul>
    <li>
      <code>&lt;script lang="scala"&gt;</code> : 処理や状態の定義をScalaで書きます(省略可).<br>
      生成されるobjectの本体になります.
    </li>
    <li><code>&lt;template&gt;</code> : HTMLを書きます(必須).</li>
    <li>
      <code>&lt;style&gt;</code> : CSSを書きます(省略可).<br>
      初回利用時にheadへ挿入されます.<br>
      <code>scoped</code> を付けると、このコンポーネントの要素にのみ適用されます.
    </li>
  </ul>

  <pre><code class="language-st">{{ helloCode }}</code></pre>

  <div class="rendered">
    <Hello />
  </div>

  <p>
    <code>src/main/scalactive</code> と <code>src/main/scala</code> 配下の <code>*.st</code> が変換対象です.<br>
    ディレクトリ構成がパッケージに、ファイル名がobject名になります.<br>
    (<code>&lt;script&gt;</code>の先頭に <code>package</code> 宣言を書いた場合は、そちらが優先されます.)<br>
    作成したコンポーネントは、<br>
    他の.stのテンプレートから <code>&lt;Hello /&gt;</code> のようにタグで呼び出せます.<br>
    別のパッケージのコンポーネントは、<br>
    <code>&lt;script&gt;</code> でimportしてください.<br>
    テンプレートの構文は <a @click='setHash("directives")'>テンプレート構文の一覧</a> を参照してください.
  </p>

  <h2 id="state">状態を持つコンポーネント</h2>

  <p>
    <code>&lt;script&gt;</code> に <code>def initState()</code> を定義すると、<br>
    状態を持つコンポーネントになります.<br>
    テンプレートからは状態を <code>s</code> で参照できます.<br>
    状態はコンポーネントを表示するたびに作られます.<br>
    状態が不要な場合は、initStateを定義しません.
  </p>

  <pre><code class="language-st">{{ counterCode }}</code></pre>

  <div class="rendered">
    <Counter />
  </div>

  <p>
    状態の名前は、<code>&lt;template&gt;</code> の <code>state</code> 属性で変更できます.<br>
    状態の型に合わせた名前を付けると、テンプレートが読みやすくなります.
  </p>

  <pre><code class="language-st">{{ stateNameCode }}</code></pre>

  <h2 id="props">props</h2>

  <p>
    <code>&lt;script&gt;</code> に <code>case class Props(...)</code> を定義すると、<br>
    親から値を受け取れます.<br>
    テンプレートからは <code>props</code> で参照できます.<br>
    (状態と同じく、<code>&lt;template props="p"&gt;</code> のように名前を変更できます.)
  </p>

  <div class="code-file">Greeting.st</div>
  <pre><code class="language-st">{{ greetingCode }}</code></pre>

  <p>親からは、タグの属性で値を渡します.</p>

  <div class="code-file">GreetingParent.st</div>
  <pre><code class="language-st">{{ greetingParentCode }}</code></pre>

  <div class="rendered">
    <GreetingParent />
  </div>

  <ul>
    <li>
      <code>name="text"</code> は文字列、値なしの <code>name</code> は <code>true</code>、<br>
      <code>:name="expr"</code> はScalaの式になります.
    </li>
    <li>ケバブケースの属性名は、キャメルケースに変換されます(<code>user-name</code> → <code>userName</code>).</li>
    <li>
      デフォルト値のあるpropsは省略できます.<br>
      すべて省略可能なら、属性なしのタグ(<code>&lt;Greeting /&gt;</code> など)で書けます.
    </li>
    <li>
      propsは生成時に1度だけ渡されます.<br>
      値の変化を子に伝えたい場合は、Reactiveを渡してください.
    </li>
    <li>
      <code>def initState()</code> と併用できます.<br>
      初期状態をpropsから作る場合は、<code>def initState(props: Props)</code> と定義します.
    </li>
  </ul>

  <h2 id="emit">子からのイベント</h2>

  <p>
    子コンポーネントのタグに書いた <code>@name="式"</code> は、<br>
    propsの <code>onName</code> に関数として渡されます.<br>
    子は <code>onName: A =&gt; Unit</code> をPropsに定義し、<br>
    <code>props.onName(a)</code> で親へ通知します.<br>
    渡す値がない場合は <code>Unit =&gt; Unit</code> にします.<br>
    親の文の中では、<code>event</code> で子から渡された値を参照できます.
  </p>

  <div class="code-file">Closable.st</div>
  <pre><code class="language-st">{{ closableCode }}</code></pre>

  <div class="code-file">ClosableParent.st</div>
  <pre><code class="language-st">{{ closableParentCode }}</code></pre>

  <div class="rendered">
    <ClosableParent />
  </div>

  <h2 id="model">双方向バインディング</h2>

  <p>
    子コンポーネントのpropsに、RVをそのまま渡します.<br>
    親と子が同じRVを共有するので、<br>
    どちらで変更しても両方に反映されます.
  </p>

  <div class="code-file">NameField.st</div>
  <pre><code class="language-st">{{ nameFieldCode }}</code></pre>

  <div class="code-file">NameFieldParent.st</div>
  <pre><code class="language-st">{{ nameFieldParentCode }}</code></pre>

  <div class="rendered">
    <NameFieldParent />
  </div>

  <ul>
    <li>
      子のpropsの型はRV(<code>RV[String]</code> など)にします.<br>
      修飾子(<code>.trim</code> など)は、子の中のinputに指定してください.
    </li>
    <li>
      Vueと同じく、<code>v-model="rv"</code> / <code>v-model:note="rv"</code> とも書けます.<br>
      それぞれ <code>:model="rv"</code> / <code>:note="rv"</code> と同じ意味です.
    </li>
  </ul>

  <h2 id="slot">slot</h2>

  <p>
    子コンポーネントのタグの中に書いた要素は、<br>
    Propsの <code>slot: Slot</code> として渡され、<br>
    子の <code>&lt;slot&gt;</code> の位置に展開されます.<br>
    <code>&lt;template #name&gt;</code> で囲んだ要素は名前付きスロットとして、<br>
    Propsの <code>name: Slot</code> に渡され、<br>
    子の <code>&lt;slot name="name"&gt;</code> の位置に展開されます.
  </p>

  <div class="code-file">Panel.st</div>
  <pre><code class="language-st">{{ panelCode }}</code></pre>

  <div class="code-file">PanelParent.st</div>
  <pre><code class="language-st">{{ panelParentCode }}</code></pre>

  <div class="rendered">
    <PanelParent />
  </div>

  <ul>
    <li>
      slotの内容は親のスコープで評価されるため、親の状態を参照できます.<br>
      scoped CSSも親のものが適用されます.
    </li>
    <li>
      名前付きスロットを受け取るには、子のPropsに <code>name: Slot = Slot.empty</code> を定義します.<br>
      (ケバブケースの名前はキャメルケースになります.)
    </li>
    <li><code>&lt;template #name&gt;</code> は <code>&lt;template v-slot:name&gt;</code> とも書けます.</li>
    <li><code>&lt;template #default&gt;</code> はデフォルトのslotです.</li>
    <li><code>&lt;slot&gt;</code> の中身は、そのスロットが渡されなかった場合の既定の内容です.</li>
  </ul>

  <h3 id="scoped-slot">スコープ付きスロット</h3>

  <p>
    子から値を受け取って、内容を作るスロットです.<br>
    リストのような汎用部品で、行の表示を親に任せる場合に使います.<br>
    子はPropsに <code>name: ScopedSlot[A]</code> を定義し、<br>
    <code>&lt;slot name="name" :value="expr"&gt;</code> で値を渡します.<br>
    親は <code>&lt;template #name="x"&gt;</code> で値を受け取ります.
  </p>

  <div class="code-file">ItemList.st</div>
  <pre><code class="language-st">{{ itemListCode }}</code></pre>

  <div class="code-file">ItemListParent.st</div>
  <pre><code class="language-st">{{ itemListParentCode }}</code></pre>

  <div class="rendered">
    <ItemListParent />
  </div>

  <ul>
    <li>
      受け取った値の型は、子のPropsから推論されます.<br>
      <code>#item="x: (String, Int)"</code> のように型を書くこともできます.
    </li>
    <li>
      複数の値はタプルで渡し、<br>
      <code>#item="(name, i)"</code> のように分解して受け取れます.
    </li>
  </ul>

  <h2 id="attrs">属性の引き継ぎ</h2>

  <p>
    子コンポーネントのタグに書いた <code>class</code>・<code>style</code>・<code>id</code>・<code>role</code>・<code>data-*</code>・<code>aria-*</code> は、<br>
    propsにならず、子のルート要素(最初の要素)に付け加えられます.<br>
    (Vueのフォールスルー属性)<br>
    classとstyleは、子のものと合わされます.<br>
    <code>:</code> を付けてReactiveを渡すと、値の変化に追従します.
  </p>

  <div class="code-file">Chip.st</div>
  <pre><code class="language-st">{{ chipCode }}</code></pre>

  <div class="code-file">ChipParent.st</div>
  <pre><code class="language-st">{{ chipParentCode }}</code></pre>

  <div class="rendered">
    <ChipParent />
  </div>

  <h2 id="lifecycle">ライフサイクルフック</h2>

  <p>
    <code>&lt;script&gt;</code> に <code>def mounted</code> / <code>def unmounted</code> を定義すると、<br>
    コンポーネントのライフサイクルに合わせて処理を実行できます.<br>
    Vue.jsのmounted / unmountedに当たります.<br>
    引数はなし、または <code>Props</code>・Stateを受け取れます(順序は自由).
  </p>

  <pre><code class="language-st">{{ clockCode }}</code></pre>

  <div class="rendered">
    <Clock />
  </div>

  <ul>
    <li>
      <code>mounted</code> : コンポーネントがDOMに挿入された後(現在の処理の直後)に呼ばれます.<br>
      子コンポーネントのものから順に呼ばれます.<br>
      要素の参照(フォーカスなど)や、外部ライブラリの組み込みに使います.
    </li>
    <li>
      <code>unmounted</code> : コンポーネントが片付けられた(DOMから取り除かれた)後に呼ばれます.<br>
      描き直し・v-ifの切り替え・v-forの行の削除などでも呼ばれます.<br>
      タイマーや外部のイベントの解除に使います.
    </li>
    <li>
      描き直しでは、<br>
      古い内容のunmountedの後に、新しい内容のmountedが呼ばれます.
    </li>
  </ul>

  <h2 id="ref">テンプレート参照</h2>

  <p>
    要素に <code>ref="expr"</code> を書くと、その要素を直接参照できます.<br>
    (Vueのテンプレート参照、ReactのuseRef)<br>
    インスタンスごとの参照になるよう、<br>
    Stateに <code>ElementRef()</code> を持たせてください.<br>
    要素が作られたときに設定され、<br>
    コンポーネントが片付けられると空(<code>get</code> がNone)に戻ります.<br>
    DOMに挿入された後の処理(フォーカスなど)は、<br>
    <code>mounted</code> の中で行ってください.
  </p>

  <pre><code class="language-st">{{ focusInputCode }}</code></pre>

  <div class="rendered">
    <FocusInput />
  </div>

  <h2 id="teleport">Teleport</h2>

  <p>
    <code>&lt;Teleport to="body"&gt;</code> の中身は、<br>
    別の要素(toのCSSセレクター)の末尾に描画されます.<br>
    モーダルなど、画面全体に重ねる表示に使います.<br>
    <code>disabled</code> を付けると、元の位置に描画します.<br>
    scoped CSSやイベントは、そのまま使えます.
  </p>

  <pre><code class="language-st">{{ modalCode }}</code></pre>

  <div class="rendered">
    <Modal />
  </div>

  <h2 id="errorBoundary">エラーの境界</h2>

  <p>
    <code>&lt;ErrorBoundary&gt;</code> で囲んだ中身で起きたエラーを受け止め、<br>
    <code>&lt;template #fallback="err"&gt;</code> の表示に切り替えます.<br>
    (ReactのError Boundary、VueのerrorCaptured)<br>
    エラーが起きても画面全体が壊れず、その部分だけを差し替えられます.
  </p>

  <pre><code class="language-st">{{ boundaryCode }}</code></pre>

  <div class="rendered">
    <Boundary />
  </div>

  <ul>
    <li>
      受け止めるのは、中身を描く途中の例外(子コンポーネントを含む)、<br>
      後から起きる子孫の描き直しの中の例外、<br>
      子孫の <code>mounted</code> とイベントハンドラーの中の例外です.
    </li>
    <li>
      <code>err.message</code> でメッセージを参照できます.<br>
      <code>err.retry()</code> で中身を描き直します.
    </li>
    <li><code>@error="..."</code> で、受け止めたエラーを <code>event</code> として受け取れます.</li>
    <li>
      境界の外で起きた例外は、<br>
      <code>ScalactiveConfig.errorHandler</code>(既定ではコンソールに出力)に渡され、<br>
      ほかの描き直しは続けられます.<br>
      エラーの報告サービスへ送る場合などは、<br>
      アプリの起動時に設定します.<br>
      (Vueのapp.config.errorHandler)
    </li>
  </ul>

  <pre><code class="language-scala">{{ errorHandlerCode }}</code></pre>

  <h2 id="await">非同期データ</h2>

  <p>
    <code>&lt;Await&gt;</code> は、非同期の処理(Future)の状態に応じて表示を切り替えます.<br>
    (VueのSuspense)<br>
    完了するまで <code>#pending</code>、<br>
    完了したら結果を受け取った <code>#default</code>、<br>
    失敗したら <code>#error</code> の表示になります.
  </p>

  <pre><code class="language-st">{{ loaderCode }}</code></pre>

  <div class="rendered">
    <Loader />
  </div>

  <ul>
    <li>
      <code>:load</code> には、Futureを返す関数を渡します.<br>
      描画されたときに呼ばれます.<br>
      <code>err.retry()</code> でもう1度呼びます.
    </li>
    <li>完了する前にコンポーネントが片付けられた場合は、完了しても描き直しません.</li>
  </ul>

  <h2 id="lazy">遅延読み込み</h2>

  <p>
    <code>asyncComponent</code> は、読み込みが終わってから描画するコンポーネントを作ります.<br>
    Scala.jsの <code>js.dynamicImport</code> と組み合わせると、<br>
    コンポーネントのコードを別のファイルに分割し、<br>
    必要になったときに読み込めます.<br>
    (sbtで <code>scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.ESModule))</code> の設定が必要です.)<br>
    <code>&lt;script&gt;</code> の中で <code>apply()</code> を持つobjectにすると、<br>
    タグで呼び出せます.
  </p>

  <pre><code class="language-st">{{ lazyLoadCode }}</code></pre>

  <div class="rendered">
    <LazyLoad />
  </div>

  <p>ルートにもそのまま使えます.</p>

  <pre><code class="language-st">{{ lazyRouteCode }}</code></pre>

  <h2 id="directives">テンプレート構文の一覧</h2>

  <p>
    テンプレートで使える構文の一覧です.<br>
    それぞれの詳しい説明は、
    <a @click='go("/attr")'>属性バインディング</a>・<a @click='go("/conditionalRendering")'>条件付きレンダリング</a>・<a @click='go("/listRendering")'>リストレンダリング</a>・<a @click='go("/event")'>イベントハンドリング</a>・<a @click='go("/builtinComponents")'>組込コンポーネント</a>を参照ください.
  </p>

  <table>
    <tr><th>構文</th><th>説明</th></tr>
    <tr><td><code>&#123;&#123; expr &#125;&#125;</code></td><td>
      テキスト展開.<br>
      Reactiveの場合は値の変化に追従します(HTMLエスケープされます).
    </td></tr>
    <tr><td><code>v-if="expr"</code><br><code>v-else-if="expr"</code><br><code>v-else</code></td><td>
      条件付きレンダリング.<br>
      exprはReactive[Boolean]またはBoolean.<br>
      各条件は1度だけ評価されます.
    </td></tr>
    <tr><td><code>v-for="item in expr"</code><br><code>v-for="(item, i) in expr"</code></td><td>
      リストレンダリング.<br>
      添字の変数を書かない方が、<br>
      行の削除や並べ替えで行を作り直さないので速いです.<br>
      RVListの場合itemはRV、RMListの場合はReactiveModelになります.
    </td></tr>
    <tr><td><code>v-for="..." :key="expr"</code></td><td>
      行を対応付けるキー.<br>
      exprはitemから計算します(例: <code>:key="item.id"</code>).<br>
      リストが変わったとき、キーが同じ行は(値が変わっていなければ)、<br>
      作り直さずに使い回されます.<br>
      指定しない場合は、RVList・RMListでは要素のオブジェクト、<br>
      それ以外では要素の値で対応付けます.<br>
      <code>&lt;template v-for&gt;</code> にも指定できます.
    </td></tr>
    <tr><td><code>&lt;template v-if/v-for&gt;</code></td><td>
      要素自体は出力せず、<br>
      中身の複数要素をまとめて条件分岐・繰り返しします.
    </td></tr>
    <tr><td><code>v-show="expr"</code></td><td>表示切替(display: noneの付け外し).</td></tr>
    <tr><td><code>ref="expr"</code></td><td>
      テンプレート参照.<br>
      exprは <code>ElementRef[HTMLInputElement]</code> などで、<br>
      要素が作られると設定され、片付けられると空に戻ります.<br>
      インスタンスごとの参照になるよう、<br>
      Stateに <code>ElementRef()</code> を持たせてください(<a @click='setHash("ref")'>テンプレート参照</a>).
    </td></tr>
    <tr><td><code>v-html="expr"</code></td><td>
      要素の中身を、式の値のHTMLで置き換えます.<br>
      Reactiveの場合は値が変わるたびに展開し直します.<br>
      要素の中身は空にしてください.<br>
      <b>値はエスケープされません.</b><br>
      ユーザーの入力など信頼できない値を渡すと、<br>
      スクリプトを埋め込まれる危険があります.
    </td></tr>
    <tr><td><code>v-model="rv"</code></td><td>
      input/textarea/selectとRVの双方向バインディング.<br>
      <code>.lazy</code> / <code>.trim</code> 修飾子が使えます(<a @click='go("/attr")'>属性バインディング</a>).
    </td></tr>
    <tr><td><code>:name="expr"</code><br><code>v-bind:name="expr"</code></td><td>
      属性バインディング.<br>
      Reactive[Boolean]は属性の有無、それ以外は値として反映します.
    </td></tr>
    <tr><td><code>:class="expr"</code></td><td>
      クラスバインディング.<br>
      静的なclass属性と併用できます.<br>
      exprには次のものが使えます(Reactiveでも可).<br>
      ・String(空白区切り)、List[String]<br>
      ・オブジェクト記法: クラス名と条件の組 <code>"active" -&gt; s.on</code>、
      <code>Map("active" -&gt; s.on, "big" -&gt; true)</code>(条件はBooleanまたはReactive[Boolean])<br>
      ・配列記法: 文字列と組の混在 <code>List("btn", "active" -&gt; s.on)</code>
    </td></tr>
    <tr><td><code>:style="expr"</code></td><td>
      スタイルバインディング.<br>
      Stringの場合はstyle属性全体になります.<br>
      オブジェクト記法 <code>Map("color" -&gt; s.color, "fontSize" -&gt; "12px")</code>(または組 <code>"display" -&gt; "none"</code>)は、<br>
      プロパティごとに設定され、静的なstyle属性と併用できます.<br>
      プロパティ名はキャメルケースでも書けます.<br>
      値がNone・false・空文字のプロパティは外します.<br>
      値にはReactiveも使えます.
    </td></tr>
    <tr><td><code>@event="stmt"</code><br><code>v-on:event="stmt"</code></td><td>
      イベントハンドリング.<br>
      文の中では <code>event</code> でEventを参照できます.<br>
      修飾子: <code>.prevent</code> / <code>.stop</code> / <code>.self</code>、<br>
      キー <code>.enter</code> / <code>.esc</code> / <code>.tab</code> / <code>.space</code> / <code>.delete</code> / <code>.up</code> / <code>.down</code> / <code>.left</code> / <code>.right</code>、<br>
      修飾キー <code>.ctrl</code> / <code>.shift</code> / <code>.alt</code> / <code>.meta</code><br>
      (例: <code>@keydown.enter.ctrl="send()"</code>).<br>
      リスナーのオプション: <code>.once</code>(条件を満たした最初の1回だけ実行)、<br>
      <code>.capture</code>(子要素より先にキャプチャフェーズで実行)、<br>
      <code>.passive</code>(スクロールなどを妨げない. <code>.prevent</code> とは併用できません).
    </td></tr>
    <tr><td><code>&lt;Other /&gt;</code></td><td>
      大文字で始まるタグは子コンポーネントとして展開されます.<br>
      属性はprops、子要素はslotとして渡されます.<br>
      <code>v-model="rv"</code> は <code>:model="rv"</code> と同じ意味です.
    </td></tr>
    <tr><td><code>&lt;slot&gt;</code><br><code>&lt;slot name="x"&gt;</code></td><td>
      親から渡された子要素(名前付きスロット)を展開します.<br>
      中身は渡されなかった場合の既定の内容です.<br>
      <code>:value="v"</code> でスコープ付きスロットに値を渡します.
    </td></tr>
    <tr><td><code>&lt;RouterLink to="/x"&gt;</code></td><td>
      Router.goで遷移するリンク.<br>
      現在の場所に応じて、<br>
      <code>router-link-active</code> / <code>router-link-exact-active</code> クラスを付けます<br>
      (<code>active-class</code> / <code>exact-active-class</code> で変更できます).
    </td></tr>
    <tr><td><code>&lt;Await :load="() =&gt; f()"&gt;</code></td><td>
      非同期の処理(Future)の状態に応じて表示を切り替えます.<br>
      <code>&lt;template #pending&gt;</code>(読み込み中)、<br>
      <code>&lt;template #default="v"&gt;</code>(結果)、<br>
      <code>&lt;template #error="err"&gt;</code>(失敗. <code>err.retry()</code> で読み込み直し)を書きます.
    </td></tr>
    <tr><td><code>&lt;ErrorBoundary&gt;</code></td><td>
      中身で起きたエラー(描画・描き直し・mounted・イベントハンドラーの中の例外)を受け止め、<br>
      <code>&lt;template #fallback="err"&gt;</code> の表示に切り替えます.<br>
      <code>err.message</code> でメッセージ、<code>err.retry()</code> で中身の描き直しができます.<br>
      <code>@error="..."</code> でエラーを受け取れます.
    </td></tr>
    <tr><td><code>&lt;Teleport to="body"&gt;</code></td><td>
      中身を別の要素(toのCSSセレクター)の末尾に描画します(モーダルなど).<br>
      <code>disabled</code> を付けると元の位置に描画します.<br>
      scoped CSSやイベントはそのまま使えます.
    </td></tr>
    <tr><td><code>&lt;template #x&gt;</code><br><code>&lt;template v-slot:x&gt;</code></td><td>
      子コンポーネントのタグの中で、名前付きスロットxに渡す内容を囲みます.<br>
      <code>#x="v"</code> とすると、スコープ付きスロットとして子から値を受け取ります.
    </td></tr>
  </table>

  <p>
    属性値の中で <code>"</code> を使う場合は、<br>
    属性を <code>'</code> で囲むか <code>&amp;quot;</code> と記述してください.<br>
    (例: <code>:class='Map("active" -&gt; s.on)'</code>)
  </p>

  <h2 id="limitations">制限事項</h2>

  <ul>
    <li>
      <code>&#123;&#123; &#125;&#125;</code> は属性値の中では使えません.<br>
      <code>:name="expr"</code> を使ってください.
    </li>
    <li>
      sbtのコンパイルエラーは生成ソースの位置で報告されます.<br>
      埋め込まれた式には、元ファイルの行番号コメントが付与されます.<br>
      (<a @click='go("/editor")'>エディタ</a>では、言語サーバーが.st上の位置で表示します.)
    </li>
  </ul>

  <p>
    Scalaで直接書く方法は、<a @click='go("/scalaApi")'>Scala API(低水準API)</a> を参照してください.
  </p>
</template>
