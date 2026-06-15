package io.github.ondeoma.scalactive.doc.components.contents

import io.github.ondeoma.scalactive.components.*
import org.scalajs.dom.*

object QuickStartComponent extends StatelessComponent {

  def genHtml(implicit cm: ComponentManager): HTML = {
    
    val code = // language=scala
      s"""import components.MainComponent
         |import io.github.ondeoma.scalactive.components.ComponentManager.*
         |import org.scalajs.dom.*
         |import org.scalajs.dom.document.body
         |
         |object App {
         |
         |  @main
         |  def main(): Unit = {
         |    MainComponent()(body, AddMethod.appendS(IdSelector("app"))).init()
         |  }
         |
         |}
         |""".stripMargin
    
    // language=html
    s"""<h1>クイックスタート</h1>
       |
       |<h2>推奨環境</h2>
       |
       |<ul>
       |  <li>・<a href="">sbt</a> 1.12系 最新</li>
       |  <li>・<a href="">node.js</a> v24系 最新</li>
       |</ul>
       |
       |<h2>セットアップ（シードプロジェクトの取得）</h2>
       |
       |<p>
       |  <a href="https://www.foundweekends.org/giter8/ja/index.html" target="_blank">Gitter8</a>テンプレートがあります.
       |  sbtコマンドで取得できます.
       |</p>
       |
       |<pre><code class="shell">sbt new scala/scala-seed.g8</code></pre>
       |
       |<p>
       |  起動方法等はダウンロードしたプロジェクト内の
       |  <a href="https://github.com/Ondeoma/scalactive-seed.g8/blob/main/src/main/g8/READE.md" target="_blank">README.md</a>
       |  を参照ください.
       |</p>
       |
       |<h2>Scalactiveコンポーネントの初期化について</h2>
       |
       |<p>
       |  作成したコンポーネント(起点コンポーネント)は以下のように呼び出します.
       |</p>
       |
       |<pre><code>ComponentName()(ルート要素: HTMLElement, 追加方法: AddMethod).init()</code></pre>
       |
       |<p>
       |  以下はmainメソッドで利用した例です。
       |</p>
       |
       |<pre><code class="language-scala">${code}</code></pre>
       |
       |
       |<p>
       |  上記の例では"MainComponent"を、<br>
       |  body要素内の"app"というIDを持つ要素に
       |  <a href="https://developer.mozilla.org/ja/docs/Web/API/Element/append" target="_blank">append</a>
       |  しています。
       |</p>
       |
       |<p>
       |  殆どのケースでは上記例の、<br>
       |  コンポーネントとID部分を差し替えるだけで充分かと思いますが、<br>
       |  AddMethod/Selectorに関する詳細は以下ソースを参照ください。
       |</p>
       |
       |<ul>
       |  <li>・<a href="https://github.com/Ondeoma/scalactive/blob/main/core/src/main/scala/io/github/ondeoma/scalactive/models/AddMethod.scala" target="_blank">AddMethod</a></li>
       |  <li>・<a href="https://github.com/Ondeoma/scalactive/blob/main/core/src/main/scala/io/github/ondeoma/scalactive/models/Selector.scala" target="_blank">Selector</a></li>
       |</ul>
       |
       |
       |""".stripMargin
  }
  
}
