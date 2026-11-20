<script lang="scala">
import Router.*
import io.github.ondeoma.scalactive.doc.components.contents.effects.*

val codeListeners = // language=scala
  """def mounted(s: State): Unit = {
    |  // Ctrl+K で検索欄へ
    |  useEventListener[KeyboardEvent](window, "keydown") { e =>
    |    if (e.key == "k" && (e.ctrlKey || e.metaKey)) { e.preventDefault(); s.searchOpen := true }
    |  }
    |  // 表示中のタブだけ、15秒ごとに読み直す(戻ってきたときはすぐ読み直す)
    |  useVisiblePolling(15000)(reload(s))
    |  useInterval(1000)(s.now := new js.Date().toLocaleTimeString())
    |  useTimeout(5000)(s.hint := false)
    |}
    |""".stripMargin

val codeDebounce = // language=scala
  """def mounted(s: State): Unit = {
    |  val search = useDebounce[String](250)(q => s.result.reload())
    |  watch(s.q)((_, q) => search(q))
    |}
    |
    |val onScroll = useThrottle[Event](100)(_ => saveScrollPosition())
    |
    |// 後から始めた検索があれば、前の結果は捨てる
    |val latest = useLatest()
    |latest(Api.search(q)).foreach(r => s.items := r)
    |""".stripMargin

val codeAsync = // language=html
  """<script lang="scala">
    |case class State(q: RV[String],
    |                 result: AsyncState[List[String]],
    |                 save: ActionState = useAction())
    |
    |def initState() = {
    |  val q = RV("")
    |  // reload() のたびに、その時の q で読み込む
    |  State(q, useAsync(Api.search(q.v)))
    |}
    |
    |def save(s: State): Unit = s.save.run(Api.save(...)).foreach(_ => toast("保存しました"))
    |</script>
    |
    |<template>
    |  <p v-if="s.result.loading">読み込み中…</p>
    |  <p v-if="s.result.failed" class="error">{{ s.result.errorMessage }}</p>
    |  <li v-for="f in s.result.getOrElse(Nil)">{{ f }}</li>
    |
    |  <button :disabled="s.save.busy" @click="save(s)">保存</button>
    |  <p v-if="s.save.failed" class="error">{{ s.save.errorMessage }}</p>
    |</template>
    |""".stripMargin
</script>

<template>
  <h1>副作用・非同期</h1>

  <p>
    イベントリスナー・タイマー・非同期の読み込みを、後片付けの手間なく書くための関数です.<br>
    どれもコンポーネントの中(描画中・<code>def mounted</code>・onMounted)で作った場合は、<br>
    コンポーネントが片付けられたときに自動で止まります.<br>
    それ以外の場所(<code>initState</code>・objectなど)で作った場合は、戻り値の <code>stop()</code> で止めてください.
  </p>

  <h2 id="Listeners">イベントリスナー・タイマー</h2>

  <table>
    <tr><th>関数</th><th>内容</th></tr>
    <tr><td><code>useEventListener[E](target, type, capture, passive, once)(f)</code></td><td>
      window・documentなど、コンポーネントの外の要素にもイベントリスナーを登録します.
    </td></tr>
    <tr><td><code>useInterval(ms)(f)</code></td><td>msミリ秒ごとにfを実行します.</td></tr>
    <tr><td><code>useTimeout(ms)(f)</code></td><td>msミリ秒後にfを1回実行します.</td></tr>
    <tr><td><code>useVisiblePolling(ms, immediate)(f)</code></td><td>
      タブが表示されている間だけ、msミリ秒ごとにfを実行します.<br>
      隠れている間は止め、再び表示されたときにすぐ1回実行してから再開します.
    </td></tr>
  </table>

  <pre><code class="language-scala">{{ codeListeners }}</code></pre>

  <h2 id="Debounce">デバウンス・スロットル</h2>

  <table>
    <tr><th>関数</th><th>内容</th></tr>
    <tr><td><code>useDebounce[A](ms)(f)</code></td><td>
      最後の呼び出しからmsミリ秒たってからfを実行します(入力中の検索など).<br>
      <code>flush()</code> ですぐ実行、<code>cancel()</code> で取り消します.
    </td></tr>
    <tr><td><code>useThrottle[A](ms)(f)</code></td><td>
      fを、msミリ秒に1回までに間引いて実行します(スクロール・リサイズなど).<br>
      最初の呼び出しはすぐに、間引いた間の最後の呼び出しは間隔があいた後に実行します.
    </td></tr>
    <tr><td><code>useLatest()</code></td><td>
      最後に始めた非同期の処理の結果だけを受け取ります(古い応答を捨てる).
    </td></tr>
  </table>

  <pre><code class="language-scala">{{ codeDebounce }}</code></pre>

  <h2 id="Async">非同期の状態</h2>

  <table>
    <tr><th>関数</th><th>内容</th></tr>
    <tr><td><code>useAsync(load, immediate)</code></td><td>
      非同期に読み込む値の状態 <code>AsyncState[A]</code> を作ります.<br>
      <code>data</code>(値)・<code>loading</code>・<code>error</code>・<code>failed</code>・<code>errorMessage</code>・<code>ready</code> を持ち、
      <code>reload()</code> で読み直します.<br>
      読み直し中も前の値のままで、後から読み直した場合は古い応答を捨てます.<br>
      <code>set(a)</code>・<code>update(f)</code> で値を直接書き換えられます(保存した結果の反映など).
    </td></tr>
    <tr><td><code>useAction()</code></td><td>
      実行中は二重に実行できない操作(保存・送信など)の状態 <code>ActionState</code> を作ります.<br>
      <code>run(future)</code> で実行し、実行中は <code>busy</code> が真になります.<br>
      実行中にもう一度呼ぶと、実行せずに <code>ActionState.Busy</code> で失敗します.
      失敗は <code>error</code>・<code>failed</code>・<code>errorMessage</code> に入ります.
    </td></tr>
  </table>

  <pre><code class="language-st">{{ codeAsync }}</code></pre>

  <div class="rendered">
    <EffectsDemo />
  </div>
</template>
