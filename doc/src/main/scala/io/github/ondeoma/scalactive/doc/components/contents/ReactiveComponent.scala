package io.github.ondeoma.scalactive.doc.components.contents

import io.github.ondeoma.scalactive.components.*
import io.github.ondeoma.scalactive.doc.utils.Utility

object ReactiveComponent extends StatelessComponent {

  def genHtml(implicit cm: ComponentManager): HTML = {

    import Router.*
    import Utility.esc

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

    // language=html
    s"""<h1>リアクティブ</h1>
       |
       |<p>
       |  Input要素に入力した値がリアルタイムにScala変数値に代入される、<br>
       |  またはScalaの変数値に代入をすれば、それが画面のInput要素に反映される、<br>
       |  といった双方向バインディングに関する説明です.<br>
       |  以下の型は全てReactive型を継承しています.
       |</p>
       |
       |<ul class="toc">
       |  <li><a ${evClick(setHash("RV"))}>RV</a></li>
       |  <li><a ${evClick(setHash("RVList"))}>RVList</a></li>
       |  <li><a ${evClick(setHash("ReactiveList"))}>ReactiveList</a></li>
       |  <li><a ${evClick(setHash("ReactiveModel"))}>ReactiveModel</a></li>
       |  <li><a ${evClick(setHash("RMList"))}>RMList</a></li>
       |  <li><a ${evClick(setHash("CRV"))}>CRV</a></li> 
       |  <li><a ${evClick(setHash("Watch"))}>リアクティブ値の監視</a></li>
       |</ul>
       | 
       |<h2 id="RV">RV[A] extends Reactive[A]</h2>
       |
       |<p>
       |  RV[A]は最もシンプルなリアクティブな型です。
       |</p>
       |
       |<pre><code class="language-scala">${esc(codeRV)}</code></pre>
       |
       |<h2 id="RVList">RVList[A] extends Reactive[List[A]]</h2>
       |
       |<p>
       |  RVList[A]はRV[List[A]]と似ていますが、<br>
       |  内部構造としてはRV[A]の可変コレクションです.要素の追加・更新・削除が可能です.<br>
       |  <a ${evClick(go("/listRendering"))}>リストレンダリング</a>に利用できます.
       |</p>
       |
       |<pre><code class="language-scala">${esc(codeListRV)}</code></pre>
       |
       |<h2 id="ReactiveList">ReactiveList[A] extends Reactive[List[A]]</h2>
       |
       |<p>
       |  ReactiveList[A]はRVList[A]とほぼ同じです.<br>
       |  内部構造としてはListBuffer[A]に近しいです. 要素の追加・更新・削除が可能です.<br>
       |  RVListよりも少し単純な仕組みのため、状況次第でパフォーマンス面で有利なのかもしれません(未検証)。<br>
       |  <a ${evClick(go("/listRendering"))}>リストレンダリング</a>に利用できます.
       |</p>
       |
       |<pre><code class="language-scala">${esc(codeReactiveList)}</code></pre>
       |
       |<h2 id="ReactiveModel">ReactiveModel[Org, Self <: ReactiveModel[Org, Self]] extends Reactive[Org]</h2>
       |
       |<p>
       |  ReactiveModel[Org, Self]は、RV[Org]と似た型になりますが、<br>
       |  Org型はcase classを前提としており、その全フィールドをReactiveにしたような型です.<br>
       |  ReactiveModel自体はtraitなので必要に応じて元クラス毎に実装する必要があります.<br>
       |  case classとの相互変換が可能なリアクティブ型がほしい時に利用ください.
       |</p>
       |
       |<pre><code class="language-scala">${esc(codeReactiveModel)}</code></pre>
       |
       |<h2 id="RMList">RMList[Org, Self <: ReactiveModel[Org, Self]] extends Reactive[List[Org]]</h2>
       |
       |<p>
       |  RVList[Org]のReactiveModel版です.<br>
       |  ReactiveModelの可変コレクションであり、要素の追加・更新・削除が可能です.<br>
       |  RVListと比較して要素のReactiveModelのフィールド値が更新された場合でも、<br>
       |  リアクティブな挙動が行えるようになっています.<br>
       |  <a ${evClick(go("/listRendering"))}>リストレンダリング</a>に利用できます.
       |</p>
       |
       |<pre><code class="language-scala">${esc(codeRMList)}</code></pre>
       |
       |<h2 id="CRV">CRV[A] extends Reactive[A]</h2>
       |
       |<p>
       |  他のリアクティブな値が変化した時に自動追従するリアクティブ型です.<br>
       |  Vue.jsにおけるComputedの概念に近いものです. 読取専用です.
       |</p>
       |
       |<pre><code class="language-scala">${esc(codeCRV)}</code></pre>
       |
       |<h2 id="Watch">リアクティブ値の監視</h2>
       |
       |<p>
       |  リアクティブな値が変化した際に任意の処理を実行させることができます.<br>
       |  Vue.jsにおけるwatchの概念に近いものです.
       |</p>
       |
       |<pre><code class="language-scala">${esc(codeWatch)}</code></pre>
       |
       |
       |""".stripMargin
  }

}
