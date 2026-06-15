package io.github.ondeoma.scalactive.doc.components.contents

import io.github.ondeoma.scalactive.components.*
import io.github.ondeoma.scalactive.components.routes.RouterComponent
import org.scalajs.dom.*

object RoutingComponent extends StatelessComponent {

  def genHtml(implicit cm: ComponentManager): HTML = {

    import Router.*
    import io.github.ondeoma.scalactive.doc.utils.Utility.esc

    val code = // language=scala
      s"""// RootComponent, AComponent, BComponentは別途実装されている想定です.
         |val routes: PathPatternRoutes = List(
         |  ("/", matchResult => RootComponent()),
         |  ("/a", matchResult => AComponent()),
         |  ("/b", matchResult => BComponent()),
         |)
         |s\"\"\"<main>$${%(RouterComponent(routes, ""))}</main>\"\"\"
         |""".stripMargin

    val code2 = // language=scala
      s"""import io.github.ondeoma.scalactive.routes.Router.go
         |s\"\"\"<a $${evClick(go("/"))}>TOP</a>
         |   :<a $${evClick(go("/b"))}>B</a>
         |   :<a $${evClick(go("/a"))}>A</a>\"\"\".stripMargin(":")""".stripMargin

    // language=html
    s"""<h1>ルーティング</h1>
       |
       |<p>
       |  SPA(Single Page Application)的なクライアントサイドルーティングを実現するための機能です.
       |</p>
       |
       |<ul class="toc">
       |  <li><a ${evClick(setHash("RouterComponent"))}>RouterComponent</a></li>
       |  <li><a ${evClick(setHash("Router"))}>Router</a></li>
       |</ul>
       |
       |<h2 id="RouterComponent">RouterComponent</h2>
       |
       |<p>
       |  RouterComponentを使うことでパスに対し、<br>
       |  どのコンポーネントをレンダリングするかを選択することができます.
       |</p>
       |
       |<p>
       |  第1引数にパスに対するコンポーネントの設定リスト、<br>
       |  第2引数にはベースパスを設定して下さい.<br>
       |  例えば、第2引数に"/base"を指定した場合、<br>
       |  第1引数の全パスの先頭部分に"/base"が追加された形で判定されます.<br>
       |</p>
       |
       |<p>
       |  パスの判定には
       |  <a href="https://developer.mozilla.org/ja/docs/Web/API/URLPattern/exec" target="_blank">URLPattern.exec()</a>
       |  を利用しています。<br>
       |  この関数の戻り値(以下サンプルソースのmatchResult部分)を利用して、<br>
       |  パスパラメータを取得すること等も可能です.<br>
       |  詳しくは <a href="https://developer.mozilla.org/ja/docs/Web/API/URLPattern/exec" target="_blank">URLPattern.exec()</a>
       |  を参照ください.
       |</p>
       |
       |<pre><code class="language-scala">${esc(code)}</code></pre>
       |
       |<p>
       |  本番環境などへのデプロイの際は、<br>
       |  HTTPサーバーミドルウェア等でindex.html等へのパスのフォールバック設定が必要です.<br>
       |  詳細はご利用になるミドルウェア等のドキュメントを参照ください.
       |</p>
       |
       |<h2 id="Router">Router</h2>
       |
       |<p>
       |  画面遷移を行う際は Router.go 関数を利用下さい.<br> 
       |  RouterComponentはこの関数によるパスの変化を監視しており、<br>
       |  RouterComponentのみのリレンダリングが行われます.
       |</p>
       |
       |<pre><code class="language-scala">${esc(code2)}</code></pre>
       |
       |""".stripMargin
  }
  
}
