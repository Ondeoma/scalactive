<script lang="scala">
import Router.*
import io.github.ondeoma.scalactive.doc.components.contents.introduction.{SampleComponent, SampleComponentCode}
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
  </p>

  <h2>どんな感じ？</h2>

  <p>
    基本的には<a @click='go("/component")'>コンポーネント</a>を組合わせる実装スタイルとなります.<br>
    以下実装例です.
  </p>

  <pre><code class="language-scala">{{ SampleComponentCode.code }}</code></pre>

  <p>
    これを呼び出すと以下のように表示されます.
  </p>

  <div class="rendered">
    <SampleComponent />
  </div>
</template>
