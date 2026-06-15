package io.github.ondeoma.scalactive.doc.components.contents

import io.github.ondeoma.scalactive.components.*
import org.scalajs.dom.*

object ListRenderingComponent extends StatelessComponent {

  def genHtml(implicit cm: ComponentManager): HTML = {

    import io.github.ondeoma.scalactive.doc.utils.Utility.esc

    val codeForC = // language=scala
      s"""val list = RVList(List("A", "B", "C"))
         |val loop = forC(list) { implicit (cm, s, index) =>
         |  s\"\"\"<p>$${index}:$${s.v}</p>\"\"\"
         |}
         |
         |\"\"\"<div>$${loop}</div>\"\"\"
         |""".stripMargin
    
    val list = RVList(List("A", "B", "C"))
    val loopC = forC(list) { implicit (cm, n, index) =>
      s"""<p>${index}:${n.v}</p>"""
    }
    

    // language=html
    s"""<h1>リストレンダリング</h1>
       |
       |<p>
       |  forC関数はvue.jsにおけるv-for機能に対応するものです.<br>
       |  ※文字列中のインライン記述は苦しいので、以下のような分けた記述を推奨します.
       |</p>
       |
       |<pre><code class="language-scala">${esc(codeForC)}</code></pre>
       |
       |<div class="rendered">$loopC</div>
       |
       |<p>
       |  利用できるコレクションは以下です.
       |</p>
       |
       |<ul>
       |  <li>・List[?]</li>
       |  <li>・RVList[?]</li>
       |  <li>・RMList[?, ?]</li>
       |  <li>・CRV[List[?]]</li>
       |  <li>・RV[List[?]]</li>
       |  <li>・ReactiveList[?]</li>
       |</ul>
       |""".stripMargin
  }
  
}
