package io.github.ondeoma.scalactive.doc.components.contents

import io.github.ondeoma.scalactive.components.*
import io.github.ondeoma.scalactive.doc.utils.Utility
import io.github.ondeoma.scalactive.doc.utils.Utility.esc
import org.scalajs.dom.*

object AttrBindingComponent extends StatelessComponent {

  def genHtml(implicit cm: ComponentManager): HTML = {

    import Router.*
    import Utility.esc

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
          
    // language=html
    s"""<h1>属性バインディング</h1>
       |
       |<p>
       |  HTML属性にリアクティブ値を設定できます.<br>
       |  class, styleは個別に最適化した実装が存在します.
       |</p>
       |
       |<ul class="toc">
       |  <li><a ${evClick(setHash("Class"))}>Classバインディング</a></li>
       |  <li><a ${evClick(setHash("Style"))}>Styleバインディング</a></li>
       |  <li><a ${evClick(setHash("Attr"))}>Attrバインディング</a></li>
       |</ul>
       |
       |<h2 id="Class">Classバインディング</h2>
       |
       |<h3>clsIf</h3>
       |
       |<pre><code class="language-scala">${esc(codeClsIf)}</code></pre>
       |
       |<h3>clsS</h3>
       |
       |<pre><code class="language-scala">${esc(codeClsS)}</code></pre>
       |
       |<h3>clsSs</h3>
       |
       |<pre><code class="language-scala">${esc(codeClsSs)}</code></pre>
       | 
       |<h3>clsTgl</h3>
       |
       |<pre><code class="language-scala">${esc(codeClsTgl)}</code></pre>
       |  
       |  
       |<h2 id="Style">Styleバインディング</h2>  
       |  
       |<h3>styleIf</h3>
       |
       |<pre><code class="language-scala">${esc(codeStyleIf)}</code></pre>
       |
       |<h3>styleS</h3>
       |
       |<pre><code class="language-scala">${esc(codeStyleS)}</code></pre>
       |
       |<h3>styleTgl</h3>
       |
       |<pre><code class="language-scala">${esc(codeStyleTgl)}</code></pre>  
       |
       |
       |<h2 id="Attr">Attrバインディング</h2>  
       |  
       |<h3>attrIf</h3>
       |
       |<pre><code class="language-scala">${esc(codeAttrIf)}</code></pre>
       |
       |<h3>attrS</h3>
       |
       |<pre><code class="language-scala">${esc(codeAttrS)}</code></pre>
       |
       |""".stripMargin
  }
  
}
