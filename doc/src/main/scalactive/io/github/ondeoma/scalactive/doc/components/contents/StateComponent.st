<script lang="scala">
import io.github.ondeoma.scalactive.doc.components.contents.state.*

val storeCode = // language=scala
  """package io.github.ondeoma.scalactive.doc.components.contents.state
     |
     |import io.github.ondeoma.scalactive.reactive.*
     |
     |// 複数のコンポーネントで共有する状態
     |object CountStore {
     |
     |  val count: RV[Int] = RV(0)
     |
     |  def increment(): Unit = count := count.v + 1
     |
     |}
     |""".stripMargin

val viewCode = // language=html
  """<template>
     |  <p>count: {{ CountStore.count }}</p>
     |</template>
     |""".stripMargin

val buttonCode = // language=html
  """<template>
     |  <button @click="CountStore.increment()">+1</button>
     |</template>
     |""".stripMargin
</script>

<template>
  <h1>状態管理</h1>

  <p>
    状態管理に特化した専用機能はありません.<br>
    任意のobjectにReactive系の値を定義して、<br>
    各コンポーネントから参照する方法を検討ください.
  </p>

  <p>
    以下は、objectに定義したRVを2つの.stで共有する例です.<br>
    objectの値はコンポーネントの外で作られるので、<br>
    コンポーネントが片付けられても残ります.
  </p>

  <div class="code-file">CountStore.scala</div>
  <pre><code class="language-scala">{{ storeCode }}</code></pre>

  <div class="code-file">CountView.st</div>
  <pre><code class="language-st">{{ viewCode }}</code></pre>

  <div class="code-file">CountButton.st</div>
  <pre><code class="language-st">{{ buttonCode }}</code></pre>

  <div class="rendered">
    <CountView />
    <CountButton />
    <CountView />
  </div>

  <p>
    どちらのCountViewも、同じRVを表示しています.<br>
    ボタンを押すと、両方が更新されます.
  </p>
</template>
