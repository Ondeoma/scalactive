<script lang="scala">
val codeUse = // language=scala
  s"""// 生成されたobject app.Counter は通常のコンポーネントとして使えます.
     |s\"\"\"<div>$${c(Counter())}</div>\"\"\"
     |""".stripMargin
</script>

<template>
  <h2 id="sfc">単一ファイルコンポーネントとScala API</h2>

  <p>
    .stから生成されたobjectは、通常のコンポーネントです.<br>
    Scalaで書いたコンポーネントからも呼び出せます.
  </p>

  <pre><code class="language-scala">{{ codeUse }}</code></pre>

  <ul>
    <li>
      propsを受け取るコンポーネントをScalaで書く場合は、<br>
      <code>StatelessPropsComponent[P]</code> / <code>StatefulPropsComponent[P, S]</code> を継承します.
    </li>
    <li>
      スコープ付きスロットをScalaで書く場合は、<br>
      親は <code>ScopedSlot { x =&gt; cm =&gt; ... }</code> を渡し、<br>
      子は <code>props.name(value)()</code> で展開します.
    </li>
    <li>
      ライフサイクルフックをScalaで書く場合は、<br>
      genHtmlの中で <code>onMounted {...}</code> / <code>onUnmounted {...}</code> を呼びます.
    </li>
  </ul>
</template>
