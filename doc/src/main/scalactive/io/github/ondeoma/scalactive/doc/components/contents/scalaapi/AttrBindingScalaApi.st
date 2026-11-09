<script lang="scala">
val codeClsIf = // language=scala
  s"""// rvBがtrueの時に"clsA"が割当.
     |val rvB = RV(true)
     |s\"\"\"<p $${clsIf(rvB, "clsA")}></p>\"\"\"
     |""".stripMargin

val codeClsS = // language=scala
  s"""// rvClsの値がclassとして割当.
     |val rvCls = RV("clsA")
     |s\"\"\"<p $${clsS(rvCls)}></p>\"\"\"
     |""".stripMargin

val codeClsSs = // language=scala
  s"""// rvClssの値がclassとして割当.
     |val rvClss = RVList("clsA", "clsB")
     |s\"\"\"<p $${clsSs(rvClss)}></p>\"\"\"
     |""".stripMargin

val codeClsTgl = // language=scala
  s"""// rvBの値がtrueの場合は第2引数, falseの場合は第3引数を割当.
     |val rvB = RV(true)
     |s\"\"\"<p $${clsTgl(rvB, "clsA", "clsB")}></p>\"\"\"
     |// リスト版もあります.
     |s\"\"\"<p $${clsTgl(rvB, List("clsA1", "clsA2"), List("clsB1", "clsB2"))}></p>\"\"\"
     |""".stripMargin

val codeStyleIf = // language=scala
  s"""// rvBがtrueの時に"color"に"red"を割当.
     |val rvB = RV(true)
     |s\"\"\"<p $${styleIf(rvB, "color", "red")}></p>\"\"\"
     |""".stripMargin

val codeStyleS = // language=scala
  s"""// rvStyleの値がstyleのcolorとして割当.
     |val rvStyle = RV("red")
     |s\"\"\"<p $${styleS("color", rvStyle)}></p>\"\"\"
     |""".stripMargin

val codeStyleTgl = // language=scala
  s"""// rvBの値がtrueの場合は第3引数, falseの場合は第4引数をcolorに割当.
     |val rvB = RV(true)
     |s\"\"\"<p $${styleTgl(rvB, "color", "red", "blue")}></p>\"\"\"
     |""".stripMargin

val codeAttrIf = // language=scala
  s"""// rvBがtrueの時に"disabled"を割当.
     |val rvB = RV(true)
     |s\"\"\"<p $${attrIf(rvB, "disabled")}></p>\"\"\"
     |""".stripMargin

val codeAttrS = // language=scala
  s"""// rvVの値がdata-some値として割当.
     |val rvV = RV("dataA")
     |s\"\"\"<p $${attrS("data-some", rvV)}></p>\"\"\"
     |""".stripMargin
</script>

<template>
  <h2 id="attr">属性バインディング</h2>

  <p>
    HTML属性にリアクティブ値を設定できます.<br>
    class, styleは個別に最適化した実装が存在します.
  </p>

  <h3 id="attr-class">Classバインディング</h3>

  <h4>clsIf</h4>

  <pre><code class="language-scala">{{ codeClsIf }}</code></pre>

  <h4>clsS</h4>

  <pre><code class="language-scala">{{ codeClsS }}</code></pre>

  <h4>clsSs</h4>

  <pre><code class="language-scala">{{ codeClsSs }}</code></pre>

  <h4>clsTgl</h4>

  <pre><code class="language-scala">{{ codeClsTgl }}</code></pre>

  <h3 id="attr-style">Styleバインディング</h3>

  <h4>styleIf</h4>

  <pre><code class="language-scala">{{ codeStyleIf }}</code></pre>

  <h4>styleS</h4>

  <pre><code class="language-scala">{{ codeStyleS }}</code></pre>

  <h4>styleTgl</h4>

  <pre><code class="language-scala">{{ codeStyleTgl }}</code></pre>

  <h3 id="attr-attr">Attrバインディング</h3>

  <h4>attrIf</h4>

  <pre><code class="language-scala">{{ codeAttrIf }}</code></pre>

  <h4>attrS</h4>

  <pre><code class="language-scala">{{ codeAttrS }}</code></pre>
</template>
