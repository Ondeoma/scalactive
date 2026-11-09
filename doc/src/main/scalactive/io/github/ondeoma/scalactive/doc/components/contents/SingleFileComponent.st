<script lang="scala">
import Router.*
import io.github.ondeoma.scalactive.doc.components.contents.st.StSample

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

val codeModel = // language=html
  s"""<input type="text" v-model.trim="s.name">         <!-- RV[String] -->
     |<input type="number" v-model="s.age">            <!-- RV[Int] / RV[Double] -->
     |<input type="checkbox" v-model="s.agree">        <!-- RV[Boolean] -->
     |<input type="checkbox" value="a" v-model="s.tags"> <!-- RV[List[String]] -->
     |<input type="radio" value="red" v-model="s.color"> <!-- RV[String] -->
     |<select v-model="s.color">...</select>           <!-- RV[String] -->
     |""".stripMargin

val codeUse = // language=html
  s"""<!-- 他の.stからの利用(同じパッケージ、またはimportしたもの) -->
     |<template>
     |  <div><Counter /></div>
     |</template>
     |""".stripMargin
</script>

<template>
  <h1>単一ファイルコンポーネント(.st)</h1>

  <p>
    Scalactiveのコンポーネントは、<code>.st</code> ファイルで書きます.<br>
    Vueの単一ファイルコンポーネントのように、<br>
    テンプレート・Scalaコード・CSSを1つのファイルにまとめて記述します.<br>
    このページは、その書き方のリファレンスです.<br>
    sbtプラグインの設定や、ファイルの置き場所は、<br>
    <a @click='go("/start")'>クイックスタート</a>を参照ください.
  </p>

  <ul class="toc">
    <li><a @click='setHash("format")'>ファイル形式</a></li>
    <li><a @click='setHash("directives")'>テンプレート構文</a></li>
    <li><a @click='setHash("model")'>v-model</a></li>
    <li><a @click='setHash("props")'>props</a></li>
    <li><a @click='setHash("slot")'>slot</a></li>
    <li><a @click='setHash("lifecycle")'>ライフサイクルフック</a></li>
    <li><a @click='setHash("demo")'>デモ</a></li>
    <li><a @click='setHash("editors")'>エディタ対応</a></li>
    <li><a @click='setHash("limitations")'>制限事項</a></li>
  </ul>

  <h2 id="format">ファイル形式</h2>

  <pre><code class="language-st">{{ codeSt }}</code></pre>

  <ul>
    <li><code>&lt;script lang="scala"&gt;</code> : 生成されるobjectの本体になります(省略可).</li>
    <li>
      <code>def initState()</code> を定義するとStatefulComponentとなり、<br>
      テンプレートからは状態を <code>s</code> で参照できます.<br>
      (<code>&lt;template state="st"&gt;</code> のように名前を変更できます.)<br>
      定義しない場合はStatelessComponentになります.
    </li>
    <li><code>&lt;template&gt;</code> : 必須です.</li>
    <li>
      <code>&lt;style&gt;</code> : 初回利用時にheadへ挿入されます.<br>
      <code>scoped</code> を付けると、このコンポーネントの要素にのみ適用されます.
    </li>
  </ul>

  <p>
    <code>src/main/scalactive</code> と <code>src/main/scala</code> 配下の <code>*.st</code> が変換対象です.<br>
    ディレクトリ構成がパッケージに、ファイル名がobject名になります.<br>
    (<code>&lt;script&gt;</code>の先頭に <code>package</code> 宣言を書いた場合は、そちらが優先されます.)<br>
    生成されたobjectは、他の.stからタグとして使えます.
  </p>

  <pre><code class="language-st">{{ codeUse }}</code></pre>

  <h2 id="directives">テンプレート構文</h2>

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
      Stateに <code>ElementRef()</code> を持たせてください(後述のライフサイクルフックの例を参照).
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
      <code>.lazy</code> / <code>.trim</code> 修飾子が使えます(後述).
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
      <code>v-model</code> も使えます.
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

  <h2 id="model">v-model</h2>

  <pre><code class="language-st">{{ codeModel }}</code></pre>

  <ul>
    <li>
      RVの型によって同期方法が決まります.<br>
      対応していない型の場合はコンパイルエラーになります.
    </li>
    <li>
      テキスト入力は <code>input</code> イベント、<br>
      チェックボックス・ラジオボタン・selectは <code>change</code> イベントで反映します.<br>
      <code>.lazy</code> を付けると常に <code>change</code> で反映します.
    </li>
    <li>IMEで変換中の入力は、確定するまで反映しません.</li>
    <li><code>.trim</code> は前後の空白を除いた値をRVへ反映します(入力欄の表示はそのままです).</li>
    <li>RV[Int] / RV[Double] では、数値として読めない入力は反映しません.</li>
    <li>
      <code>RV[List[String]]</code> は、チェックボックスのグループ、<br>
      または <code>&lt;select multiple&gt;</code> に使えます.
    </li>
  </ul>

  <h2 id="props">props</h2>

  <p>
    <code>&lt;script&gt;</code> に <code>case class Props(...)</code> を定義すると、<br>
    親から値を受け取るコンポーネントになります.<br>
    テンプレートからは <code>props</code> で参照できます.<br>
    (<code>&lt;template props="p"&gt;</code> のように名前を変更できます.)
  </p>

  <pre><code class="language-st">{{ codeProps }}</code></pre>

  <ul>
    <li>
      子コンポーネントタグの属性は、<br>
      <code>Badge(label = ..., count = ...)</code> のように名前付き引数として渡されます.
    </li>
    <li>
      <code>name="text"</code> は文字列、値なしの <code>name</code> は <code>true</code>、<br>
      <code>:name="expr"</code> はScalaの式になります.
    </li>
    <li>
      <code>@name="stmt"</code> は <code>onName = (event =&gt; { stmt })</code> として渡されます.<br>
      子は <code>onName: A =&gt; Unit</code> をpropsに定義し、<br>
      <code>props.onName(a)</code> で親へ通知します(値がない場合は <code>Unit =&gt; Unit</code>).
    </li>
    <li>ケバブケースの属性名はキャメルケースに変換されます(<code>on-reset</code> → <code>onReset</code>).</li>
    <li>
      <code>v-model="rv"</code> はpropsの <code>model</code> に、<br>
      <code>v-model:name="rv"</code> はpropsの <code>name</code> にRVを渡します(後述).
    </li>
    <li>
      <code>class</code>・<code>style</code>・<code>id</code>・<code>role</code>・<code>data-*</code>・<code>aria-*</code>(<code>:</code>付きも可)はpropsにならず、<br>
      子コンポーネントのルート要素(最初の要素)に付け加えられます(属性の引き継ぎ).<br>
      classとstyleは子のものと合わされます.<br>
      子が描き直されたときも付け直され、Reactiveな値は変化に追従します.
    </li>
    <li>
      デフォルト値のあるpropsは省略できます.<br>
      すべて省略可能なら <code>&lt;Badge /&gt;</code> と書けます.
    </li>
    <li>
      propsは生成時に1度だけ渡されます.<br>
      値の変化を子に伝えたい場合はReactiveを渡してください.<br>
      子から親への通知は、<code>@name</code> またはコールバック関数のpropsで行います.
    </li>
    <li>
      <code>def initState()</code> と併用できます.<br>
      初期状態をpropsから作る場合は <code>def initState(props: Props)</code> と定義します.
    </li>
  </ul>

  <h3 id="component-model">コンポーネントのv-model</h3>

  <p>
    子コンポーネントのタグに <code>v-model</code> を書くと、RVをそのままpropsとして渡します.<br>
    親と子が同じRVを共有するので、<br>
    どちらで変更しても両方に反映されます(双方向バインディング).
  </p>

  <pre><code class="language-st">{{ codeComponentModel }}</code></pre>

  <ul>
    <li>
      <code>v-model="rv"</code> はpropsの <code>model</code>、<code>v-model:title="rv"</code> はpropsの <code>title</code> になります.<br>
      複数指定できます.
    </li>
    <li>
      子のpropsの型はRV(<code>RV[String]</code> など)にします.<br>
      修飾子(<code>.trim</code> など)は子の中のinputに指定してください.
    </li>
  </ul>

  <h2 id="slot">slot</h2>

  <p>
    子コンポーネントのタグの中に書いた要素は、Propsの <code>slot: Slot</code> として渡され、<br>
    子の <code>&lt;slot&gt;</code> の位置に展開されます.<br>
    <code>&lt;template #name&gt;</code>(または <code>&lt;template v-slot:name&gt;</code>)で囲んだ要素は、<br>
    名前付きスロットとしてPropsの <code>name: Slot</code> に渡され、<br>
    子の <code>&lt;slot name="name"&gt;</code> の位置に展開されます.
  </p>

  <pre><code class="language-st">{{ codeSlot }}</code></pre>

  <ul>
    <li>
      slotの内容は親のスコープで評価されるため、親の状態を参照できます.<br>
      scoped CSSも親のものが適用されます.
    </li>
    <li>
      名前付きスロットを受け取るには、子のPropsに <code>name: Slot = Slot.empty</code> を定義します<br>
      (ケバブケースの名前はキャメルケースになります).
    </li>
    <li><code>&lt;slot&gt;</code> の中身は、そのスロットが渡されなかった場合の既定の内容です.</li>
    <li><code>&lt;template #default&gt;</code> はデフォルトのslotです.</li>
  </ul>

  <h3 id="scoped-slot">スコープ付きスロット</h3>

  <p>
    子から値を受け取って内容を作るスロットです(Vueのスコープ付きスロット).<br>
    表やリストのような汎用部品で、<br>
    行の表示を親に任せる場合に使います.
  </p>

  <pre><code class="language-st">{{ codeScopedSlot }}</code></pre>

  <ul>
    <li>
      子はPropsに <code>name: ScopedSlot[A] = ScopedSlot.empty</code> を定義し、<br>
      <code>&lt;slot name="name" :value="expr"&gt;</code> で値(A)を渡して展開します.
    </li>
    <li>
      親は <code>&lt;template #name="x"&gt;</code> で値を受け取ります.<br>
      <code>x</code> の型は子のPropsから推論されます(<code>#name="x: Todo"</code> と型を書くこともできます).
    </li>
    <li>
      複数の値はタプルで渡し、<br>
      <code>#name="(item, i)"</code> のように分解して受け取れます.
    </li>
  </ul>

  <h2 id="lifecycle">ライフサイクルフック</h2>

  <p>
    <code>&lt;script&gt;</code> に <code>def mounted</code> / <code>def unmounted</code> を定義すると、<br>
    コンポーネントがDOMに挿入された後・片付けられた後に呼ばれます(Vueのmounted / unmounted).<br>
    引数はなし、または <code>Props</code>・Stateを受け取れます(順序は自由).
  </p>

  <pre><code class="language-st">{{ codeLifecycle }}</code></pre>

  <ul>
    <li>
      <code>mounted</code> は現在の処理(描画・描き直し)が終わった直後に、<br>
      子コンポーネントのものから順に呼ばれます.
    </li>
    <li><code>unmounted</code> は描き直し・v-ifの切り替え・v-forの行の削除などでも呼ばれます.</li>
  </ul>

  <h2 id="demo">デモ</h2>

  <p>
    以下は <code>doc/src/main/scalactive/.../st/StSample.st</code>(と <code>StBadge.st</code> / <code>StCard.st</code> など)から、<br>
    生成されたコンポーネントです.
  </p>

  <div class="demo">
    <StSample />
  </div>

  <h2 id="editors">エディタ対応</h2>

  <p>
    VS Code拡張とIntelliJプラグインで、<code>.st</code> のシンタックスハイライトと、<br>
    Scalaの部分の言語機能が使えます.<br>
    <code>&lt;script&gt;</code> と、テンプレート内の <code>&#123;&#123; &#125;&#125;</code>・ディレクティブ・<code>:attr</code>・<code>@event</code> の値がScalaとして、<br>
    <code>&lt;style&gt;</code> がCSSとしてハイライトされます.
  </p>

  <p>
    これらのScalaの部分では、言語サーバーにより次の機能が使えます.
  </p>

  <ul>
    <li>
      補完<br>
      (テンプレートのHTMLの部分では、同じパッケージのコンポーネント名、<br>
      子コンポーネントのprops・イベント・v-model、ディレクティブ、スロット名、<br>
      HTMLの標準のタグ・属性・イベント・列挙された属性値(inputのtypeなど)も補完します)
    </li>
    <li>型エラーなどの表示(.st上の位置で表示されます)</li>
    <li>
      子コンポーネントのpropの参照の検索と名前の変更<br>
      (定義・属性・名前付きスロット・定義した.stの中の参照をまとめて書き換え.<br>
      @closeやv-model:xなどの書き方に合わせます)
    </li>
    <li>
      子コンポーネントのタグの検査(未知のprop・スロット、必須のpropの不足を警告)と、<br>
      そのクイックフィックス(近い名前への修正、足りない必須のpropの書き足し)
    </li>
    <li>推論した型のインレイヒント(scriptの型を書いていない val / def など)</li>
    <li>
      ホバーでの型の表示<br>
      (子コンポーネントのタグではPropsの一覧、属性ではpropの型、<br>
      HTMLのタグ・属性・イベントでは説明)、引数のヒント
    </li>
    <li>
      定義へのジャンプ<br>
      (同じ.st内と、プロジェクト内の .scala / .st.<br>
      子コンポーネントのタグ名・属性名からは、その.stのPropsへ)
    </li>
    <li>
      ドキュメントのフォーマット(整形).<br>
      テンプレートは、要素の入れ子に合わせて2スペースずつインデントし直します.<br>
      <code>&lt;style&gt;</code> のCSSは、1行で書いたルールは1行のまま空白を整え、<br>
      複数行のルールは宣言を1行ずつインデントします.<br>
      <code>&lt;script&gt;</code> の中はscalafmtで整形します<br>
      (<code>.scalafmt.conf</code> があればその設定、なければ改行の位置を保つ組み込みの設定).<br>
      構文エラーなどで整形できないときは、<br>
      行の途中の余分な空白(連続した空白、括弧の内側と , の前の空白)だけを取り除きます.
    </li>
  </ul>

  <p>
    言語サーバーは、ScalactivePluginを有効にしたsbtプロジェクトで、<br>
    <code>sbt compile</code> を1度実行すると使えるようになります.<br>
    (コンパイル時に、クラスパスなどを <code>target/scalactive-lsp.json</code> に書き出します.)<br>
    他の.stやScalaソースの定義は、最後にコンパイルした時点の内容で参照されます.<br>
    言語サーバーの実行にはJava 11以降が必要です(IntelliJではIDEのJavaを使います).<br>
    初回はScalaのpresentation compilerをMaven Centralから取得します.<br>
    ビルド・インストール方法は、リポジトリの <code>editors/README.md</code> を参照してください.
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
      (エディタでは言語サーバーが.st上の位置で表示します.)
    </li>
    <li>
      言語サーバーは、ライブラリ(jar)内の定義へのジャンプ、<br>
      ドキュメントの表示、自動importには対応していません.
    </li>
  </ul>

  <p>
    .stを使わずにScalaで書く方法は、<br>
    <a @click='go("/scalaApi")'>Scala API(従来の書き方)</a>を参照ください.
  </p>
</template>
