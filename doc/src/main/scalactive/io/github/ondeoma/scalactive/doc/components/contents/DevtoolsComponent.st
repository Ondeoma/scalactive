<script lang="scala">
import Router.*
import io.github.ondeoma.scalactive.doc.components.contents.devtools.Counter

val codeCounter = // language=html
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
    |    <p v-if="s.count.mapC(_ >= 3)" class="many">3以上です!</p>
    |  </div>
    |</template>
    |""".stripMargin

val codeTest = // language=scala
  """import io.github.ondeoma.scalactive.testing.TestKit
    |import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
    |
    |class CounterSuite extends munit.FunSuite {
    |
    |  // 描き直しを待たない・スクロールしない(jsdom向け)設定にする
    |  override def beforeAll(): Unit = TestKit.useTestConfig()
    |  override def afterAll(): Unit = TestKit.restoreConfig()
    |
    |  test("ボタンを押すと増える") {
    |    // .stのコンポーネントも Counter()(_, _) で渡せる
    |    val m = TestKit.mount(Counter()(_, _))
    |    TestKit.click(m.find("button"))
    |    // テキストのバインディングはすぐに反映される
    |    assertEquals(m.find("p").textContent, "count: 1")
    |    TestKit.click(m.find("button"))
    |    TestKit.click(m.find("button"))
    |    // v-if・v-forなどの描き直しを待つ
    |    TestKit.flush().map { _ =>
    |      assertEquals(m.find(".many").textContent, "3以上です!")
    |      m.unmount()
    |    }
    |  }
    |
    |}
    |""".stripMargin

val codeSbt = // language=scala
  s"""// build.sbt: jsdom上でテストを実行する(事前に npm install jsdom)
     |libraryDependencies += "org.scalameta" %%% "munit" % "<version>" % Test
     |Test / jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv()
     |
     |// project/plugins.sbt
     |libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0"
     |""".stripMargin

val codeDevtools = // language=scala
  s"""import io.github.ondeoma.scalactive.Devtools
     |
     |// ブラウザのコンソールから scalactive.stats() などで使えるようにする
     |Devtools.install()
     |
     |// 描き直しのたびに、対象と所要時間をコンソールに出力する
     |Devtools.logReloads = true
     |// 描き直した部分を一瞬枠で囲む
     |Devtools.highlightUpdates = true
     |
     |// 表示中のコンポーネントの数と、描き直しの回数
     |Devtools.stats  // Stats(liveComponents = 42, reloads = 10)
     |// リアクティブな値を監視している数
     |Devtools.watcherCount(rv)
     |""".stripMargin
</script>

<template>
  <h1>テスト・デバッグ</h1>

  <ul class="toc">
    <li><a @click='setHash("Test")'>コンポーネントのテスト(TestKit)</a></li>
    <li><a @click='setHash("Devtools")'>診断機能(Devtools)</a></li>
  </ul>

  <h2 id="Test">コンポーネントのテスト(TestKit)</h2>

  <p>
    <code>TestKit</code> は、コンポーネントを描画して操作・確認するためのヘルパーです.<br>
    テストフレームワークには依存しないので、munitなどと組み合わせて使います.<br>
    jsdom(scalajs-env-jsdom-nodejs)上や、ブラウザ上のテストで動きます.
  </p>

  <p>
    例として、次の.stのコンポーネントをテストします.
  </p>

  <div class="code-file">Counter.st</div>
  <pre><code class="language-st">{{ codeCounter }}</code></pre>

  <div class="rendered">
    <Counter />
  </div>

  <p>
    .stのコンポーネントは、<code>apply()</code> を持つobjectになります.<br>
    そのため、<code>TestKit.mount(Counter()(_, _))</code> のように渡せます.<br>
    (propsのあるコンポーネントは <code>Badge(label = "x")(_, _)</code>)
  </p>

  <div class="code-file">CounterSuite.scala</div>
  <pre><code class="language-scala">{{ codeTest }}</code></pre>

  <ul>
    <li>
      <code>mount(コンポーネント)</code> / <code>mountHtml(cm =&gt; HTML)</code>: document.bodyの下に描画します.<br>
      戻り値の <code>find</code> / <code>findAll</code> / <code>text</code> で確認し、<code>unmount()</code> で片付けます.
    </li>
    <li><code>click</code> / <code>input</code> / <code>check</code> / <code>keydown</code>: 要素を操作します(v-modelやイベントが反応します).</li>
    <li>
      <code>flush()</code>: 予約された描き直し(v-if・v-forなど)とmountedが終わるまで待ちます.<br>
      テキストや属性のバインディングは待たずに反映されます.
    </li>
    <li>
      <code>useTestConfig()</code> / <code>restoreConfig()</code>: 描き直しを待たず、スクロールしない設定にします<br>
      (jsdomはwindow.scrollToを実装していないため).
    </li>
  </ul>

  <pre><code class="language-scala">{{ codeSbt }}</code></pre>

  <h2 id="Devtools">診断機能(Devtools)</h2>

  <p>
    開発中に、描き直しの様子や解除漏れを確かめるための機能です.<br>
    このドキュメントサイトでも有効にしてあるので、<br>
    ブラウザのコンソールで <code>scalactive.highlightUpdates(true)</code> と入力してから、<br>
    画面を操作してみてください.
  </p>

  <pre><code class="language-scala">{{ codeDevtools }}</code></pre>

  <ul>
    <li>
      <code>logReloads</code>: v-if・v-for・リアクティブな値の変化などによる描き直しのたびに、<br>
      対象と所要時間をコンソールに出力します.
    </li>
    <li>
      <code>highlightUpdates</code>: 描き直した部分を一瞬枠で囲みます.<br>
      思ったより広い範囲が描き直されていないかを、目で確かめられます.
    </li>
    <li>
      <code>stats</code>: 表示中のコンポーネントの数と描き直しの回数です.<br>
      画面を行き来しても表示中の数が増え続ける場合は、<br>
      片付けの漏れ(メモリリーク)の疑いがあります.
    </li>
    <li>
      <code>watcherCount(rv)</code>: リアクティブな値を監視している数です.<br>
      片付けた後も減らない場合は、監視の解除漏れの疑いがあります.
    </li>
  </ul>

  <p>
    ※状態を保ったままコンポーネントを差し替える、<br>
    HMR(Hot Module Replacement)には対応していません.<br>
    Viteの開発サーバーでは、Scala.jsの再ビルドのたびにページ全体が読み込み直されます.
  </p>
</template>
