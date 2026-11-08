package io.github.ondeoma.scalactive.doc.components.contents

import io.github.ondeoma.scalactive.components.*
import io.github.ondeoma.scalactive.doc.components.contents.st.StSample
import org.scalajs.dom.*

object SingleFileComponent extends StatelessComponent {

  def genHtml(implicit cm: ComponentManager): HTML = {

    import Router.*
    import io.github.ondeoma.scalactive.doc.utils.Utility.esc

    val codeSetup = // language=scala
      s"""// project/plugins.sbt
         |addSbtPlugin("io.github.ondeoma" % "sbt-scalactive" % "<version>")
         |
         |// build.sbt
         |lazy val app = project
         |  .enablePlugins(ScalaJSPlugin, ScalactivePlugin)
         |  .settings(
         |    libraryDependencies += "io.github.ondeoma" %%% "scalactive" % "<version>"
         |  )
         |""".stripMargin

    val codeSt = // language=html
      s"""<!-- src/main/scalactive/app/Counter.st -->
         |<script lang="scala">
         |case class State(count: RV[Int] = RV(0))
         |
         |def initState() = State()
         |</script>
         |
         |<template>
         |  <div class="counter">
         |    <p>count: {{ s.count }}</p>
         |    <button @click="s.count := s.count.v + 1">+1</button>
         |    <p v-if="s.count.mapC(_ >= 3)">3以上です!</p>
         |    <p v-else>3未満です.</p>
         |  </div>
         |</template>
         |
         |<style scoped>
         |.counter { padding: 8px; }
         |</style>
         |""".stripMargin

    val codeProps = // language=html
      s"""<!-- src/main/scalactive/app/Badge.st -->
         |<script lang="scala">
         |case class Props(label: String,
         |                 count: Reactive[Int] = RV(0),
         |                 onReset: Unit => Unit = _ => ())
         |</script>
         |
         |<template>
         |  <span>{{ props.label }}: {{ props.count }}</span>
         |  <button @click="props.onReset(())">reset</button>
         |</template>
         |
         |<!-- 親(.st)からの利用 -->
         |<Badge label="count" :count="s.count" @reset="s.count := 0" />
         |""".stripMargin

    val codeSlot = // language=html
      s"""<!-- src/main/scalactive/app/Card.st -->
         |<script lang="scala">
         |case class Props(title: String, slot: Slot = Slot.empty)
         |</script>
         |
         |<template>
         |  <div class="card">
         |    <h4>{{ props.title }}</h4>
         |    <slot><p>(内容がありません)</p></slot>
         |  </div>
         |</template>
         |
         |<!-- 親(.st)からの利用. 子要素がslotに展開されます -->
         |<Card title="お知らせ">
         |  <p>count = {{ s.count }}</p>
         |</Card>
         |""".stripMargin

    val codeModel = // language=html
      s"""<input type="text" v-model.trim="s.name">         <!-- RV[String] -->
         |<input type="number" v-model="s.age">            <!-- RV[Int] / RV[Double] -->
         |<input type="checkbox" v-model="s.agree">        <!-- RV[Boolean] -->
         |<input type="checkbox" value="a" v-model="s.tags"> <!-- RV[List[String]] -->
         |<input type="radio" value="red" v-model="s.color"> <!-- RV[String] -->
         |<select v-model="s.color">...</select>           <!-- RV[String] -->
         |""".stripMargin

    val codeUse = // language=scala
      s"""// 生成されたobject app.Counter は通常のコンポーネントとして使えます.
         |s\"\"\"<div>$${c(Counter())}</div>\"\"\"
         |""".stripMargin

    // language=html
    s"""<h1>単一ファイルコンポーネント(.st)</h1>
       |
       |<p>
       |  Vueの単一ファイルコンポーネントのように、<br>
       |  テンプレート・Scalaコード・CSSを1つの <code>.st</code> ファイルにまとめて記述できます.<br>
       |  sbtプラグイン(sbt-scalactive)がビルド時に通常のコンポーネント(Scalaソース)へ変換します.
       |</p>
       |
       |<ul class="toc">
       |  <li><a ${evClick(setHash("setup"))}>セットアップ</a></li>
       |  <li><a ${evClick(setHash("format"))}>ファイル形式</a></li>
       |  <li><a ${evClick(setHash("directives"))}>テンプレート構文</a></li>
       |  <li><a ${evClick(setHash("model"))}>v-model</a></li>
       |  <li><a ${evClick(setHash("props"))}>props</a></li>
       |  <li><a ${evClick(setHash("slot"))}>slot</a></li>
       |  <li><a ${evClick(setHash("demo"))}>デモ</a></li>
       |  <li><a ${evClick(setHash("editors"))}>エディタ対応</a></li>
       |  <li><a ${evClick(setHash("limitations"))}>制限事項</a></li>
       |</ul>
       |
       |<h2 id="setup">セットアップ</h2>
       |
       |<pre><code class="language-scala">${esc(codeSetup)}</code></pre>
       |
       |<p>
       |  <code>src/main/scalactive</code> と <code>src/main/scala</code> 配下の <code>*.st</code> が変換対象です.<br>
       |  ディレクトリ構成がパッケージに、ファイル名がobject名になります.<br>
       |  (<code>&lt;script&gt;</code>の先頭に <code>package</code> 宣言を書いた場合はそちらが優先されます.)
       |</p>
       |
       |<h2 id="format">ファイル形式</h2>
       |
       |<pre><code class="language-html">${esc(codeSt)}</code></pre>
       |
       |<ul>
       |  <li><code>&lt;script lang="scala"&gt;</code> : 生成されるobjectの本体になります(省略可).</li>
       |  <li>
       |    <code>def initState()</code> を定義するとStatefulComponentとなり、
       |    テンプレートからは状態を <code>s</code> で参照できます.<br>
       |    (<code>&lt;template state="st"&gt;</code> のように名前を変更できます.)
       |    定義しない場合はStatelessComponentになります.
       |  </li>
       |  <li><code>&lt;template&gt;</code> : 必須です.</li>
       |  <li>
       |    <code>&lt;style&gt;</code> : 初回利用時にheadへ挿入されます.
       |    <code>scoped</code> を付けると、このコンポーネントの要素にのみ適用されます.
       |  </li>
       |</ul>
       |
       |<pre><code class="language-scala">${esc(codeUse)}</code></pre>
       |
       |<h2 id="directives">テンプレート構文</h2>
       |
       |<table>
       |  <tr><th>構文</th><th>説明</th></tr>
       |  <tr><td><code>{{ expr }}</code></td><td>テキスト展開. Reactiveの場合は値の変化に追従します(HTMLエスケープされます).</td></tr>
       |  <tr><td><code>v-if="expr"</code><br><code>v-else-if="expr"</code><br><code>v-else</code></td><td>条件付きレンダリング(ifC). exprはReactive[Boolean]またはBoolean. 各条件は1度だけ評価されます.</td></tr>
       |  <tr><td><code>v-for="item in expr"</code><br><code>v-for="(item, i) in expr"</code></td><td>リストレンダリング. 添字の変数を書かなければforEachC(行の削除や並べ替えで行を作り直さないので速い)、書けばforC. RVListの場合itemはRV、RMListの場合はReactiveModelになります.</td></tr>
       |  <tr><td><code>&lt;template v-if/v-for&gt;</code></td><td>要素自体は出力せず、中身の複数要素をまとめて条件分岐・繰り返しします.</td></tr>
       |  <tr><td><code>v-show="expr"</code></td><td>表示切替(showIf).</td></tr>
       |  <tr><td><code>v-html="expr"</code></td><td>
       |    要素の中身を、式の値のHTMLで置き換えます(HtmlComponent). Reactiveの場合は値が変わるたびに展開し直します.
       |    要素の中身は空にしてください.<br>
       |    <b>値はエスケープされません.</b> ユーザーの入力など信頼できない値を渡すと、スクリプトを埋め込まれる危険があります.
       |  </td></tr>
       |  <tr><td><code>v-model="rv"</code></td><td>input/textarea/selectとRVの双方向バインディング. <code>.lazy</code> / <code>.trim</code> 修飾子が使えます(後述).</td></tr>
       |  <tr><td><code>:name="expr"</code><br><code>v-bind:name="expr"</code></td><td>属性バインディング. Reactive[Boolean]は属性の有無、それ以外は値として反映します.</td></tr>
       |  <tr><td><code>:class="expr"</code></td><td>クラスバインディング. String(空白区切り)またはList[String]. 静的なclass属性と併用できます.</td></tr>
       |  <tr><td><code>@event="stmt"</code><br><code>v-on:event="stmt"</code></td><td>
       |    イベントハンドリング. 文の中では <code>event</code> でEventを参照できます.<br>
       |    修飾子: <code>.prevent</code> / <code>.stop</code> / <code>.self</code>、
       |    キー <code>.enter</code> / <code>.esc</code> / <code>.tab</code> / <code>.space</code> / <code>.delete</code> / <code>.up</code> / <code>.down</code> / <code>.left</code> / <code>.right</code>、
       |    修飾キー <code>.ctrl</code> / <code>.shift</code> / <code>.alt</code> / <code>.meta</code>
       |    (例: <code>@keydown.enter.ctrl="send()"</code>).
       |  </td></tr>
       |  <tr><td><code>&lt;Other /&gt;</code></td><td>大文字で始まるタグは子コンポーネント(<code>c(Other())</code>)として展開されます. 属性はprops、子要素はslotとして渡されます.</td></tr>
       |  <tr><td><code>&lt;slot&gt;</code></td><td>親から渡された子要素を展開します. 中身は子要素がない場合の既定の内容です.</td></tr>
       |</table>
       |
       |<p>
       |  属性値の中で <code>"</code> を使う場合は、属性を <code>'</code> で囲むか <code>&amp;quot;</code> と記述してください.
       |</p>
       |
       |<h2 id="model">v-model</h2>
       |
       |<pre><code class="language-html">${esc(codeModel)}</code></pre>
       |
       |<ul>
       |  <li>RVの型によって同期方法が決まります. 対応していない型の場合はコンパイルエラーになります.</li>
       |  <li>テキスト入力は <code>input</code> イベント、チェックボックス・ラジオボタン・selectは <code>change</code> イベントで反映します. <code>.lazy</code> を付けると常に <code>change</code> で反映します.</li>
       |  <li>IMEで変換中の入力は、確定するまで反映しません.</li>
       |  <li><code>.trim</code> は前後の空白を除いた値をRVへ反映します(入力欄の表示はそのままです).</li>
       |  <li>RV[Int] / RV[Double] では、数値として読めない入力は反映しません.</li>
       |  <li><code>RV[List[String]]</code> はチェックボックスのグループ、または <code>&lt;select multiple&gt;</code> に使えます.</li>
       |</ul>
       |
       |<h2 id="props">props</h2>
       |
       |<p>
       |  <code>&lt;script&gt;</code> に <code>case class Props(...)</code> を定義すると、親から値を受け取るコンポーネントになります.<br>
       |  テンプレートからは <code>props</code> で参照できます.
       |  (<code>&lt;template props="p"&gt;</code> のように名前を変更できます.)
       |</p>
       |
       |<pre><code class="language-html">${esc(codeProps)}</code></pre>
       |
       |<ul>
       |  <li>子コンポーネントタグの属性は、<code>Badge(label = ..., count = ...)</code> のように名前付き引数として渡されます.</li>
       |  <li><code>name="text"</code> は文字列、値なしの <code>name</code> は <code>true</code>、<code>:name="expr"</code> はScalaの式になります.</li>
       |  <li>
       |    <code>@name="stmt"</code> は <code>onName = (event =&gt; { stmt })</code> として渡されます.
       |    子は <code>onName: A =&gt; Unit</code> をpropsに定義し、<code>props.onName(a)</code> で親へ通知します(値がない場合は <code>Unit =&gt; Unit</code>).
       |  </li>
       |  <li>ケバブケースの属性名はキャメルケースに変換されます(<code>on-reset</code> → <code>onReset</code>).</li>
       |  <li>デフォルト値のあるpropsは省略できます. すべて省略可能なら <code>&lt;Badge /&gt;</code> と書けます.</li>
       |  <li>
       |    propsは生成時に1度だけ渡されます. 値の変化を子に伝えたい場合はReactiveを渡してください.
       |    子から親への通知は、<code>@name</code> またはコールバック関数のpropsで行います.
       |  </li>
       |  <li>
       |    <code>def initState()</code> と併用できます. 初期状態をpropsから作る場合は <code>def initState(props: Props)</code> と定義します.
       |  </li>
       |  <li>Scalaで書く場合は <code>StatelessPropsComponent[P]</code> / <code>StatefulPropsComponent[P, S]</code> を継承します.</li>
       |</ul>
       |
       |<h2 id="slot">slot</h2>
       |
       |<p>
       |  子コンポーネントのタグの中に書いた要素は、Propsの <code>slot: Slot</code> として渡され、子の <code>&lt;slot&gt;</code> の位置に展開されます.
       |</p>
       |
       |<pre><code class="language-html">${esc(codeSlot)}</code></pre>
       |
       |<ul>
       |  <li>slotの内容は親のスコープで評価されるため、親の状態を参照できます. scoped CSSも親のものが適用されます.</li>
       |  <li>名前付きslot・スコープ付きslotは未対応です.</li>
       |</ul>
       |
       |<h2 id="demo">デモ</h2>
       |
       |<p>
       |  以下は <code>doc/src/main/scalactive/.../st/StSample.st</code>(と <code>StBadge.st</code> / <code>StCard.st</code>)から生成されたコンポーネントです.
       |</p>
       |
       |<div class="demo">
       |  ${c(StSample())}
       |</div>
       |
       |<h2 id="editors">エディタ対応</h2>
       |
       |<p>
       |  VS Code拡張とIntelliJプラグインで、<code>.st</code> のシンタックスハイライトと、Scalaの部分の言語機能が使えます.<br>
       |  <code>&lt;script&gt;</code> と、テンプレート内の <code>{{ }}</code>・ディレクティブ・<code>:attr</code>・<code>@event</code> の値がScalaとして、
       |  <code>&lt;style&gt;</code> がCSSとしてハイライトされます.
       |</p>
       |
       |<p>
       |  これらのScalaの部分では、言語サーバーにより次の機能が使えます.
       |</p>
       |
       |<ul>
       |  <li>補完</li>
       |  <li>型エラーなどの表示(.st上の位置で表示されます)</li>
       |  <li>ホバーでの型の表示、引数のヒント</li>
       |  <li>定義へのジャンプ(同じ.st内と、プロジェクト内の .scala / .st)</li>
       |</ul>
       |
       |<p>
       |  言語サーバーは、ScalactivePluginを有効にしたsbtプロジェクトで <code>sbt compile</code> を1度実行すると使えるようになります.
       |  (コンパイル時に、クラスパスなどを <code>target/scalactive-lsp.json</code> に書き出します.)<br>
       |  他の.stやScalaソースの定義は、最後にコンパイルした時点の内容で参照されます.<br>
       |  言語サーバーの実行にはJava 11以降が必要です(IntelliJではIDEのJavaを使います).
       |  初回はScalaのpresentation compilerをMaven Centralから取得します.<br>
       |  ビルド・インストール方法はリポジトリの <code>editors/README.md</code> を参照してください.
       |</p>
       |
       |<h2 id="limitations">制限事項</h2>
       |
       |<ul>
       |  <li>子コンポーネントへの <code>class</code>・<code>style</code>・<code>v-model</code> の指定、名前付きslotは未対応です.</li>
       |  <li><code>{{ }}</code> は属性値の中では使えません. <code>:name="expr"</code> を使ってください.</li>
       |  <li>
       |    sbtのコンパイルエラーは生成ソースの位置で報告されます. 埋め込まれた式には元ファイルの行番号コメントが付与されます.
       |    (エディタでは言語サーバーが.st上の位置で表示します.)
       |  </li>
       |  <li>言語サーバーは、ライブラリ(jar)内の定義へのジャンプ、ドキュメントの表示、自動importには対応していません.</li>
       |</ul>
       |""".stripMargin
  }

}
