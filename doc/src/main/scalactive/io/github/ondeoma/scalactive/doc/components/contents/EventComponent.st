<script lang="scala">
import Router.*
import io.github.ondeoma.scalactive.doc.components.contents.event.{ClickEvent, EventModifiers, KeyModifiers, ListenerOptions}

val codeClick = // language=html
  """<script lang="scala">
     |case class State(count: RV[Int] = RV(0),
     |                 pos: RV[String] = RV("-"))
     |
     |def initState() = State()
     |
     |def showPos(s: State, e: MouseEvent): Unit = {
     |  s.pos := s"${e.clientX.toInt}, ${e.clientY.toInt}"
     |}
     |</script>
     |
     |<template>
     |  <button @click="s.count := s.count.v + 1">クリック: {{ s.count }}</button>
     |  <button v-on:click="s.count := 0">リセット</button>
     |  <div class="area" @mousemove="showPos(s, event.asInstanceOf[MouseEvent])">
     |    マウスの位置: {{ s.pos }}
     |  </div>
     |</template>
     |
     |<style scoped>
     |.area { margin-top: 8px; padding: 16px; background: mediumpurple; color: white; }
     |</style>
     |""".stripMargin

val codeModifiers = // language=html
  """<script lang="scala">
     |case class State(name: RV[String] = RV(""),
     |                 log: RVList[String] = RVList(List[String]()))
     |
     |def initState() = State()
     |
     |def put(s: State, msg: String): Unit = {
     |  s.log.add(msg)
     |  if (s.log.v.size > 3) s.log.rm(0)
     |}
     |</script>
     |
     |<template>
     |  <!-- .prevent: 送信でページが移動する(既定の動作)のを止めます -->
     |  <form @submit.prevent='put(s, "送信: " + s.name.v)'>
     |    <input type="text" v-model="s.name">
     |    <button type="submit">送信</button>
     |  </form>
     |  <!-- .stop: 親要素へ伝わるのを止めます -->
     |  <div class="box" @click='put(s, "外側のdiv")'>
     |    <button @click='put(s, "伝わるボタン")'>伝わる</button>
     |    <button @click.stop='put(s, "止めるボタン")'>止める</button>
     |  </div>
     |  <!-- .self: 子要素ではなく、その要素自体で起きた時だけ実行します -->
     |  <div class="box" @click.self='put(s, "余白(.self)")'>
     |    <button>子要素のボタン</button> 余白をクリック
     |  </div>
     |  <ul>
     |    <li v-for="l in s.log">{{ l }}</li>
     |  </ul>
     |</template>
     |
     |<style scoped>
     |.box { margin: 8px 0; padding: 8px; border: 1px dashed #999; }
     |</style>
     |""".stripMargin

val codeKeys = // language=html
  """<script lang="scala">
     |case class State(input: RV[String] = RV(""),
     |                 msg: RV[String] = RV("-"))
     |
     |def initState() = State()
     |</script>
     |
     |<template>
     |  <input type="text" v-model="s.input" placeholder="Enter / Ctrl+Enter / Esc" @keydown.enter='s.msg := "Enter: " + s.input.v' @keydown.enter.ctrl='s.msg := "Ctrl+Enter: " + s.input.v' @keydown.esc='s.input := ""'>
     |  <p>{{ s.msg }}</p>
     |</template>
     |""".stripMargin

val codeOptions = // language=html
  """<script lang="scala">
     |case class State(once: RV[Int] = RV(0),
     |                 order: RV[String] = RV("-"),
     |                 wheel: RV[Int] = RV(0))
     |
     |def initState() = State()
     |</script>
     |
     |<template>
     |  <!-- .once: 最初の1回だけ実行します -->
     |  <button @click.once="s.once := s.once.v + 1">1回だけ数えます: {{ s.once }}</button>
     |  <!-- .capture: 子要素より先に実行します -->
     |  <div class="box" @click.capture='s.order := "div"'>
     |    <button @click='s.order := s.order.v + " → button"'>実行順を確認</button>
     |    {{ s.order }}
     |  </div>
     |  <!-- .passive: スクロールを妨げません -->
     |  <div class="box" @wheel.passive="s.wheel := s.wheel.v + 1">
     |    ここでホイールを回してください: {{ s.wheel }}
     |  </div>
     |</template>
     |
     |<style scoped>
     |.box { margin: 8px 0; padding: 8px; border: 1px dashed #999; }
     |</style>
     |""".stripMargin
