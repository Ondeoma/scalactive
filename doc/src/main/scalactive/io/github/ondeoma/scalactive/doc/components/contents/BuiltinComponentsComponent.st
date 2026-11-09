<script lang="scala">
import Router.*
import io.github.ondeoma.scalactive.doc.components.contents.builtin.*

val codeNavLinks = // language=html
  """<template>
    |  <nav class="nav-links">
    |    <RouterLink to="/">はじめに</RouterLink> /
    |    <RouterLink to="/builtinComponents">組込コンポーネント(このページ)</RouterLink> /
    |    <RouterLink to="/sfc" active-class="on">単一ファイルコンポーネント</RouterLink>
    |  </nav>
    |</template>
    |
    |<style>
    |.nav-links .router-link-active { font-weight: bold; }
    |.nav-links .router-link-exact-active { color: tomato; }
    |.nav-links .on { text-decoration: underline; }
    |</style>
    |""".stripMargin

val codeModalSample = // language=html
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
    |          <p>bodyの末尾に描画されています.</p>
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

val codeSafeBox = // language=html
  """<script lang="scala">
    |case class State(crash: RV[Boolean] = RV(false))
    |
    |def initState() = State()
    |
    |// 描画中に例外を起こす
    |def boom(): String = throw new RuntimeException("描画中にエラーが起きました")
    |</script>
    |
    |<template>
    |  <ErrorBoundary @error="println(event)">
    |    <p>
    |      <span v-if="s.crash">{{ boom() }}</span><span v-else>正常です.</span>
    |      <button @click="s.crash := true">エラーを起こす</button>
    |    </p>
    |    <template #fallback="err">
    |      <p class="error">{{ err.message }} <button @click="s.crash := false; err.retry()">戻す</button></p>
    |    </template>
    |  </ErrorBoundary>
    |</template>
    |
    |<style scoped>
    |.error { color: tomato; }
    |</style>
    |""".stripMargin

val codeQuoteLoader = // language=html
  """<script lang="scala">
    |import scala.concurrent.{Future, Promise}
    |
    |case class State(fail: RV[Boolean] = RV(false),
    |                 show: RV[Boolean] = RV(true))
    |
    |def initState() = State()
    |
    |// 0.8秒後に完了する非同期の処理
    |def fetchQuote(fail: Boolean): Future[String] = {
    |  val p = Promise[String]()
    |  window.setTimeout(
    |    () => {
    |      if (fail) p.failure(new RuntimeException("読み込みに失敗しました"))
    |      else p.success("Scalactiveは、Scala.jsのリアクティブなフレームワークです.")
    |    },
    |    800)
    |  p.future
    |}
    |</script>
    |
    |<template>
    |  <div>
    |    <label><input type="checkbox" v-model="s.fail"> 失敗させる</label>
    |    <!-- v-ifを付け直してAwaitを作り直す(もう1度読み込む) -->
    |    <button @click="s.show := false; s.show := true">読み込み直す</button>
    |    <Await v-if="s.show" :load="() => fetchQuote(s.fail.v)">
    |      <template #pending><p>読み込み中...</p></template>
    |      <template #default="quote"><p>{{ quote }}</p></template>
    |      <template #error="err"><p class="error">{{ err.message }} <button @click="err.retry()">再試行</button></p></template>
    |    </Await>
    |  </div>
    |</template>
    |
    |<style scoped>
    |.error { color: tomato; }
    |</style>
    |""".stripMargin

val codeInputSamples = // language=html
  """<script lang="scala">
    |case class State(text: RV[String] = RV("TEXT"),
    |                 num: RV[Int] = RV(1),
    |                 memo: RV[String] = RV(""),
    |                 agree: RV[Boolean] = RV(false),
    |                 tags: RV[List[String]] = RV(List()),
    |                 color: RV[String] = RV("red"),
    |                 size: RV[Int] = RV(2),
    |                 files: RV[List[File]] = RV(List()))
    |
    |def initState() = State()
    |
    |// ファイルはv-modelではなく、changeイベントで受け取る
    |def onFiles(s: State, e: Event): Unit = {
    |  s.files := e.target.asInstanceOf[HTMLInputElement].files.toList
    |}
    |</script>
    |
    |<template>
    |  <div>
    |    <p><input type="text" v-model.lazy="s.text"> → {{ s.text }}</p>
    |    <p><input type="number" v-model="s.num"> → {{ s.num }}</p>
    |    <p><textarea v-model="s.memo"></textarea> → {{ s.memo }}</p>
    |    <p><label><input type="checkbox" v-model="s.agree"> 同意する</label> → {{ s.agree }}</p>
    |    <p>
    |      <label><input type="checkbox" value="A" v-model="s.tags"> A</label>
    |      <label><input type="checkbox" value="B" v-model="s.tags"> B</label>
    |      → {{ s.tags.mapC(_.mkString(", ")) }}
    |    </p>
    |    <p>
    |      <label><input type="radio" name="input-color" value="red" v-model="s.color"> red</label>
    |      <label><input type="radio" name="input-color" value="blue" v-model="s.color"> blue</label>
    |      → {{ s.color }}
    |    </p>
    |    <p>
    |      <select v-model="s.size">
    |        <option value="1">One</option>
    |        <option value="2">Two</option>
    |      </select>
    |      → {{ s.size }}
    |    </p>
    |    <p><input type="file" multiple @change="onFiles(s, event)"> → {{ s.files.mapC(_.map(_.name).mkString(", ")) }}</p>
    |  </div>
    |</template>
    |""".stripMargin
