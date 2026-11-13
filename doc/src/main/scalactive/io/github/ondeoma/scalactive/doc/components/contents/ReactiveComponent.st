<script lang="scala">
import Router.*
import io.github.ondeoma.scalactive.doc.components.contents.reactive.{CrvDemo, NextTickDemo, RmListDemo, RvCounter, RvListDemo, WatchDemo}

val codeRvSt = // language=html
  """<script lang="scala">
     |case class State(name: RV[String] = RV("Taro"),
     |                 count: RV[Int] = RV(0))
     |
     |def initState() = State()
     |</script>
     |
     |<template>
     |  <p><input type="text" v-model="s.name"></p>
     |  <p>{{ s.name }}さんのカウント: {{ s.count }}</p>
     |  <button @click="s.count := s.count.v + 1">+1</button>
     |  <button @click="s.count := 0">0に戻す</button>
     |</template>
     |""".stripMargin

val codeRvListSt = // language=html
  """<script lang="scala">
     |case class State(nums: RVList[Int] = RVList(List(1, 2)))
     |
     |def initState() = State()
     |</script>
     |
     |<template>
     |  <button @click="s.nums.add(s.nums.v.size + 1)">add</button>
     |  <button @click="s.nums.liftV(0).foreach(n => s.nums.update(0, n * 10))">update(0)</button>
     |  <button @click="if (s.nums.v.nonEmpty) s.nums.rm(0)">rm(0)</button>
     |  <button @click="s.nums := List(1, 2)">:=</button>
     |  <p>合計: {{ s.nums.mapC(_.sum) }}</p>
     |  <ul>
     |    <li v-for="n in s.nums">{{ n }}</li>
     |  </ul>
     |</template>
     |""".stripMargin

val codeRmListSt = // language=html
  """<script lang="scala">
     |case class Human(name: String,
     |                 age: Int)
     |
     |case class HumanRM(name: RV[String],
     |                   age: RV[Int]) extends ReactiveModelOf[Human, HumanRM]
     |
     |object HumanRM extends ReactiveModelOf.Companion[Human, HumanRM]
     |
     |case class State(humans: RMList[Human, HumanRM] = RMList(List(Human("Taro", 10), Human("Hanako", 20))))
     |
     |def initState() = State()
     |</script>
     |
     |<template>
     |  <ul>
     |    <!-- RMListの場合、hはHumanRMになります -->
     |    <li v-for="h in s.humans">
     |      <input type="text" v-model="h.name"> {{ h.age }}歳
     |      <button @click="h.age := h.age.v + 1">+1歳</button>
     |    </li>
     |  </ul>
     |  <button @click='s.humans.add(Human("Bob", 30))'>Bobを追加</button>
     |  <p>s.humans.v = {{ s.humans }}</p>
     |</template>
     |""".stripMargin

val codeCrvSt = // language=html
  """<script lang="scala">
     |case class State(price: RV[Int] = RV(100),
     |                 qty: RV[Int] = RV(2))
     |
     |def initState() = State()
     |</script>
     |
     |<template>
     |  <p>
     |    単価 <input type="number" v-model="s.price" style="width: 5em">円
     |    × <input type="number" v-model="s.qty" style="width: 4em">個
     |  </p>
     |  <p>合計: {{ CRV(s.price, s.qty, (p, q) => p * q) }}円</p>
     |  <p>税込: {{ s.price.v * s.qty.v * 110 / 100 }}円</p>
     |  <p v-if="s.qty.v >= 10">まとめ買いです!</p>
     |</template>
     |""".stripMargin

val codeTrackText = "{{ s.price.v * s.qty.v }}"
val codeTrackComputed = "{{ computed(s.price.v * s.qty.v) }}"
val codeMustache = "{{ }}"