</script>

<template>
  <h1>イベントハンドリング</h1>

  <p>
    要素で発生したイベント(クリック等)を検知して処理を行う方法です.
  </p>

  <ul class="toc">
    <li><a @click='setHash("on")'>@イベント名</a></li>
    <li><a @click='setHash("modifiers")'>修飾子</a></li>
    <li><a @click='setHash("keys")'>キー修飾子</a></li>
    <li><a @click='setHash("options")'>リスナーのオプション</a></li>
  </ul>

  <h2 id="on">@イベント名</h2>

  <p>
    <code>@click="文"</code> のように、<code>@</code> の後にイベント名を書きます.<br>
    <code>v-on:click="文"</code> と書いても同じです.<br>
    値にはScalaの文を書きます.<br>
    <code>;</code> で区切って複数の文も書けます.<br>
    文の中では、<code>event</code> で
    <a href="https://github.com/scala-js/scala-js-dom/blob/main/dom/src/main/scala/org/scalajs/dom/Event.scala" target="_blank">Event</a>
    を参照できます.<br>
    長い処理は <code>&lt;script&gt;</code> に関数を定義して呼び出すと読みやすいです.
  </p>

  <pre><code class="language-st">{{ codeClick }}</code></pre>

  <div class="rendered">
    <ClickEvent />
  </div>

  <p>
    子コンポーネントのタグに書いた <code>@イベント名</code> は、子への関数のpropsになります.<br>
    詳しくは<a @click='go("/component")'>コンポーネント</a>のページを参照してください.
  </p>

  <h2 id="modifiers">修飾子</h2>

  <p>
    イベント名の後に <code>.修飾子</code> を付けると、<br>
    よくある処理を短く書けます.
  </p>

  <ul>
    <li><code>.prevent</code> : <code>event.preventDefault()</code> を呼びます.</li>
    <li><code>.stop</code> : <code>event.stopPropagation()</code> を呼びます.</li>
    <li><code>.self</code> : 子要素ではなく、その要素自体で起きた時だけ実行します.</li>
  </ul>

  <pre><code class="language-st">{{ codeModifiers }}</code></pre>

  <div class="rendered">
    <EventModifiers />
  </div>

  <h2 id="keys">キー修飾子</h2>

  <p>
    キーボードのイベントでは、キーを修飾子で指定できます.<br>
    指定したキーの時だけ実行します.
  </p>

  <ul>
    <li>
      キー: <code>.enter</code> / <code>.esc</code> / <code>.tab</code> / <code>.space</code> / <code>.delete</code>、<br>
      <code>.up</code> / <code>.down</code> / <code>.left</code> / <code>.right</code><br>
      (<code>.delete</code> はDeleteとBackspaceの両方です)
    </li>
    <li>
      修飾キー: <code>.ctrl</code> / <code>.shift</code> / <code>.alt</code> / <code>.meta</code><br>
      (修飾キーを押している時だけ実行します. クリックなどにも使えます)
    </li>
  </ul>

  <p>
    修飾キーを指定しない場合は、修飾キーを押していても実行します.<br>
    以下の例でCtrl+Enterを押すと、<br>
    <code>@keydown.enter</code> と <code>@keydown.enter.ctrl</code> の両方が実行されます.
  </p>

  <pre><code class="language-st">{{ codeKeys }}</code></pre>

  <div class="rendered">
    <KeyModifiers />
  </div>

  <h2 id="options">リスナーのオプション</h2>

  <p>
    以下の修飾子は、addEventListenerのオプションになります.
  </p>

  <ul>
    <li><code>.once</code> : 条件を満たした最初の1回だけ実行します.</li>
    <li><code>.capture</code> : 子要素より先に、キャプチャフェーズで実行します.</li>
    <li>
      <code>.passive</code> : スクロールなどを妨げません.<br>
      preventDefaultできなくなるので、<code>.prevent</code> とは併用できません.
    </li>
  </ul>

  <pre><code class="language-st">{{ codeOptions }}</code></pre>

  <div class="rendered">
    <ListenerOptions />
  </div>
</template>
