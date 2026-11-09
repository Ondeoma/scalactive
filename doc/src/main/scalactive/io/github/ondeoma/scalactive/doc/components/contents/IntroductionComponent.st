<script lang="scala">
import Router.*
import io.github.ondeoma.scalactive.doc.components.contents.introduction.IntroSample

// language=html
val code =
  """|<script lang="scala">
     |// 状態はReactiveな値(RV)で持たせます.
     |case class State(txt: RV[String] = RV(""))
     |
     |// initStateでは状態を初期化する処理を定義します。
     |// 生成した値はテンプレート内で標準で"s"で参照できます。
     |def initState() = State()
     |</script>
     |
     |<template>
     |  <!-- HTMLをそのまま書くような形です. -->
     |  <table>
     |    <tbody>
     |      <tr>
     |        <th>入力</th>
     |        <td><input type="text" v-model="s.txt"></td>
     |      </tr>
     |      <tr>
     |        <th>状態</th>
     |        <td>{{ s.txt }}</td>
     |      </tr>
     |      <tr>
     |        <th>イベント</th>
     |        <td><button @click='window.alert(s"入力値は${s.txt.v}です。")'>ボタン</button></td>
     |      </tr>
     |    </tbody>
     |  </table>
     |</template>
     |""".stripMargin
</script>

<template>
  <h1>はじめに</h1>
  <p>
    Scalactive(スカラクティブ)は主に<a href="https://ja.vuejs.org/">Vue.js</a>の仕様を参考にした<a href="https://www.scala-js.org/">Scala.js</a>のライブラリ？フレームワーク？です.<br>
    Scalaでフロントエンドを快適に実装したいという動機で開発を進めています.
  </p>
  <p>
    現在、技術検証段階のため、本番利用は避けていただくのが無難ですが、<br>
    使っていただき、ご意見賜れますと喜びます.
  </p>
  <p>
    ※このドキュメントはScalactiveで実装されています.
    <br>
    ※ver0.2以降はAIを用いて開発しています.
  </p>

  <h2>どんな感じ？</h2>

  <p>
    基本的には<a @click='go("/component")'>コンポーネント</a>を組合わせる実装スタイルとなります.<br>
    コンポーネントは、Vueの単一ファイルコンポーネントのように、<br>
    <code>.st</code> ファイルに、Scalaコードとテンプレートをまとめて書きます.<br>
    以下実装例です.
  </p>

  <pre><code class="language-st">{{ code }}</code></pre>

  <p>
    これを呼び出すと以下のように表示されます.
  </p>

  <div class="rendered">
    <IntroSample />
  </div>
</template>
