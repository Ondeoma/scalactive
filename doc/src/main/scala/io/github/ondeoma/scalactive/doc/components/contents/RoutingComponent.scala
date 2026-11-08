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
         |s\"\"\"<main>$${%(RouterComponent(routes))}</main>\"\"\"
         |""".stripMargin

    val code2 = // language=scala
      s"""import io.github.ondeoma.scalactive.routes.Router.go
         |s\"\"\"<a $${evClick(go("/"))}>TOP</a>
         |   :<a $${evClick(go("/b"))}>B</a>
         |   :<a $${evClick(go("/a"))}>A</a>\"\"\".stripMargin(":")""".stripMargin


    val code3 = // language=scala
      s"""import io.github.ondeoma.scalactive.components.ComponentManager.*
         |import org.scalajs.dom.*
         |import org.scalajs.dom.document.body
         |import io.github.ondeoma.scalactive.ScalactiveConfig
         |
         |object App {
         |
         |  @main
         |  def main(): Unit = {
         |    // "https://example.com/subpath/..."のような場合.
         |    ScalactiveConfig.basePath = "subpath" 
         |    
         |    // MainComponent()(body, AddMethod.appendS(IdSelector("app"))).init()
         |  }
         |}""".stripMargin  

    val codeNested = // language=scala
      s"""// 外側のルート: "/guide/*" で /guide 以下のすべてのパスにマッチさせる
         |val routes: PathPatternRoutes = List(
         |  ("/", _ => RootComponent()),
         |  ("/guide/*", _ => GuideLayout()),
         |)
         |
         |// レイアウト: 内側のRouterComponentで /guide より下のパスを切り替える
         |object GuideLayout extends StatelessComponent {
         |  def genHtml(implicit cm: ComponentManager): HTML = {
         |    val inner = RouterComponent.nested("/guide")(List(
         |      ("intro", _ => IntroComponent()),        // /guide/intro
         |      ("api/:name", r => ApiComponent(r.pathname.groups("name"))), // /guide/api/xxx
         |    ))
         |    s\"\"\"<nav>...</nav><section>$${%(inner)}</section>\"\"\"
         |  }
         |}
         |""".stripMargin

    val codeGuard = // language=scala
      s"""import io.github.ondeoma.scalactive.routes.Router
         |import io.github.ondeoma.scalactive.routes.Router.*
         |
         |// 遷移の前に呼ばれる(Router.go・戻る/進む・ページを開いた直後)
         |val removeGuard = Router.beforeEach { (to, from) =>
         |  if (to.path.startsWith("/admin") && !loggedIn.v) Redirect("/login")
         |  else if (editing.v && !window.confirm("保存していない変更があります. 移動しますか?")) Cancel
         |  else Continue
         |}
         |
         |// 遷移の後に呼ばれる
         |Router.afterEach((to, from) => document.title = s"MyApp $${to.path}")
         |
         |// 登録の解除
         |removeGuard()
         |""".stripMargin

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
       |  <li><a ${evClick(setHash("Nested"))}>ネストしたルート</a></li>
       |  <li><a ${evClick(setHash("Guard"))}>ナビゲーションガード</a></li>
       |  <li><a ${evClick(setHash("BasePath"))}>BasePath</a></li>
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
       |<p>
       |  RouterComponentは、マッチしたルート・名前付きのパスパラメータ(<code>:id</code> など)・クエリ文字列のどれかが変わったときだけ描き直します.
       |  名前のないワイルドカード(<code>*</code>)の部分だけが変わった場合は描き直しません(次のネストしたルートのため).
       |  ワイルドカードの値を使って描く場合は、<code>/files/:path*</code> のように名前を付けてください.
       |</p>
       |
       |<h2 id="Nested">ネストしたルート</h2>
       |
       |<p>
       |  ルートに割り当てたコンポーネント(レイアウト)の中にRouterComponentを置くと、その部分だけを子のパスで切り替えられます.
       |  Vue Routerのネストしたルート(childrenと入れ子のrouter-view)に当たります.
       |</p>
       |
       |<pre><code class="language-scala">${esc(codeNested)}</code></pre>
       |
       |<ul>
       |  <li>外側のルートは <code>"/guide/*"</code> のように、ワイルドカードで子のパスまでマッチさせます.</li>
       |  <li>
       |    内側は <code>RouterComponent.nested("/guide")(routes)</code> と書き、パターンは <code>/guide</code> からの相対パスで指定します.
       |    (<code>RouterComponent(routes, base = ...)</code> にベースパスを渡しても同じです.)
       |  </li>
       |  <li>/guide/intro から /guide/api/xxx への遷移では、レイアウトは作り直されず、内側だけが切り替わります.</li>
       |</ul>
       |
       |<h2 id="Guard">ナビゲーションガード</h2>
       |
       |<p>
       |  <code>Router.beforeEach</code> で、遷移の前に呼ばれる関数(ガード)を登録できます.
       |  ログインしていない場合のリダイレクトや、編集中の離脱の確認などに使います.
       |</p>
       |
       |<pre><code class="language-scala">${esc(codeGuard)}</code></pre>
       |
       |<ul>
       |  <li>
       |    ガードは遷移先 <code>to</code> と遷移元 <code>from</code>(<code>RouteLocation</code>. <code>path</code> はベースパスを除いたパス)を受け取り、
       |    <code>Continue</code>(遷移する)・<code>Cancel</code>(取り消す)・<code>Redirect(url)</code>(別のパスへ遷移する)のどれかを返します.
       |  </li>
       |  <li>Router.goのほか、ブラウザの戻る・進むでも呼ばれます. 戻る・進むを取り消した場合はURLを元に戻します.</li>
       |  <li>
       |    最初に登録したときに、ページを開いた直後の場所も確認します(このとき <code>from</code> は <code>None</code>).
       |    Redirectを返すと、そのパスへ置き換えます.
       |  </li>
       |  <li>複数登録した場合は登録順に呼ばれ、最初にContinue以外を返したものが使われます.</li>
       |  <li><code>Router.afterEach</code> で、遷移の後に呼ばれる関数を登録できます.</li>
       |  <li>
       |    Router.goで遷移したときにウィンドウを先頭までスクロールしたくない場合は、
       |    <code>ScalactiveConfig.scrollToTopOnNavigate = false</code> を設定してください.
       |  </li>
       |</ul>
       |
       |
       |<h2 id="BasePath">BasePath</h2>
       |
       |<p>
       |  Scalacitveがドメイン直下パスではなく、<br>
       |  特定のサブパスにホスティングされる場合はベースパス設定を行ってください.<br>
       |  以下はMain関数で設定する例です.
       |</p>
       |
       |<pre><code class="language-scala">${esc(code3)}</code></pre>
       |
       |<p>
       |  Router.goで遷移する時と、RouterComponentのパス判定時にベースパスが考慮されます.
       |</p>
       |""".stripMargin
  }
  
}