val codeTrackingSt = // language=html
  """<template>
     |  <!-- 読んだ値(price・qty)が変わると表示し直す -->
     |  <p>{{ s.price.v * s.qty.v }}円</p>
     |  <button :disabled="s.items.v.isEmpty">注文</button>
     |  <p :class='if (s.qty.v >= 10) "bulk" else "normal"'>...</p>
     |  <p v-if="s.user.v.isDefined">ようこそ</p>
     |  <li v-for="item in s.items.v.filter(_.visible)">{{ item.name }}</li>
     |  <!-- 子のprops: 型がReactiveならその場で、それ以外なら子を作り直して追従する -->
     |  <Price :amount="s.price.v * s.qty.v" />
     |  <!-- untrackedの中で読んだ値は監視しない -->
     |  <p>{{ untracked(s.log.v).size }}件</p>
     |</template>
     |""".stripMargin

val codeWatchSt = // language=html
  """<script lang="scala">
     |// コンポーネントの外のリアクティブな値(全てのインスタンスで共有されます)
     |val shared = RV(0)
     |
     |case class State(count: RV[Int] = RV(0),
     |                 log: RV[String] = RV("-"),
     |                 sharedLog: RV[String] = RV("-"))
     |
     |def initState() = State()
     |
     |// mountedの中のwatchは、コンポーネントが片付けられると自動で解除されます
     |def mounted(s: State): Unit = {
     |  watch(s.count)((oldV, newV) => s.log := s"count: $oldV -> $newV")
     |  watch(shared, immediate = true)((_, newV) => s.sharedLog := s"shared: $newV")
     |}
     |</script>
     |
     |<template>
     |  <button @click="s.count := s.count.v + 1">count +1</button>
     |  <button @click="shared := shared.v + 1">shared +1</button>
     |  <p>{{ s.log }}</p>
     |  <p>{{ s.sharedLog }}</p>
     |</template>
     |""".stripMargin

val codeNextTickSt = // language=html
  """<script lang="scala">
     |import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
     |
     |case class State(items: RVList[String] = RVList(List("りんご")),
     |                 list: ElementRef[HTMLElement] = ElementRef())
     |
     |def initState() = State()
     |
     |def add(s: State): Unit = {
     |  s.items.add("")
     |  // 描き直しが終わってから、追加した行の入力欄にフォーカスします
     |  nextTick().foreach { _ =>
     |    s.list.foreach { ul =>
     |      val inputs = ul.querySelectorAll("input")
     |      inputs(inputs.length - 1).asInstanceOf[HTMLInputElement].focus()
     |    }
     |  }
     |}
     |</script>
     |
     |<template>
     |  <div>
     |    <ul ref="s.list">
     |      <li v-for="item in s.items"><input type="text" :value="item"></li>
     |    </ul>
     |    <button @click="add(s)">行を追加</button>
     |  </div>
     |</template>
     |""".stripMargin

val codeRV = // language=scala
  s"""import io.github.ondeoma.scalactive.reactive.*
     |// 初期化
     |val rv: RV[Int] = RV(10)
     |
     |// 値の更新
     |rv := 20 
     |// rv.v = 20
     |
     |// 値の取得
     |val rvV: Int = rv.v
     |//             rv()
     |
     |// 前の値と等しい(==)値を代入しても、監視している側(画面など)には通知されません
     |rv := 20
     |
     |// 可変なオブジェクトの中身を直接書き換えた場合などは、triggerで明示的に通知できます
     |val buf = RV(scala.collection.mutable.ListBuffer(1, 2))
     |buf.v += 3
     |buf.trigger()
     |""".stripMargin

val codeListRV = // language=scala
  s"""import io.github.ondeoma.scalactive.reactive.*
     |// 初期化
     |val lrv: RVList[Int] = RVList(List(1, 2))
     |//                     RVList(List(RV(1), RV(2)))
     |
     |// 値の追加
     |lrv.add(3)
     |// lrv.add(RV(3))
     |
     |// 値の更新
     |lrv.update(0, 10)
     |// lrv(0) = 10
     |
     |// 値の削除
     |lrv.rm(2)
     |
     |// 値の総入替
     |lrv := List(1, 2) 
     |//     List(RV(1), RV(2))   
     |    
     |// 値の取得
     |val lrvV: List[Int] = lrv.v 
     |//                    lrv()
     |""".stripMargin

