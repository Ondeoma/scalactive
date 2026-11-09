<script lang="scala">
import Router.*

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
     |import io.github.ondeoma.scalactive.reactive.RM.*
     |
     |//  *** 型定義 *** //
     |
     |// 元クラス
     |case class Human(name: String,
     |                 age: Int)
     |
     |// ReactiveModel
     |// 実装の省力化のためマクロ関数を用意しています.
     |// マクロを正しく動作させるために元クラスと同様のフィールド構成とし、
     |// 後述のRMCompatible型クラスインスタンスを配置してください。
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
     |  // マクロ関数に必要なRMCompatible型クラスインスタンス 
     |  given RMCompatible[Human, HumanRM] = genToReactiveModel[Human, HumanRM](_)
     |}
     |
     |//  *** 使い方 *** //
     |
     |// 元クラス
     |val h: Human = Human("Taro", 10)
     |
     |// 元クラスからReactiveModelを生成
     |val hrm: HumanRM = RMCompatible.toRM(h)
     |
     |// 値の更新
     |hrm := Human("Hanako", 20) // 全体
     |hrm.name := "Bob" // 個別
     |
     |// 値の取得
     |val newH: Human = hrm.v // 全体
     |val newName: String = hrm.name.v // 個別
   """.stripMargin

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
     |// 複数のRVからCRVを作成
     |// ※v0.1.11時点で最大6つのRVから作成可能です
     |val rv2 = RV("A")
     |val crv2: CRV[String] = CRV(rv1, rv2, (v1, v2) => s"$$v1:$$v2")
     |
     |// Reactiveな値からmapCを使ってCRVを作成することが可能です
     |val crv3: CRV[Int] = rv1.mapC(_ * 10)
     |""".stripMargin

