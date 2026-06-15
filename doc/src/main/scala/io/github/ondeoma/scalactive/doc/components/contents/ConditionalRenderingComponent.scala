package io.github.ondeoma.scalactive.doc.components.contents

import io.github.ondeoma.scalactive.components.*
import org.scalajs.dom.*

import scala.concurrent.duration.*

object ConditionalRenderingComponent extends StatelessComponent {

  def genHtml(implicit cm: ComponentManager): HTML = {

    import Router.*
    import io.github.ondeoma.scalactive.doc.utils.Utility.esc

    val codeIfC = // language=scala
      s"""// rvBがtrueの時に表示.
         |val rvB = RV(true)
         |val ifComponent = ifC(rvB) { implicit cm =>
         |  s\"\"\"<p>Hello ifC.</p>\"\"\".stripMargin
         |}
         |s\"\"\"<div>$${ifComponent}</div>\"\"\"
         |
         |// 以下のように直接書いても問題ありません.
         |s\"\"\"<div>$${ifC(rvB){ implicit cm => s\"\"\"<p>Hello ifC.</p>\"\"\" }}</div>\"\"\"
         |""".stripMargin

    val codeShowIf = // language=scala
      s"""// rvBがtrueの時に表示.
         |val rvB = RV(true)
         |s\"\"\"<p $${showIf(rvB)}>Hello showIf.</p>\"\"\"
         |""".stripMargin

    val codeShowIf2 = // language=scala
      s"""s\"\"\"<p $${showIf(rvB, "flex")}>This is flex.</p>\"\"\"
         |s\"\"\"<p $${showFlexIf(rvB)}>This is flex.</p>\"\"\"
         |s\"\"\"<p $${showGridIf(rvB)}>This is grid.</p>\"\"\"
         |""".stripMargin
    
    val codeShowIfBriefly = // language=scala
      s"""import scala.concurrent.duration.*
         |// 3秒後に非表示
         |s\"\"\"<p $${showIfBriefly(rvB, 3.seconds)}>showBriefly</p>\"\"\"
         |""".stripMargin
    
    // language=html
    s"""<h1>条件付きレンダリング</h1>
       |
       |<p>
       |  リアクティブな真偽値に合わせて表示有無を切り替える方法です.
       |</p>
       |
       |<ul class="toc">
       |  <li><a ${evClick(setHash("ifC"))}>ifC</a></li>
       |  <li><a ${evClick(setHash("showIf"))}>showIf</a></li>
       |</ul>
       |
       |<h2 id="ifC">ifC</h2>
       |
       |<p>
       |  vue.jsにおけるv-ifに相当するものになります. <br>
       |  非表示状態になっている時はDOMが存在しなくなります.<br>
       |  一方、記述が比較的に煩雑なので、<br>
       |  後述のshowIfで問題ない場合はそちらを推奨します.<br>
       |  (※煩雑なのはScalactive実装者の実力問題なので助言お待ちしております.)
       |</p>
       |
       |<pre><code class="language-scala">${esc(codeIfC)}</code></pre>
       |
       |<h2 id="showIf">showIf</h2>
       |
       |<p>
       |  vue.jsにおけるv-showに相当するものになります. <br>
       |  非表示状態になっている時もDOMは存在します.<br>
       |  ※displayスタイルがnoneとなっています.
       |</p>
       |
       |<pre><code class="language-scala">${esc(codeShowIf)}</code></pre>
       |
       |<p>
       |  表示状態では標準でdisplayスタイルにblockが設定されます.<br>
       |  別の値を設定したい場合は、showIfの第2引数に指定できます.<br>
       |  ※flexとgridについては専用のshowFlexIf, showGridIf関数でも可能です.
       |</p>
       |
       |<pre><code class="language-scala">${esc(codeShowIf2)}</code></pre>
       | 
       |<p>
       |  showIfBriefly関数では指定時間後に自動的に非表示化させることができます.
       |</p>
       |
       |<pre><code class="language-scala">${esc(codeShowIfBriefly)}</code></pre> 
       | 
       |""".stripMargin
  }

}
