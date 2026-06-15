package io.github.ondeoma.scalactive.doc.components.contents

import io.github.ondeoma.scalactive.components.*
import io.github.ondeoma.scalactive.doc.components.contents.component.*
import io.github.ondeoma.scalactive.doc.utils.Utility
import org.scalajs.dom.*

object ComponentComponent extends StatelessComponent {

  def genHtml(implicit cm: ComponentManager): HTML = {

    import Router.*
    import Utility.esc
    
    val sample1Code = // language=scala
      s"""import io.github.ondeoma.scalactive.components.*
         |import org.scalajs.dom.window.alert
         |
         |object Sample1Component extends StatelessComponent {
         |
         |  def genHtml(implicit cm: ComponentManager): HTML = {
         |    // language=html
         |    s\"\"\"<div>
         |       :  <p>これは Sample1Component です。</p>
         |       :  <button $${evClick(alert("Sample1Component"))}>Click</button>
         |       :</div>
         |       :\"\"\".stripMargin(':')
         |  }
         |}   
         |""".stripMargin
    
    val sample2Code = // language=scala
      s"""import io.github.ondeoma.scalactive.components.*
         |import io.github.ondeoma.scalactive.reactive.*
         |import Sample2Component.State
         |
         |object Sample2Component extends StatefulComponent[State] {
         |
         |  case class State(count: RV[Int] = RV(0))
         |
         |  def initState() = State()
         |
         |  def genHtml(s: State)
         |             (implicit cm: ComponentManager): HTML = {
         |    // language=html
         |    s\"\"\"<div>
         |       :  <p>
         |       :    これは Sample2Component です。<br>
         |       :    可変値 => $${%(TextComponent(s.count)(_.toString))}
         |       :  </p>
         |       :  <button $${evClick(s.count := s.count.v + 1)}>Click</button>
         |       :</div>
         |       :\"\"\".stripMargin(':')
         |  }
         |} 
         |""".stripMargin
    
    val sample3Code = // language=scala
      s"""import io.github.ondeoma.scalactive.components.*
         |import org.scalajs.dom.HTMLElement
         |
         |object Sample3Component extends BaseComponent {
         |
         |  def apply(valueFromParent: String)
         |           (implicit parent: HTMLElement,
         |            am: AddMethod): NodesComponentController = {
         |    // mkNCC = NodesComponentControllerを作成する便利関数        
         |    mkNCC(genHtml(valueFromParent))
         |  }
         |
         |  def genHtml(valueFromParent: String)
         |             (implicit cm: ComponentManager): HTML = {
         |    // language=html
         |    s\"\"\"<div>
         |       :  これは Sample3Component です。<br>
         |       :  親コンポーネントからは"$${valueFromParent}"を受け取りました。
         |       :</div>
         |       :\"\"\".stripMargin(':')
         |  }
         |}  
         |""".stripMargin
    
    val sample4Code = // language=scala
      s"""import io.github.ondeoma.scalactive.components.*
         |
         |object Sample4Component extends StatelessComponent {
         |  
         |  def genHtml(implicit cm: ComponentManager): HTML = {
         |    // language=html
         |    s\"\"\"<div>
         |       :  <p>これは Sample4Component です。</p>
         |       :  <hr>
         |       :  $${%(Sample1Component())}
         |       :  $${%(Sample2Component())}
         |       :  $${%(Sample3Component("BySample4Component"))}
         |       :</div>
         |       :\"\"\".stripMargin(':')
         |  }
         |  
         |}  
         |""".stripMargin

    // language=html
    s"""<h1>コンポーネント</h1>
       |
       |<p>
       |  コンポーネントはHTML＋処理＋状態のような概念です.
       |</p>
       |
       |<ul class="toc">
       |  <li><a ${evClick(setHash("StatelessComponent"))}>StatelessComponent</a></li>
       |  <li><a ${evClick(setHash("StatefulComponent"))}>StatefulComponent</a></li>
       |  <li><a ${evClick(setHash("BaseComponent"))}>BaseComponent</a></li>
       |  <li><a ${evClick(setHash("Use"))}>コンポーネントの呼出し</a></li>
       |</ul>
       |
       |
       |<h2 id="StatelessComponent">StatelessComponent</h2>
       |
       |<p>
       |  最も単純な作成方法は、<br>
       |  StatelessComponentを実装したオブジェクトを作成することです.<br>
       |  状態(可変値)を持たないコンポーネントに適しています.<br>
       |  以下のようにgetHtmlを実装します.
       |</p>
       |
       |<pre><code class="language-scala">${esc(sample1Code)}</code></pre>
       |
       |<div class="rendered">
       |  ${%(Sample1Component())}
       |</div>
       |
       |<h2 id="StatefulComponent">StatefulComponent</h2>
       |
       |<p>
       |  StatefulComponentは状態を持つコンポーネントの作成に便利です.<br>
       |  
       |  <ul>
       |    <li>・状態の型</li>
       |    <li>・状態の初期化(initState)</li>
       |    <li>・getHtml</li>
       |  </ul>
       |  
       |  を実装します。
       |</p>
       |
       |<pre><code class="language-scala">${esc(sample2Code)}</code></pre>
       |
       |<div class="rendered">
       |  ${%(Sample2Component())}
       |</div>
       |
       |<h2 id="BaseComponent">BaseComponent</h2>
       |
       |<p>
       |  その他のコンポーネントはBaseComponentで作成します.<br>
       |  自由度が高く、親コンポーネントから値を受け取ること等ができるようになります.<br>
       |  原則、BaseComponentでは以下のシグネチャを持ったapplyとgetHtmlを実装します.
       |</p>
       |
       |<pre><code class="language-scala">def apply(任意引数)(implicit parent: HTMLElement, am: AddMethod): NodesComponentController
       |def genHtml(任意引数)(implicit cm: ComponentManager): HTML // HTML = String
       |</code></pre>
       |
       |<p>以下実装例です.</p>
       |
       |<pre><code class="language-scala">${esc(sample3Code)}</code></pre>
       |
       |<p>親から"ParentValue"を受けとった場合は以下のように表示されます.</p>
       |
       |<div class="rendered">
       |  ${%(Sample3Component("ParentValue"))}
       |</div>
       |
       |<h2 id="Use">コンポーネントの呼出し</h2>
       |
       |<p>
       |  作成したコンポーネントは`%`関数で呼び出すことができます.<br>
       |  ※ComponentManagerが裏で色々やっています.
       |</p>
       |
       |<pre><code class="language-scala">${esc(sample4Code)}</code></pre>
       |
       |<div class="rendered">
       |  ${%(Sample4Component())}
       |</div>
       |
       |
       |""".stripMargin
  }
   
}
