<script lang="scala">
import Router.*
import io.github.ondeoma.scalactive.doc.components.contents.scalaapi.quickstart.Hello

val code = // language=scala
  s"""package io.github.ondeoma.scalactive.doc.components.contents.scalaapi.quickstart
     |
     |import io.github.ondeoma.scalactive.components.*
     |import io.github.ondeoma.scalactive.reactive.*
     |import Hello.State
     |
     |object Hello extends StatefulComponent[State] {
     |
     |  case class State(count: RV[Int] = RV(0))
     |
     |  def initState() = State()
     |
     |  def genHtml(s: State)
     |             (implicit cm: ComponentManager): HTML = {
     |    // language=html
     |    s\"\"\"<p>Hello, Scalactive!</p>
     |       |<button $${evClick(s.count := s.count.v + 1)}>count: $${%(TextComponent(s.count)(_.toString))}</button>
     |       |\"\"\".stripMargin
     |  }
     |}
     |""".stripMargin
</script>

<template>
  <h2 id="start">クイックスタート</h2>

  <p>
    <a @click='go("/start")'>クイックスタート</a>のコンポーネントを、<br>
    .stを使わずにScalaで書くと以下のようになります.<br>
    .scalaファイルとして <code>src/main/scala</code> 配下に置きます.
  </p>

  <pre><code class="language-scala">{{ code }}</code></pre>

  <p>
    状態を持たない場合は、StatelessComponentを継承し、<br>
    <code>genHtml(implicit cm: ComponentManager)</code> を実装します.<br>
    呼び出し方(mainメソッドでの初期化)は.stの場合と同じです.
  </p>

  <div class="rendered">
    <Hello />
  </div>
</template>
