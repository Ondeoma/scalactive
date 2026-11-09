<script lang="scala">
val codeWatch = // language=scala
  s"""object ClockComponent extends StatelessComponent {
     |  def genHtml(implicit cm: ComponentManager): HTML = {
     |    // コンポーネントの外のリアクティブな値(Router.hrefなど)を監視する
     |    val watchInfo = Router.href.addWatcher {
     |      case (oldV, newV) => println(s"href: $$oldV -> $$newV")
     |    }
     |    // 片付けるときに解除する
     |    onUnmounted(watchInfo.abort())
     |
     |    s\"\"\"<p>...</p>\"\"\"
     |  }
     |}
     |""".stripMargin
</script>

<template>
  <h2 id="reactive">リアクティブ</h2>

  <h3 id="reactive-watch">genHtmlの中での監視</h3>

  <p>
    コンポーネントの外のリアクティブな値(グローバルな状態やRouter.hrefなど)を、<br>
    genHtmlの中で監視する場合は、<br>
    <code>onUnmounted(watchInfo.abort())</code> で片付けるときに解除してください.<br>
    解除しないと、描画のたびに監視が増えていきます.
  </p>

  <pre><code class="language-scala">{{ codeWatch }}</code></pre>
</template>
