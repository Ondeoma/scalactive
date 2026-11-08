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

    val codeForEachC = // language=scala
      s"""val list = RVList(List("A", "B", "C"))
         |// 添字を使わない場合はforEachC
         |val loop = forEachC(list) { implicit (cm, s) =>
         |  s\"\"\"<p>$${s.v}</p>\"\"\"
         |}
         |""".stripMargin


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
       |<h2>リストが変わったとき</h2>
       |
       |<p>
       |  リストが変わったときは全行を作り直さず、変わった部分だけを画面に反映します.<br>
       |  行は、RVList・RMListでは要素のオブジェクト、それ以外のリストでは要素の値で対応付けられ、
       |  消えた行は取り除かれ、新しい行は作られ、残った行は(必要なら位置を動かして)そのまま使われます.<br>
       |  ただし、以下の行は作り直されます.
       |</p>
       |
       |<ul>
       |  <li>・RVListへの代入などで、値が変わった行</li>
       |  <li>・forCの場合、位置(添字)が変わった行. 行のHTMLに埋め込んだ添字が古くならないようにするためです</li>
       |</ul>
       |
       |<p>
       |  添字を使わない場合は、forEachCを使うと、行の削除や並べ替えで位置が変わった行も作り直さずに済むため高速です.<br>
       |  単一ファイルコンポーネント(.st)の <code>v-for</code> では、添字の変数を書かなければ自動的にforEachCになります.
       |</p>
       |
       |<pre><code class="language-scala">${esc(codeForEachC)}</code></pre>
       |
       |<p>
       |  利用できるコレクションは以下です(forC・forEachC共通).
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
