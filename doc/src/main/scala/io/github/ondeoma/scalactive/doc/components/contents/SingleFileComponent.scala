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

    val codeComponentModel = // language=html
      s"""<!-- src/main/scalactive/app/Field.st -->
         |<script lang="scala">
         |case class Props(label: String, model: RV[String], note: RV[String] = RV(""))
         |</script>
         |
         |<template>
         |  <label>{{ props.label }}: <input v-model="props.model"></label>
         |  <input v-model="props.note">
         |</template>
         |
         |<!-- 親(.st)からの利用 -->
         |<Field label="名前" v-model="s.name" v-model:note="s.memo" />
         |""".stripMargin

    val codeSlot = // language=html
      s"""<!-- src/main/scalactive/app/Card.st -->
         |<script lang="scala">
         |case class Props(title: String,
         |                 slot: Slot = Slot.empty,
         |                 footer: Slot = Slot.empty)
         |</script>
         |
         |<template>
         |  <div class="card">
         |    <h4>{{ props.title }}</h4>
         |    <slot><p>(内容がありません)</p></slot>
         |    <footer><slot name="footer">(footerなし)</slot></footer>
         |  </div>
         |</template>
         |
         |<!-- 親(.st)からの利用. <template #footer>の中身はfooterに、それ以外の子要素はslotに展開されます -->
         |<Card title="お知らせ">
         |  <p>count = {{ s.count }}</p>
         |  <template #footer><small>更新: 今日</small></template>
         |</Card>
         |""".stripMargin

    val codeScopedSlot = // language=html
      s"""<!-- src/main/scalactive/app/TodoList.st -->
         |<script lang="scala">
         |case class Todo(id: Int, title: String)
         |case class Props(todos: List[Todo],
         |                 item: ScopedSlot[Todo] = ScopedSlot.empty)
         |</script>
         |
         |<template>
         |  <ul>
         |    <!-- :valueで値を渡す. 親が内容を渡さなければ中身(既定の内容)を表示 -->
         |    <li v-for="t in props.todos" :key="t.id"><slot name="item" :value="t">{{ t.title }}</slot></li>
         |  </ul>
         |</template>
         |
         |<!-- 親(.st)からの利用. #item="t" で子から受け取った値を使う -->
         |<TodoList :todos="s.todos">
         |  <template #item="t"><b>{{ t.id }}</b> {{ t.title }}</template>
         |</TodoList>
         |""".stripMargin

    val codeModel = // language=html
      s"""<input type="text" v-model.trim="s.name">         <!-- RV[String] -->
         |<input type="number" v-model="s.age">            <!-- RV[Int] / RV[Double] -->
         |<input type="checkbox" v-model="s.agree">        <!-- RV[Boolean] -->
         |<input type="checkbox" value="a" v-model="s.tags"> <!-- RV[List[String]] -->
         |<input type="radio" value="red" v-model="s.color"> <!-- RV[String] -->
         |<select v-model="s.color">...</select>           <!-- RV[String] -->
         |""".stripMargin

    val codeLifecycle = // language=html
      s"""<script lang="scala">
         |case class State(input: RV[String] = RV(""),
         |                 search: ElementRef[HTMLInputElement] = ElementRef())
         |
         |def initState() = State()
         |
         |// DOMに挿入された後に実行される(引数はなし、またはProps・Stateを受け取れる)
         |def mounted(s: State): Unit = s.search.foreach(_.focus())
         |
         |// 片付けられた(DOMから取り除かれた)後に実行される
         |def unmounted(): Unit = println("unmounted")
         |</script>
         |
         |<template>
         |  <input ref="s.search" v-model="s.input">
         |</template>
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
       |  <li><a ${evClick(setHash("lifecycle"))}>ライフサイクルフック</a></li>
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
       |  <tr><td><code>v-for="..." :key="expr"</code></td><td>
       |    行を対応付けるキー. exprはitemから計算します(例: <code>:key="item.id"</code>).
       |    リストが変わったとき、キーが同じ行は(値が変わっていなければ)作り直さずに使い回されます.
       |    指定しない場合は、RVList・RMListでは要素のオブジェクト、それ以外では要素の値で対応付けます.
       |    <code>&lt;template v-for&gt;</code> にも指定できます.
       |  </td></tr>
       |  <tr><td><code>&lt;template v-if/v-for&gt;</code></td><td>要素自体は出力せず、中身の複数要素をまとめて条件分岐・繰り返しします.</td></tr>
       |  <tr><td><code>v-show="expr"</code></td><td>表示切替(showIf).</td></tr>
       |  <tr><td><code>ref="expr"</code></td><td>
       |    テンプレート参照. exprは <code>ElementRef[HTMLInputElement]</code> などで、要素が作られると設定され、片付けられると空に戻ります.
       |    インスタンスごとの参照になるよう、Stateに <code>ElementRef()</code> を持たせてください(後述のライフサイクルフックの例を参照).
       |  </td></tr>
       |  <tr><td><code>v-html="expr"</code></td><td>
       |    要素の中身を、式の値のHTMLで置き換えます(HtmlComponent). Reactiveの場合は値が変わるたびに展開し直します.
       |    要素の中身は空にしてください.<br>
       |    <b>値はエスケープされません.</b> ユーザーの入力など信頼できない値を渡すと、スクリプトを埋め込まれる危険があります.
       |  </td></tr>
       |  <tr><td><code>v-model="rv"</code></td><td>input/textarea/selectとRVの双方向バインディング. <code>.lazy</code> / <code>.trim</code> 修飾子が使えます(後述).</td></tr>
       |  <tr><td><code>:name="expr"</code><br><code>v-bind:name="expr"</code></td><td>属性バインディング. Reactive[Boolean]は属性の有無、それ以外は値として反映します.</td></tr>
       |  <tr><td><code>:class="expr"</code></td><td>
       |    クラスバインディング. 静的なclass属性と併用できます. exprには次のものが使えます(Reactiveでも可).<br>
       |    ・String(空白区切り)、List[String]<br>
       |    ・オブジェクト記法: クラス名と条件の組 <code>"active" -&gt; s.on</code>、
       |    <code>Map("active" -&gt; s.on, "big" -&gt; true)</code>(条件はBooleanまたはReactive[Boolean])<br>
       |    ・配列記法: 文字列と組の混在 <code>List("btn", "active" -&gt; s.on)</code>
       |  </td></tr>
       |  <tr><td><code>:style="expr"</code></td><td>
       |    スタイルバインディング. Stringの場合はstyle属性全体になります.<br>
       |    オブジェクト記法 <code>Map("color" -&gt; s.color, "fontSize" -&gt; "12px")</code>(または組 <code>"display" -&gt; "none"</code>)は、
       |    プロパティごとに設定され、静的なstyle属性と併用できます.
       |    プロパティ名はキャメルケースでも書けます. 値がNone・false・空文字のプロパティは外します. 値にはReactiveも使えます.
       |  </td></tr>
       |  <tr><td><code>@event="stmt"</code><br><code>v-on:event="stmt"</code></td><td>
       |    イベントハンドリング. 文の中では <code>event</code> でEventを参照できます.<br>
       |    修飾子: <code>.prevent</code> / <code>.stop</code> / <code>.self</code>、
       |    キー <code>.enter</code> / <code>.esc</code> / <code>.tab</code> / <code>.space</code> / <code>.delete</code> / <code>.up</code> / <code>.down</code> / <code>.left</code> / <code>.right</code>、
       |    修飾キー <code>.ctrl</code> / <code>.shift</code> / <code>.alt</code> / <code>.meta</code>
       |    (例: <code>@keydown.enter.ctrl="send()"</code>).<br>
       |    リスナーのオプション: <code>.once</code>(条件を満たした最初の1回だけ実行)、
       |    <code>.capture</code>(子要素より先にキャプチャフェーズで実行)、
       |    <code>.passive</code>(スクロールなどを妨げない. <code>.prevent</code> とは併用できません).
       |  </td></tr>
       |  <tr><td><code>&lt;Other /&gt;</code></td><td>大文字で始まるタグは子コンポーネント(<code>c(Other())</code>)として展開されます. 属性はprops、子要素はslotとして渡されます. <code>v-model</code> も使えます.</td></tr>
       |  <tr><td><code>&lt;slot&gt;</code><br><code>&lt;slot name="x"&gt;</code></td><td>親から渡された子要素(名前付きスロット)を展開します. 中身は渡されなかった場合の既定の内容です. <code>:value="v"</code> でスコープ付きスロットに値を渡します.</td></tr>
       |  <tr><td><code>&lt;Await :load="() =&gt; f()"&gt;</code></td><td>
       |    非同期の処理(Future)の状態に応じて表示を切り替えます. <code>&lt;template #pending&gt;</code>(読み込み中)、
       |    <code>&lt;template #default="v"&gt;</code>(結果)、<code>&lt;template #error="err"&gt;</code>(失敗. <code>err.retry()</code> で読み込み直し)を書きます.
       |  </td></tr>
       |  <tr><td><code>&lt;ErrorBoundary&gt;</code></td><td>
       |    中身で起きたエラー(描画・描き直し・mountedの中の例外)を受け止め、<code>&lt;template #fallback="err"&gt;</code> の表示に切り替えます.
       |    <code>err.message</code> でメッセージ、<code>err.retry()</code> で中身の描き直しができます. <code>@error="..."</code> でエラーを受け取れます.
       |  </td></tr>
       |  <tr><td><code>&lt;Teleport to="body"&gt;</code></td><td>
       |    中身を別の要素(toのCSSセレクター)の末尾に描画します(モーダルなど). <code>disabled</code> を付けると元の位置に描画します.
       |    scoped CSSやイベントはそのまま使えます.
       |  </td></tr>
       |  <tr><td><code>&lt;template #x&gt;</code><br><code>&lt;template v-slot:x&gt;</code></td><td>子コンポーネントのタグの中で、名前付きスロットxに渡す内容を囲みます. <code>#x="v"</code> とするとスコープ付きスロットとして子から値を受け取ります.</td></tr>
       |</table>
       |
       |<p>
       |  属性値の中で <code>"</code> を使う場合は、属性を <code>'</code> で囲むか <code>&amp;quot;</code> と記述してください.
       |  (例: <code>:class='Map("active" -&gt; s.on)'</code>)
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
       |  <li>
       |    <code>v-model="rv"</code> はpropsの <code>model</code> に、<code>v-model:name="rv"</code> はpropsの <code>name</code> にRVを渡します(後述).
       |  </li>
       |  <li>
       |    <code>class</code>・<code>style</code>・<code>id</code>・<code>role</code>・<code>data-*</code>・<code>aria-*</code>(<code>:</code>付きも可)はpropsにならず、
       |    子コンポーネントのルート要素(最初の要素)に付け加えられます(属性の引き継ぎ). classとstyleは子のものと合わされます.
       |    子が描き直されたときも付け直され、Reactiveな値は変化に追従します.
       |  </li>
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
       |<h3 id="component-model">コンポーネントのv-model</h3>
       |
       |<p>
       |  子コンポーネントのタグに <code>v-model</code> を書くと、RVをそのままpropsとして渡します.
       |  親と子が同じRVを共有するので、どちらで変更しても両方に反映されます(双方向バインディング).
       |</p>
       |
       |<pre><code class="language-html">${esc(codeComponentModel)}</code></pre>
       |
       |<ul>
       |  <li><code>v-model="rv"</code> はpropsの <code>model</code>、<code>v-model:title="rv"</code> はpropsの <code>title</code> になります. 複数指定できます.</li>
       |  <li>子のpropsの型はRV(<code>RV[String]</code> など)にします. 修飾子(<code>.trim</code> など)は子の中のinputに指定してください.</li>
       |</ul>
       |
       |<h2 id="slot">slot</h2>
       |
       |<p>
       |  子コンポーネントのタグの中に書いた要素は、Propsの <code>slot: Slot</code> として渡され、子の <code>&lt;slot&gt;</code> の位置に展開されます.<br>
       |  <code>&lt;template #name&gt;</code>(または <code>&lt;template v-slot:name&gt;</code>)で囲んだ要素は名前付きスロットとして
       |  Propsの <code>name: Slot</code> に渡され、子の <code>&lt;slot name="name"&gt;</code> の位置に展開されます.
       |</p>
       |
       |<pre><code class="language-html">${esc(codeSlot)}</code></pre>
       |
       |<ul>
       |  <li>slotの内容は親のスコープで評価されるため、親の状態を参照できます. scoped CSSも親のものが適用されます.</li>
       |  <li>名前付きスロットを受け取るには、子のPropsに <code>name: Slot = Slot.empty</code> を定義します(ケバブケースの名前はキャメルケースになります).</li>
       |  <li><code>&lt;slot&gt;</code> の中身は、そのスロットが渡されなかった場合の既定の内容です.</li>
       |  <li><code>&lt;template #default&gt;</code> はデフォルトのslotです.</li>
       |</ul>
       |
       |<h3 id="scoped-slot">スコープ付きスロット</h3>
       |
       |<p>
       |  子から値を受け取って内容を作るスロットです(Vueのスコープ付きスロット). 表やリストのような汎用部品で、行の表示を親に任せる場合に使います.
       |</p>
       |
       |<pre><code class="language-html">${esc(codeScopedSlot)}</code></pre>
       |
       |<ul>
       |  <li>子はPropsに <code>name: ScopedSlot[A] = ScopedSlot.empty</code> を定義し、<code>&lt;slot name="name" :value="expr"&gt;</code> で値(A)を渡して展開します.</li>
       |  <li>親は <code>&lt;template #name="x"&gt;</code> で値を受け取ります. <code>x</code> の型は子のPropsから推論されます(<code>#name="x: Todo"</code> と型を書くこともできます).</li>
       |  <li>複数の値はタプルで渡し、<code>#name="(item, i)"</code> のように分解して受け取れます.</li>
       |  <li>Scalaで書く場合は、親は <code>ScopedSlot { x =&gt; cm =&gt; ... }</code> を渡し、子は <code>props.name(value)()</code> で展開します.</li>
       |</ul>
       |
       |<h2 id="lifecycle">ライフサイクルフック</h2>
       |
       |<p>
       |  <code>&lt;script&gt;</code> に <code>def mounted</code> / <code>def unmounted</code> を定義すると、
       |  コンポーネントがDOMに挿入された後・片付けられた後に呼ばれます(Vueのmounted / unmounted).
       |  引数はなし、または <code>Props</code>・Stateを受け取れます(順序は自由).
       |</p>
       |
       |<pre><code class="language-html">${esc(codeLifecycle)}</code></pre>
       |
       |<ul>
       |  <li><code>mounted</code> は現在の処理(描画・描き直し)が終わった直後に、子コンポーネントのものから順に呼ばれます.</li>
       |  <li><code>unmounted</code> は描き直し・v-ifの切り替え・v-forの行の削除などでも呼ばれます.</li>
       |  <li>Scalaで書く場合は、genHtmlの中で <code>onMounted {...}</code> / <code>onUnmounted {...}</code> を呼びます(コンポーネントのページを参照).</li>
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
