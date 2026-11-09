<script lang="scala">
import Router.*
import io.github.ondeoma.scalactive.doc.components.contents.cond.{IfElse, ShowToggle, TemplateIf}

val codeIf = // language=html
  """<script lang="scala">
     |case class State(score: RV[Int] = RV(70))
     |
     |def initState() = State()
     |</script>
     |
     |<template>
     |  <p><input type="number" v-model="s.score" style="width: 5em">点</p>
     |  <p v-if="s.score.mapC(_ >= 80)">合格です!</p>
     |  <p v-else-if="s.score.mapC(_ >= 60)">あと少しです.</p>
     |  <p v-else>不合格です.</p>
     |</template>
     |""".stripMargin

val codeTemplateIf = // language=html
  """<script lang="scala">
     |case class State(loggedIn: RV[Boolean] = RV(false))
     |
     |def initState() = State()
     |</script>
     |
     |<template>
     |  <button @click="s.loggedIn := !s.loggedIn.v">ログイン状態を切り替え</button>
     |  <template v-if="s.loggedIn">
     |    <p><b>ようこそ!</b></p>
     |    <p>この2つの要素がまとめて表示されます.</p>
     |  </template>
     |  <p v-else>ログインしていません.</p>
     |</template>
     |""".stripMargin

val codeShow = // language=html
  """<script lang="scala">
     |case class State(open: RV[Boolean] = RV(true))
     |
     |def initState() = State()
     |</script>
     |
     |<template>
     |  <label><input type="checkbox" v-model="s.open"> 表示する</label>
     |  <p v-show="s.open">v-showで切り替えています(非表示の間もDOMは残ります).</p>
     |  <!-- display: flex で表示したい場合 -->
     |  <div class="row" :style='"display" -> s.open.mapC(o => if (o) "flex" else "none")'>
     |    <span>flexの</span><span>要素です.</span>
     |  </div>
     |</template>
     |
     |<style scoped>
     |.row { gap: 8px; }
     |</style>
     |""".stripMargin
</script>

<template>
  <h1>条件付きレンダリング</h1>

  <p>
    リアクティブな真偽値に合わせて表示有無を切り替える方法です.
  </p>

  <ul class="toc">
    <li><a @click='setHash("vIf")'>v-if / v-else-if / v-else</a></li>
    <li><a @click='setHash("templateIf")'>&lt;template v-if&gt;</a></li>
    <li><a @click='setHash("vShow")'>v-show</a></li>
  </ul>

  <h2 id="vIf">v-if / v-else-if / v-else</h2>

  <p>
    vue.jsにおけるv-ifと同じです.<br>
    条件が真の時だけ要素を描画します.<br>
    非表示状態になっている時はDOMが存在しなくなります.<br>
    条件には <code>Reactive[Boolean]</code> または <code>Boolean</code> が使えます.<br>
    Reactiveの場合は、値の変化に追従して描き直します.
  </p>

  <pre><code class="language-st">{{ codeIf }}</code></pre>

  <div class="rendered">
    <IfElse />
  </div>

  <ul>
    <li>
      <code>v-else-if</code> / <code>v-else</code> は、<br>
      直前の <code>v-if</code> / <code>v-else-if</code> の要素に続けて書きます.
    </li>
    <li>
      各条件は1度だけ評価されます.<br>
      <code>s.score.mapC(_ &gt;= 80)</code> のような条件は、<br>
      scoreが変わるたびではなく、真偽が切り替わった時だけ描き直されます.
    </li>
  </ul>

  <h2 id="templateIf">&lt;template v-if&gt;</h2>

  <p>
    複数の要素をまとめて切り替える場合は、<code>&lt;template&gt;</code> で囲みます.<br>
    <code>&lt;template&gt;</code> 自体は出力されず、中身だけが描画されます.
  </p>

  <pre><code class="language-st">{{ codeTemplateIf }}</code></pre>

  <div class="rendered">
    <TemplateIf />
  </div>

  <h2 id="vShow">v-show</h2>

  <p>
    vue.jsにおけるv-showに相当するものになります.<br>
    非表示状態になっている時もDOMは存在します.<br>
    ※displayスタイルがnoneとなっています.<br>
    要素を作り直さないので、頻繁に切り替える場合に向いています.
  </p>

  <p>
    表示状態ではdisplayスタイルにblockが設定されます.<br>
    flexなど別の値にしたい場合は、<br>
    v-showの代わりに <code>:style</code> でdisplayを指定してください.
  </p>

  <pre><code class="language-st">{{ codeShow }}</code></pre>

  <div class="rendered">
    <ShowToggle />
  </div>
</template>