</script>

<template>
  <h1>組込コンポーネント</h1>

  <p>
    .stのテンプレートで、タグとして使える標準のコンポーネントです.<br>
    子コンポーネントと同じく、属性はprops、子要素はslotとして渡されます.<br>
    importは不要です.
  </p>

  <ul class="toc">
    <li><a @click='setHash("RouterLink")'>RouterLink</a></li>
    <li><a @click='setHash("Teleport")'>Teleport</a></li>
    <li><a @click='setHash("ErrorBoundary")'>ErrorBoundary</a></li>
    <li><a @click='setHash("Await")'>Await</a></li>
    <li><a @click='setHash("Inputs")'>入力要素とv-model</a></li>
  </ul>

  <h2 id="RouterLink">RouterLink</h2>

  <p>
    <code>&lt;a href&gt;</code> を描画し、クリックでRouter.goするリンクです.<br>
    (Vue Routerのrouter-link)<br>
    Ctrl/⌘/Shiftを押しながらのクリックや中クリックは、<br>
    ブラウザ(新しいタブで開くなど)に任せます.
  </p>

  <table class="disc-table">
    <thead>
      <tr>
        <th>属性</th>
        <th>説明</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <th>to</th>
        <td>遷移先のパス</td>
      </tr>
      <tr>
        <th>active-class</th>
        <td>
          現在のパスがtoと同じか、その下にある場合に付けるクラス.<br>
          (<code>/</code> は完全一致のみ)<br>
          既定は <code>router-link-active</code>
        </td>
      </tr>
      <tr>
        <th>exact-active-class</th>
        <td>
          現在のパスがtoと同じ場合に付けるクラス.<br>
          既定は <code>router-link-exact-active</code>
        </td>
      </tr>
      <tr>
        <th>(子要素)</th>
        <td>リンクの中身</td>
      </tr>
    </tbody>
  </table>

  <h3>利用例</h3>

  <pre><code class="language-html">{{ codeNavLinks }}</code></pre>

  <div class="rendered">
    <NavLinks />
  </div>

  <h2 id="Teleport">Teleport</h2>

  <p>
    中身を、別の要素(CSSセレクターで指定)の末尾に描画します.<br>
    VueのTeleport、ReactのPortalにあたります.<br>
    モーダル・トースト・ツールチップなどを、<br>
    親要素のoverflowやz-indexの影響を受けない場所に描く場合に使います.<br>
    元の位置には、目印のコメントだけが残ります.<br>
    中身はこのコンポーネントのものとして扱われます.<br>
    そのため、リアクティブな表示・イベント・scoped CSSはそのまま動き、<br>
    片付けられると描画先から取り除かれます.
  </p>

  <table class="disc-table">
    <thead>
      <tr>
        <th>属性</th>
        <th>説明</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <th>to</th>
        <td>描画先のCSSセレクター(例: <code>body</code>、<code>#modals</code>)</td>
      </tr>
      <tr>
        <th>disabled</th>
        <td>
          付けた場合(trueの場合)は移動せず、元の位置に描画します.<br>
          <code>:disabled="expr"</code> で切り替えることもできます.
        </td>
      </tr>
      <tr>
        <th>(子要素)</th>
        <td>描画する中身</td>
      </tr>
    </tbody>
  </table>

  <h3>利用例</h3>

  <pre><code class="language-html">{{ codeModalSample }}</code></pre>

  <div class="rendered">
    <ModalSample />
  </div>

  <h2 id="ErrorBoundary">ErrorBoundary</h2>

  <p>
    中身で起きたエラーを受け止め、代わりの表示に切り替えます.<br>
    (ReactのError Boundary、VueのerrorCaptured)<br>
    描画・描き直し・mounted・イベントハンドラーの中の例外が対象です.<br>
    境界の外の例外は、<code>ScalactiveConfig.errorHandler</code> に渡されます.
  </p>

  <table class="disc-table">
    <thead>
      <tr>
        <th>属性・スロット</th>
        <th>説明</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <th>#fallback="err"</th>
        <td>
          エラーのときの表示.<br>
          <code>err.message</code> でメッセージ、<code>err.error</code> で例外、<br>
          <code>err.retry()</code> で中身の描き直しができます.<br>
          省略した場合は、メッセージだけを表示します.
        </td>
      </tr>
      <tr>
        <th>@error</th>
        <td>
          エラーを受け止めたときに呼ばれます.<br>
          <code>event</code> で例外(Throwable)を参照できます.<br>
          省略した場合は、コンソールに出力します.
        </td>
      </tr>
      <tr>
        <th>(子要素)</th>
        <td>中身</td>
      </tr>
    </tbody>
  </table>

  <h3>利用例</h3>

  <pre><code class="language-html">{{ codeSafeBox }}</code></pre>

  <div class="rendered">
    <SafeBox />
  </div>

  <h2 id="Await">Await</h2>

  <p>
    非同期の処理(Future)の状態に応じて、表示を切り替えます.<br>
    (VueのSuspense)<br>
    描画されたときにloadを呼び、完了するまではpendingを表示します.<br>
    完了したら結果を、失敗したらerrorを表示します.<br>
    完了前に片付けられた場合は、完了しても描き直しません.
  </p>

  <table class="disc-table">
    <thead>
      <tr>
        <th>属性・スロット</th>
        <th>説明</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <th>:load</th>
        <td>Futureを返す関数(<code>() =&gt; Future[A]</code>)</td>
      </tr>
      <tr>
        <th>#pending</th>
        <td>読み込み中の表示</td>
      </tr>
      <tr>
        <th>#default="v"</th>
        <td>完了したときの表示. <code>v</code> で結果(A)を参照できます.</td>
      </tr>
      <tr>
        <th>#error="err"</th>
        <td>
          失敗したときの表示.<br>
          <code>err.message</code> でメッセージ、<code>err.retry()</code> で読み込み直しができます.
        </td>
      </tr>
    </tbody>
  </table>

  <h3>利用例</h3>

  <pre><code class="language-html">{{ codeQuoteLoader }}</code></pre>

  <div class="rendered">
    <QuoteLoader />
  </div>

  <h2 id="Inputs">入力要素とv-model</h2>

  <p>
    .stでは、入力用のコンポーネントは使いません.<br>
    HTMLの <code>&lt;input&gt;</code>・<code>&lt;textarea&gt;</code>・<code>&lt;select&gt;</code> に、<br>
    <code>v-model</code> でRVをバインドします.<br>
    RVの型によって、同期の方法が決まります.<br>
    (詳しくは <a @click='go("/sfc")'>単一ファイルコンポーネント</a> のv-modelを参照ください.)
  </p>

  <pre><code class="language-html">{{ codeInputSamples }}</code></pre>

  <div class="rendered">
    <InputSamples />
  </div>

  <p>
    Scalaで書く場合のコンポーネントとの対応は、次のとおりです.
  </p>

  <table class="disc-table">
    <thead>
      <tr>
        <th>コンポーネント</th>
        <th>.stでの書き方</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <th>TextComponent</th>
        <td><code>&#123;&#123; expr &#125;&#125;</code></td>
      </tr>
      <tr>
        <th>HtmlComponent</th>
        <td><code>&lt;div v-html="expr"&gt;&lt;/div&gt;</code></td>
      </tr>
      <tr>
        <th>ButtonComponent</th>
        <td><code>&lt;button @click="..."&gt;</code></td>
      </tr>
      <tr>
        <th>LinkComponent</th>
        <td><code>&lt;a @click="..."&gt;</code>(画面遷移には <code>&lt;RouterLink&gt;</code>)</td>
      </tr>
      <tr>
        <th>TextInputStringComponent<br>TextInputGeneralComponent</th>
        <td>
          <code>&lt;input v-model="rv"&gt;</code>(RV[String]・RV[Int]・RV[Double]).<br>
          blur・changeで反映する場合は <code>v-model.lazy</code>
        </td>
      </tr>
      <tr>
        <th>TextAreaComponent</th>
        <td><code>&lt;textarea v-model="rv"&gt;&lt;/textarea&gt;</code></td>
      </tr>
      <tr>
        <th>FileInputComponent</th>
        <td><code>&lt;input type="file" @change="..."&gt;</code></td>
      </tr>
      <tr>
        <th>CheckboxBooleanComponent</th>
        <td><code>&lt;input type="checkbox" v-model="rv"&gt;</code>(RV[Boolean])</td>
      </tr>
      <tr>
        <th>CheckboxGroupPartComponent</th>
        <td><code>&lt;input type="checkbox" value="A" v-model="rv"&gt;</code>(RV[List[String]])</td>
      </tr>
      <tr>
        <th>CheckboxGeneralComponent</th>
        <td><code>&lt;input type="checkbox" :checked="..." @change="..."&gt;</code></td>
      </tr>
      <tr>
        <th>RadioStringComponent<br>RadioGeneralComponent</th>
        <td><code>&lt;input type="radio" value="A" v-model="rv"&gt;</code>(RV[String]・RV[Int]・RV[Double])</td>
      </tr>
      <tr>
        <th>SelectStringComponent<br>SelectGeneralComponent</th>
        <td>
          <code>&lt;select v-model="rv"&gt;</code>(RV[String]・RV[Int]・RV[Double]).<br>
          選択肢が変わる場合は、<code>&lt;option&gt;</code> を <code>v-for</code> で描画します.
        </td>
      </tr>
    </tbody>
  </table>

  <p>
    従来の書き方は、<a @click='go("/scalaApi")'>Scala API(従来の書き方)</a> を参照ください.
  </p>
</template>