val codeReactiveList = // language=scala
  s"""import io.github.ondeoma.scalactive.reactive.*
     |// 初期化
     |val lrv: ReactiveList[Int] = ReactiveList(List(1, 2))
     |
     |// 値の追加
     |lrv.add(3)
     |
     |// 値の更新
     |lrv.update(0, 10)
     |// lrv(0) = 10
     |
     |// 値の削除
     |lrv.rm(2)
     |
     |// 値の総入替
     |lrv := List(1, 2) 
     |    
     |// 値の取得
     |val lrvV: List[Int] = lrv.v 
     |//                    lrv()
     |""".stripMargin

val codeReactiveModel = // language=scala
  s"""import io.github.ondeoma.scalactive.reactive.*
     |
     |//  *** 型定義 *** //
     |
     |// 元クラス
     |case class Human(name: String,
     |                 age: Int)
     |
     |// ReactiveModel
     |// 元クラスと同じ名前のフィールドを、RV・RVList・ReactiveModel・RMListのどれかで持たせます.
     |// 元クラスとの変換はマクロで自動的に作られます.
     |case class HumanRM(name: RV[String],
     |                   age: RV[Int]) extends ReactiveModelOf[Human, HumanRM]
     |
     |// コンパニオンに継承させると、RMCompatible.toRMなどで変換できるようになります.
     |object HumanRM extends ReactiveModelOf.Companion[Human, HumanRM]
     |
     |//  *** 使い方 *** //
     |
     |// 元クラス
     |val h: Human = Human("Taro", 10)
     |
     |// 元クラスからReactiveModelを生成
     |val hrm: HumanRM = RMCompatible.toRM(h)
     |//                 HumanRM.from(h)
     |
     |// 値の更新
     |hrm := Human("Hanako", 20) // 全体
     |hrm.name := "Bob" // 個別
     |
     |// 値の取得
     |val newH: Human = hrm.v // 全体
     |val newName: String = hrm.name.v // 個別
   """.stripMargin

val codeReactiveModelManual = // language=scala
  s"""import io.github.ondeoma.scalactive.reactive.*
     |import io.github.ondeoma.scalactive.reactive.ReactiveModelMacro.*
     |
     |case class HumanRM(name: RV[String],
     |                   age: RV[Int]) extends ReactiveModel[Human, HumanRM] {
     |
     |  val reactives = collectReactives(this)
     |  val reactiveModels = collectReactiveModels(this)
     |  def reload(s: O) = genReload(this, s)
     |  def toOrigin = genToOrigin(this)
     |
     |}
     |
     |object HumanRM {
     |  given RMCompatible[Human, HumanRM] = genToReactiveModel[Human, HumanRM](_)
     |}
     |""".stripMargin

val codeRMList = // language=scala
  s"""import io.github.ondeoma.scalactive.reactive.*
     |
     |// ReactiveModelのサンプルコードのHumanRMを再利用しています。
     |
     |// 元クラスリスト
     |val hs: List[Human] = List(Human("Taro", 10), Human("Hanako", 20))
     |
     |// 初期化
     |val rms: RMList[Human, HumanRM] = RMList(hs)
     |//                                RMList(hs.map(RMCompatible.toRM[Human, HumanRM]))
     |
     |// 値の追加
     |rms.add(Human("Bob", 20))
     |// rms.add(RMCompatible.toRM[Human, HumanRM](Human("Bob", 20)))
     |
     |// 値の更新
     |rms.update(0, Human("Bob", 20))
     |rms(0) = Human("Bob", 20)
     |// rms.update(0, RMCompatible.toRM[Human, HumanRM](Human("Bob", 20)))
     |// rms(0) = RMCompatible.toRM[Human, HumanRM](Human("Bob", 20))
     |    
     |// 値の削除
     |rms.rm(2)
     |
     |// 値の総入替
     |rms := hs
     |//     hs.map(RMCompatible.toRM[Human, HumanRM])
     |
     |// 値の取得
     |val rmsV: List[Human] = rms.v
     |//                      rms()
     |""".stripMargin