val codeWatch = // language=scala
  s"""import io.github.ondeoma.scalactive.reactive.*
     |
     |val rv1 = RV(10)
     |
     |// 監視処理の追加
     |val watchInfo = rv1.addWatcher {
     |  case (oldV, newV) => println(s"rv1: $$oldV -> $$newV")
     |}
     |
     |// 監視の終了
     |watchInfo.abort()
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
  s"""import scala.scalajs.js
     |
     |rv := false // ifCなどの描き直しが予約される
     |// ここではまだ描き直されていない
     |
     |// 現在の処理の後(マイクロタスク)に実行すれば、描き直し後のDOMを参照できます
     |js.Dynamic.global.queueMicrotask(() => {
     |  // 描き直し後のDOMを参照する処理
     |})
     |""".stripMargin
</script>

<template>
  <h1>リアクティブ</h1>

  <p>
    Input要素に入力した値がリアルタイムにScala変数値に代入される、<br>
    またはScalaの変数値に代入をすれば、それが画面のInput要素に反映される、<br>
    といった双方向バインディングに関する説明です.<br>
    以下の型は全てReactive型を継承しています.
  </p>

  <ul class="toc">
    <li><a @click='setHash("RV")'>RV</a></li>
    <li><a @click='setHash("RVList")'>RVList</a></li>
    <li><a @click='setHash("ReactiveList")'>ReactiveList</a></li>
    <li><a @click='setHash("ReactiveModel")'>ReactiveModel</a></li>
    <li><a @click='setHash("RMList")'>RMList</a></li>
    <li><a @click='setHash("CRV")'>CRV</a></li>
    <li><a @click='setHash("Watch")'>リアクティブ値の監視</a></li>
    <li><a @click='setHash("Timing")'>画面に反映されるタイミング</a></li>
  </ul>

  <h2 id="RV">RV[A] extends Reactive[A]</h2>

  <p>
    RV[A]は最もシンプルなリアクティブな型です。<br>
    前の値と等しい値を代入した場合は、監視している側に通知されません.<br>
    ※v0.1.xまでは通知されていました.
  </p>

  <pre><code class="language-scala">{{ codeRV }}</code></pre>

  <h2 id="RVList">RVList[A] extends Reactive[List[A]]</h2>

  <p>
    RVList[A]はRV[List[A]]と似ていますが、<br>
    内部構造としてはRV[A]の可変コレクションです.要素の追加・更新・削除が可能です.<br>
    <a @click='go("/listRendering")'>リストレンダリング</a>に利用できます.
  </p>

  <pre><code class="language-scala">{{ codeListRV }}</code></pre>

  <h2 id="ReactiveList">ReactiveList[A] extends Reactive[List[A]]</h2>

  <p>
    ReactiveList[A]はRVList[A]とほぼ同じです.<br>
    内部構造としてはListBuffer[A]に近しいです. 要素の追加・更新・削除が可能です.<br>
    RVListよりも少し単純な仕組みのため、状況次第でパフォーマンス面で有利なのかもしれません(未検証)。<br>
    <a @click='go("/listRendering")'>リストレンダリング</a>に利用できます.
  </p>

  <pre><code class="language-scala">{{ codeReactiveList }}</code></pre>

  <h2 id="ReactiveModel">ReactiveModel[Org, Self &lt;: ReactiveModel[Org, Self]] extends Reactive[Org]</h2>

  <p>
    ReactiveModel[Org, Self]は、RV[Org]と似た型になりますが、<br>
    Org型はcase classを前提としており、その全フィールドをReactiveにしたような型です.<br>
    ReactiveModel自体はtraitなので必要に応じて元クラス毎に実装する必要があります.<br>
    case classとの相互変換が可能なリアクティブ型がほしい時に利用ください.
  </p>

  <pre><code class="language-scala">{{ codeReactiveModel }}</code></pre>

  <h2 id="RMList">RMList[Org, Self &lt;: ReactiveModel[Org, Self]] extends Reactive[List[Org]]</h2>

  <p>
    RVList[Org]のReactiveModel版です.<br>
    ReactiveModelの可変コレクションであり、要素の追加・更新・削除が可能です.<br>
    RVListと比較して要素のReactiveModelのフィールド値が更新された場合でも、<br>
    リアクティブな挙動が行えるようになっています.<br>
    <a @click='go("/listRendering")'>リストレンダリング</a>に利用できます.
  </p>

  <pre><code class="language-scala">{{ codeRMList }}</code></pre>

  <h2 id="CRV">CRV[A] extends Reactive[A]</h2>

  <p>
    他のリアクティブな値が変化した時に自動追従するリアクティブ型です.<br>
    Vue.jsにおけるComputedの概念に近いものです. 読取専用です.<br>
    計算し直した値が前の値と等しい場合は通知されません.<br>
    例えば <code>count.mapC(_ &gt; 3)</code> を条件にしたifCは、<br>
    countが変わるたびではなく、真偽が切り替わった時だけ描き直されます.
  </p>

  <pre><code class="language-scala">{{ codeCRV }}</code></pre>

  <h2 id="Watch">リアクティブ値の監視</h2>

  <p>
    リアクティブな値が変化した際に任意の処理を実行させることができます.<br>
    Vue.jsにおけるwatchの概念に近いものです.<br>
    コンポーネントの外のリアクティブな値(グローバルな状態やRouter.hrefなど)を、<br>
    genHtmlの中で監視する場合は、<br>
    <code>onUnmounted(watchInfo.abort())</code> で片付けるときに解除してください.<br>
    解除しないと、描画のたびに監視が増えていきます.
  </p>

  <pre><code class="language-scala">{{ codeWatch }}</code></pre>

  <h2 id="Timing">画面に反映されるタイミング</h2>

  <p>
    テキスト・属性・classなどのバインディングは、<br>
    リアクティブな値を変更するとその場で書き換わります.<br>
    一方、ifC・forCなど要素を作り直すコンポーネントは、描き直しを予約し、<br>
    現在の処理(イベントハンドラーなど)が終わった直後にまとめて描き直します.<br>
    そのため、1つの処理の中で何度値を変更しても、描き直しは1回で済みます.<br>
    親と子の両方が描き直しを予約した場合は、親だけが描き直されます.
  </p>

  <pre><code class="language-scala">{{ codeAfterReload }}</code></pre>

  <p>
    描き直しを待つ時間は <code>ScalactiveConfig.reloadTimerLazyDuration</code> で変更できます.<br>
    0より大きい値を指定すると、<br>
    最後の変更からその時間だけ待ってから描き直します.<br>
    例えば、文字を入力するたびに大きなリストを絞り込んで描き直すと重くなる場合に、<br>
    入力が落ち着くまで描き直しを遅らせることができます.<br>
    ※v0.1.xまでは既定値が100msで、<br>
    変更が画面に反映されるまで常に100ms以上かかっていました.
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