val codeCRV = // language=scala
  s"""import io.github.ondeoma.scalactive.reactive.*
     |
     |// RVからCRVを作成
     |val rv1 = RV(10)
     |val crv1: CRV[Int] = CRV(rv1, _ + 10)
     |
     |// 複数のRVからCRVを作成(最大6つまで. それより多い場合はcomputedを使います)
     |val rv2 = RV("A")
     |val crv2: CRV[String] = CRV(rv1, rv2, (v1, v2) => s"$$v1:$$v2")
     |
     |// Reactiveな値からmapCを使ってCRVを作成することが可能です
     |val crv3: CRV[Int] = rv1.mapC(_ * 10)
     |""".stripMargin

val codeWatch = // language=scala
  s"""import io.github.ondeoma.scalactive.components.ComponentManager.*
     |
     |val rv1 = RV(10)
     |val rv2 = RV("A")
     |
     |// 値が変わったときに実行します(前の値と等しい値の代入では実行しません)
     |val w = watch(rv1)((oldV, newV) => println(s"rv1: $$oldV -> $$newV"))
     |
     |// immediate = true で、登録したときにも1回実行します
     |watch(rv1, immediate = true)((_, newV) => println(s"rv1: $$newV"))
     |
     |// 複数の値を見る場合は、computedと組み合わせます
     |watch(computed((rv1.v, rv2.v))) { case (_, (v1, v2)) => println(s"$$v1, $$v2") }
     |
     |// 監視の終了(コンポーネントの中・mountedの中で登録したものは、自動で解除されます)
     |w.stop()
     |""".stripMargin

val codeComputed = // language=scala
  s"""import io.github.ondeoma.scalactive.components.ComponentManager.*
     |
     |val price = RV(100)
     |val qty = RV(2)
     |val taxIncluded = RV(true)
     |
     |// 計算の中で読んだ値(.v)を、自動で監視します
     |val total: CRV[Int] = computed {
     |  val sum = price.v * qty.v
     |  if (taxIncluded.v) sum * 110 / 100 else sum
     |}
     |
     |// untrackedの中で読んだ値は監視しません(qtyが変わっても計算し直さない)
     |val label: CRV[String] = computed(s"$${price.v}円 × $${untracked(qty.v)}個")
     |""".stripMargin

val codeTiming = // language=scala
  s"""import io.github.ondeoma.scalactive.ScalactiveConfig
     |import scala.concurrent.duration.*
     |
     |// 既定(0)では、イベントハンドラーなどの処理が終わった直後に描き直します.
     |// 最後の変更から一定時間待ってから描き直したい場合は、アプリの起動時に指定します.
     |ScalactiveConfig.reloadTimerLazyDuration = 200.millis
     |""".stripMargin

val codeAfterReload = // language=scala
  s"""import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
     |
     |rv := false // v-ifなどの描き直しが予約される
     |// ここではまだ描き直されていない
     |
     |// nextTickは、描き直しとmountedが終わったときに完了します
     |nextTick().foreach { _ =>
     |  // 描き直し後のDOMを参照する処理
     |}
     |""".stripMargin
</script>

<template>
  <h1>リアクティブ</h1>

  <p>
    Input要素に入力した値がリアルタイムにScala変数値に代入される、<br>
    またはScalaの変数値に代入をすれば、それが画面に反映される、<br>
    といった双方向バインディングに関する説明です.<br>
    以下の型は全てReactive型を継承しています.<br>
    .stのテンプレートでは、<code>&#123;&#123; &#125;&#125;</code>・<code>:属性名</code>・v-if・v-forなどにReactiveな値を渡すと、<br>
    値の変化に追従して画面が更新されます.
  </p>

  <ul class="toc">
    <li><a @click='setHash("RV")'>RV</a></li>
    <li><a @click='setHash("RVList")'>RVList</a></li>
    <li><a @click='setHash("ReactiveList")'>ReactiveList</a></li>
    <li><a @click='setHash("ReactiveModel")'>ReactiveModel</a></li>
    <li><a @click='setHash("RMList")'>RMList</a></li>
    <li><a @click='setHash("CRV")'>CRV</a></li>
    <li><a @click='setHash("computed")'>computed</a></li>
    <li><a @click='setHash("Tracking")'>テンプレートの式の追跡</a></li>
    <li><a @click='setHash("Watch")'>リアクティブ値の監視</a></li>
    <li><a @click='setHash("Timing")'>画面に反映されるタイミング</a></li>
  </ul>

  <h2 id="RV">RV[A] extends Reactive[A]</h2>

  <p>
    RV[A]は最もシンプルなリアクティブな型です.
  </p>

  <pre><code class="language-scala">{{ codeRV }}</code></pre>

  <p>
    .stでは、Stateのフィールドに持たせて使うのが基本です.<br>
    テンプレートでは <code>&#123;&#123; s.count &#125;&#125;</code> で表示し、<br>
    <code>v-model</code> で入力欄と同期できます.<br>
    イベントハンドラーで <code>:=</code> で代入すると、画面に反映されます.
  </p>

  <pre><code class="language-st">{{ codeRvSt }}</code></pre>

  <div class="rendered">
    <RvCounter />
  </div>

  <h2 id="RVList">RVList[A] extends Reactive[List[A]]</h2>

  <p>
    RVList[A]はRV[List[A]]と似ていますが、<br>
    内部構造としてはRV[A]の可変コレクションです.<br>
    要素の追加・更新・削除が可能です.<br>
    <a @click='go("/listRendering")'>リストレンダリング</a>に利用できます.
  </p>

  <pre><code class="language-scala">{{ codeListRV }}</code></pre>

  <p>
    v-forでは、各行のitemがRV[A]になります.<br>
    行の値が更新されると、その行の表示だけが書き換わります.
  </p>

  <pre><code class="language-st">{{ codeRvListSt }}</code></pre>

  <div class="rendered">
    <RvListDemo />
  </div>

  <h2 id="ReactiveList">ReactiveList[A] extends Reactive[List[A]]</h2>

  <p>
    ReactiveList[A]はRVList[A]とほぼ同じです.<br>
    内部構造としてはListBuffer[A]に近しいです.<br>
    要素の追加・更新・削除が可能です.<br>
    RVListと違い、要素はRVではなく値のまま持ちます.<br>
    <a @click='go("/listRendering")'>リストレンダリング</a>に利用できます.
  </p>

  <p>
    1,000行の表でRVListと比べると、次のようになりました(リポジトリの <code>bench</code> で計測).
  </p>

  <ul>
    <li>
      全行の置き換え・2行の入れ替え・削除・全行の削除は、RVListより15〜20%ほど速いです.<br>
      行ごとのRVと、その値を監視する仕組みがないためです.
    </li>
    <li>
      10行に1行の値の書き換えは、RVListより1.5倍ほど遅いです.<br>
      値が変わった行を作り直すためです.<br>
      RVListは、行の中の表示だけを書き換えます.
    </li>
    <li>
      リスト自体の操作(代入・update)もReactiveListの方が速いですが、<br>
      1,000件で1ms未満の差なので、描画の時間に比べればわずかです.
    </li>
  </ul>

  <p>
    どちらも差は大きくありません.<br>
    行の中の値をよく書き換えるならRVList、行をまとめて入れ替えることが多いならReactiveList、を目安にしてください.
  </p>

  <pre><code class="language-scala">{{ codeReactiveList }}</code></pre>

  <h2 id="ReactiveModel">ReactiveModel[Org, Self &lt;: ReactiveModel[Org, Self]] extends Reactive[Org]</h2>

  <p>
    ReactiveModel[Org, Self]は、RV[Org]と似た型になりますが、<br>
    Org型はcase classを前提としており、その全フィールドをReactiveにしたような型です.<br>
    元クラス毎に、ReactiveModelOfを継承したcase classと、<br>
    ReactiveModelOf.Companionを継承したコンパニオンを定義します.<br>
    case classとの相互変換が可能なリアクティブ型がほしい時に利用ください.
  </p>

  <pre><code class="language-scala">{{ codeReactiveModel }}</code></pre>

  <p>
    変換の処理を自分で書きたい場合は、ReactiveModelを直接継承します.<br>
    ReactiveModelMacroのマクロ関数を使うと、上と同じ動きになります.
  </p>

  <pre><code class="language-scala">{{ codeReactiveModelManual }}</code></pre>

  <h2 id="RMList">RMList[Org, Self &lt;: ReactiveModel[Org, Self]] extends Reactive[List[Org]]</h2>

  <p>
    RVList[Org]のReactiveModel版です.<br>
    ReactiveModelの可変コレクションであり、要素の追加・更新・削除が可能です.<br>
    RVListと比較して要素のReactiveModelのフィールド値が更新された場合でも、<br>
    リアクティブな挙動が行えるようになっています.<br>
    <a @click='go("/listRendering")'>リストレンダリング</a>に利用できます.
  </p>

  <pre><code class="language-scala">{{ codeRMList }}</code></pre>

  <p>
    v-forでは、各行のitemがReactiveModelになります.<br>
    フィールドのRVを、そのまま <code>v-model</code> や <code>&#123;&#123; &#125;&#125;</code> に使えます.<br>
    ReactiveModelの型定義は、<code>&lt;script&gt;</code> に書くこともできます.
  </p>

  <pre><code class="language-st">{{ codeRmListSt }}</code></pre>

  <div class="rendered">
    <RmListDemo />
  </div>

  <h2 id="CRV">CRV[A] extends Reactive[A]</h2>

  <p>
    他のリアクティブな値が変化した時に自動追従するリアクティブ型です.<br>
    Vue.jsにおけるComputedの概念に近いものです. 読取専用です.<br>
    計算し直した値が前の値と等しい場合は通知されません.<br>
    例えば <code>v-if="count.mapC(_ &gt; 3)"</code> は、<br>
    countが変わるたびではなく、真偽が切り替わった時だけ描き直されます.
  </p>

  <pre><code class="language-scala">{{ codeCRV }}</code></pre>

  <p>
    テンプレートの中で作ったCRVは、<br>
    コンポーネントが片付けられる時に、監視も一緒に解除されます.
  </p>

  <pre><code class="language-st">{{ codeCrvSt }}</code></pre>

  <div class="rendered">
    <CrvDemo />
  </div>

  <h2 id="computed">computed</h2>

  <p>
    <code>computed &#123; ... &#125;</code> は、計算の中で読んだリアクティブな値(<code>.v</code>)を、自動で監視するCRVを作ります.<br>
    Vue.jsのcomputedに当たります.<br>
    CRV(...)のように元の値を並べる必要がなく、数の上限もありません.<br>
    計算し直すたびに読んだ値を集め直すので、<br>
    条件によって読む値が変わる場合も、その時点で読んだ値だけを監視します.
  </p>

  <pre><code class="language-scala">{{ codeComputed }}</code></pre>

  <ul>
    <li>計算し直した値が前の値と等しい場合は、CRVと同じく通知されません.</li>
    <li>テンプレートの中・コンポーネントの中で作ったものは、コンポーネントが片付けられる時に監視も解除されます.</li>
    <li>計算の中では、リアクティブな値を書き換えないでください.</li>
    <li>RVList・RMList・ReactiveModelは、それ自体が監視されます(行・項目の変化でも計算し直します).</li>
  </ul>

  <h2 id="Tracking">テンプレートの式の追跡</h2>

  <p>
    テンプレートの式は、Vue.jsと同じく、中で読んだリアクティブな値(<code>.v</code>)を自動で監視します.<br>
    <code>{{ codeTrackText }}</code> は、<code>{{ codeTrackComputed }}</code> と書いたのと同じように、値が変わると表示し直します.
  </p>

  <pre><code class="language-st">{{ codeTrackingSt }}</code></pre>

  <ul>
    <li>対象は <code>{{ codeMustache }}</code>・<code>:属性</code>・子コンポーネントのprops・<code>:class</code>・<code>:style</code>・<code>v-if</code>・<code>v-else-if</code>・<code>v-show</code>・<code>v-html</code>・<code>v-for</code> のリストの式です.</li>
    <li>式の値がReactive(<code>s.count</code> など)の場合は、これまでどおりその値に追従します.</li>
    <li>リアクティブな値を何も読んでいない式は、これまでどおり1度だけ評価され、監視のためのものは作られません.</li>
    <li>監視したくない部分は <code>untracked(...)</code> で囲みます.</li>
    <li>子コンポーネントへ渡すprops(<code>:name="..."</code>)も対象です. propsの型がReactiveなら追従するcomputedを渡し、それ以外の型なら値が変わったときに子を作り直します(詳しくはコンポーネントのpropsを見てください).</li>
  </ul>

  <h2 id="Watch">リアクティブ値の監視</h2>

  <p>
    <code>watch</code> で、リアクティブな値が変化した際に任意の処理を実行させることができます.<br>
    Vue.jsのwatchに当たります.
  </p>

  <pre><code class="language-scala">{{ codeWatch }}</code></pre>

  <p>
    .stでは、<code>def mounted</code> の中で <code>watch</code> を登録してください.<br>
    コンポーネントが片付けられる時(描き直しを含みます)に自動で解除されるので、<br>
    コンポーネントの外のリアクティブな値(グローバルな状態やRouter.hrefなど)も、解除を書かずに監視できます.<br>
    それ以外の場所(<code>initState</code> やオブジェクトの直下など)で登録した場合は自動では解除されないので、<br>
    必要に応じて <code>stop()</code> を呼んでください.<br>
    より低いレベルの <code>addWatcher</code> / <code>WatchInfo.abort()</code> も使えます.
  </p>

  <pre><code class="language-st">{{ codeWatchSt }}</code></pre>

  <div class="rendered">
    <WatchDemo />
  </div>

  <h2 id="Timing">画面に反映されるタイミング</h2>

  <p>
    テキスト・属性・classなどのバインディングは、<br>
    リアクティブな値を変更するとその場で書き換わります.<br>
    一方、v-if・v-forなど要素を作り直すものは、描き直しを予約し、<br>
    現在の処理(イベントハンドラーなど)が終わった直後にまとめて描き直します.<br>
    そのため、1つの処理の中で何度値を変更しても、描き直しは1回で済みます.<br>
    親と子の両方が描き直しを予約した場合は、親だけが描き直されます.
  </p>

  <pre><code class="language-scala">{{ codeAfterReload }}</code></pre>

  <p>
    <code>nextTick()</code> は、予約された描き直しと、その後のmountedがすべて終わった時に完了するFutureを返します.<br>
    Vue.jsのnextTickに当たります.<br>
    描き直した後の要素の大きさを測る・フォーカスを当てる、といった処理に使います.<br>
    下の例では、行を追加した後、新しい行の入力欄にフォーカスします.
  </p>

  <pre><code class="language-st">{{ codeNextTickSt }}</code></pre>

  <div class="rendered">
    <NextTickDemo />
  </div>

  <p>
    描き直しを待つ時間は <code>ScalactiveConfig.reloadTimerLazyDuration</code> で変更できます.<br>
    0より大きい値を指定すると、<br>
    最後の変更からその時間だけ待ってから描き直します.<br>
    例えば、文字を入力するたびに大きなリストを絞り込んで描き直すと重くなる場合に、<br>
    入力が落ち着くまで描き直しを遅らせることができます.
  </p>

  <pre><code class="language-scala">{{ codeTiming }}</code></pre>

  <h3>注意: イベントの伝播中の描き直し</h3>

  <p>
    Vue.jsと同様に、描き直しはマイクロタスクで行われます.<br>
    ブラウザはイベントリスナーを1つ呼び出すたびに、マイクロタスクを実行します.<br>
    そのため、クリックなどのイベントが親要素へ伝播する途中で、<br>
    描き直しが行われることがあります.<br>
    例えば「クリックされた要素がメニューの内側か」を、<br>
    <code>contains(event.target)</code> で判定する処理をdocumentに登録しているとします.<br>
    この場合、クリックされた要素がすでに描き直しで取り除かれていて、<br>
    外側のクリックと判定されることがあります.<br>
    このような判定は、描き直しの影響を受けない要素(作り直されない親要素など)で行うか、<br>
    <code>event.composedPath()</code> で伝播経路を参照してください.
  </p>

</template>
