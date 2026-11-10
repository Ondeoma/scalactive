<script lang="scala">
import Router.*

val codeRoutingApp = // language=html
  """<script lang="scala">
    |import io.github.ondeoma.scalactive.components.routes.RouterComponent
    |import Router.*
    |
    |// パスと、描画するコンポーネントの組のリスト
    |val routes: PathPatternRoutes = List(
    |  ("/", _ => PageTitle(title = "TOP")),
    |  ("/items/:id", r => PageTitle(title = "商品 " + r.pathname.groups("id"))),
    |  ("/guide/*", _ => GuideLayout()),
    |)
    |</script>
    |
    |<template>
    |  <nav>
    |    <RouterLink to="/">TOP</RouterLink>
    |    <RouterLink to="/guide/intro">ガイド</RouterLink>
    |    <button @click='go("/items/1")'>商品1へ</button>
    |  </nav>
    |  <main>
    |    <RouterComponent :routes="routes" />
    |  </main>
    |</template>
    |""".stripMargin

val codePageTitle = // language=html
  """<script lang="scala">
     |case class Props(title: String)
     |
     |// 表示している間、画面(タブ)のタイトルにします
     |def mounted(props: Props): Unit = pageTitle(props.title)
     |</script>
     |
     |<template>
     |  <h2>{{ props.title }}</h2>
     |</template>
     |""".stripMargin

val codeGuideLayout = // language=html
  """<script lang="scala">
    |import io.github.ondeoma.scalactive.components.routes.RouterComponent
    |import Router.*
    |
    |// /guide より下のパス(/guide からの相対パスで書く)
    |val routes: PathPatternRoutes = List(
    |  ("intro", _ => PageTitle(title = "はじめに")),             // /guide/intro
    |  ("api/:name", r => PageTitle(title = r.pathname.groups("name"))), // /guide/api/xxx
    |)
    |</script>
    |
    |<template>
    |  <nav>
    |    <RouterLink to="/guide/intro">はじめに</RouterLink>
    |    <RouterLink to="/guide/api/router">API</RouterLink>
    |  </nav>
    |  <section>
    |    <RouterComponent :routes="routes" prefix="/guide" />
    |  </section>
    |</template>
    |""".stripMargin

val codeTitle = // language=scala
  """import io.github.ondeoma.scalactive.ScalactiveConfig
     |
     |// ルートにタイトルを書く
     |val routes = List(
     |  Route("/", _ => Top(), title = _ => "TOP"),
     |  Route("/items/:id", r => Item(), title = r => "商品 " + r.pathname.groups("id")),
     |)
     |
     |// コンポーネントの中で決める(.stでは def mounted の中で)
     |pageTitle("TOP")
     |pageTitle(item.mapC(i => "商品 " + i.name)) // 読み込んだデータに合わせて変わるタイトル
     |
     |// 書式と、どれも指定されていない時のタイトル
     |ScalactiveConfig.titleTemplate = "%s | MyApp"
     |ScalactiveConfig.defaultTitle = Some("MyApp")
     |""".stripMargin

val codeGuardedApp = // language=html
  """<script lang="scala">
    |import io.github.ondeoma.scalactive.components.routes.RouterComponent
    |import Router.*
    |
    |val loggedIn = RV(false)
    |val itemsLoaded = RV(false)
    |
    |val routes = List(
    |  Route("/", _ => PageTitle(title = "TOP")),
    |  Route("/login", _ => PageTitle(title = "ログイン")),
    |  // ルートに入るときだけ呼ばれるガード
    |  Route("/items/:id", r => PageTitle(title = "商品 " + r.pathname.groups("id")),
    |    beforeEnter = (to, from) => if (itemsLoaded.v) Continue else Redirect("/")),
    |  // ガードの中で to.meta として参照できる情報
    |  Route("/admin", _ => PageTitle(title = "管理"), meta = Map("requiresAuth" -> true)),
    |)
    |
    |// すべての遷移の前に呼ばれるガード
    |val removeGuard = Router.beforeEach { (to, from) =>
    |  if (to.meta.get("requiresAuth").contains(true) && !loggedIn.v) Redirect("/login")
    |  else Continue
    |}
    |
    |// 遷移の後に呼ばれる
    |Router.afterEach((to, from) => document.title = "MyApp " + to.path)
    |</script>
    |
    |<template>
    |  <main>
    |    <RouterComponent.of :routes="routes" />
    |  </main>
    |</template>
    |""".stripMargin

val codeGuard = // language=scala
  """import io.github.ondeoma.scalactive.routes.Router
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
    |Router.afterEach((to, from) => document.title = s"MyApp ${to.path}")
    |
    |// 登録の解除
    |removeGuard()
    |""".stripMargin

val codeBasePath = // language=scala
  """import io.github.ondeoma.scalactive.components.ComponentManager.*
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
</script>

<template>
  <h1>ルーティング</h1>

  <p>
    SPA(Single Page Application)的なクライアントサイドルーティングを実現するための機能です.
  </p>

  <ul class="toc">
    <li><a @click='setHash("RouterComponent")'>RouterComponent</a></li>
    <li><a @click='setHash("Router")'>Router.go</a></li>
    <li><a @click='setHash("Nested")'>ネストしたルート</a></li>
    <li><a @click='setHash("Guard")'>ナビゲーションガード</a></li>
    <li><a @click='setHash("Route")'>ルートごとのガード・meta</a></li>
    <li><a @click='setHash("Title")'>画面のタイトル</a></li>
    <li><a @click='setHash("Link")'>RouterLink</a></li>
    <li><a @click='setHash("BasePath")'>BasePath</a></li>
  </ul>

  <h2 id="RouterComponent">RouterComponent</h2>

  <p>
    RouterComponentを使うことでパスに対し、<br>
    どのコンポーネントをレンダリングするかを選択することができます.<br>
    <code>&lt;script&gt;</code> にパスとコンポーネントの組のリストを定義し、<br>
    テンプレートで <code>&lt;RouterComponent :routes="routes" /&gt;</code> と書きます.<br>
    RouterComponentはimportが必要です.
  </p>

  <div class="code-file">RoutingApp.st</div>
  <pre><code class="language-st">{{ codeRoutingApp }}</code></pre>

  <p>
    ルートから描画するコンポーネントは、<br>
    <code>PageTitle(title = "TOP")</code> のように、propsを名前付き引数で渡して作ります.<br>
    (propsのない.stは <code>Xxx()</code>)
  </p>

  <div class="code-file">PageTitle.st</div>
  <pre><code class="language-st">{{ codePageTitle }}</code></pre>

  <p>
    パスの判定には
    <a href="https://developer.mozilla.org/ja/docs/Web/API/URLPattern/exec" target="_blank">URLPattern.exec()</a>
    を利用しています.<br>
    この関数の戻り値(上の例の <code>r</code>)を利用して、<br>
    パスパラメータを取得すること等も可能です.<br>
    詳しくは <a href="https://developer.mozilla.org/ja/docs/Web/API/URLPattern/exec" target="_blank">URLPattern.exec()</a>
    を参照ください.
  </p>

  <p>
    本番環境などへのデプロイの際は、<br>
    HTTPサーバーミドルウェア等でindex.html等へのパスのフォールバック設定が必要です.<br>
    詳細はご利用になるミドルウェア等のドキュメントを参照ください.
  </p>

  <h2 id="Router">Router.go</h2>

  <p>
    画面遷移を行う際は Router.go 関数を利用下さい.<br>
    <code>import Router.*</code> しておくと、<br>
    上の例のように <code>@click='go("/items/1")'</code> と書けます.<br>
    RouterComponentはこの関数によるパスの変化を監視しており、<br>
    RouterComponentのみのリレンダリングが行われます.
  </p>

  <p>
    RouterComponentは、次のどれかが変わったときだけ描き直します.<br>
    マッチしたルート・名前付きのパスパラメータ(<code>:id</code> など)・クエリ文字列.<br>
    名前のないワイルドカード(<code>*</code>)の部分だけが変わった場合は描き直しません.<br>
    (次のネストしたルートのため)<br>
    ワイルドカードの値を使って描く場合は、<br>
    <code>/files/:path*</code> のように名前を付けてください.
  </p>

  <h2 id="Nested">ネストしたルート</h2>

  <p>
    ルートに割り当てたコンポーネント(レイアウト)の中にRouterComponentを置くと、<br>
    その部分だけを子のパスで切り替えられます.<br>
    Vue Routerのネストしたルート(childrenと入れ子のrouter-view)に当たります.<br>
    上の例の <code>"/guide/*"</code> に割り当てたレイアウトです.
  </p>

  <pre><code class="language-st">{{ codeGuideLayout }}</code></pre>

  <ul>
    <li>
      外側のルートは <code>"/guide/*"</code> のように、<br>
      ワイルドカードで子のパスまでマッチさせます.
    </li>
    <li>
      内側のRouterComponentには、<code>prefix="/guide"</code> を渡します.<br>
      パターンは <code>/guide</code> からの相対パスで指定します.
    </li>
    <li>
      /guide/intro から /guide/api/xxx への遷移では、<br>
      レイアウトは作り直されず、内側だけが切り替わります.
    </li>
  </ul>

  <h2 id="Guard">ナビゲーションガード</h2>

  <p>
    <code>Router.beforeEach</code> で、遷移の前に呼ばれる関数(ガード)を登録できます.<br>
    ログインしていない場合のリダイレクトや、<br>
    編集中の離脱の確認などに使います.<br>
    ルートのコンポーネントの <code>&lt;script&gt;</code> や、main関数などに書きます.
  </p>

  <pre><code class="language-scala">{{ codeGuard }}</code></pre>

  <ul>
    <li>
      ガードは遷移先 <code>to</code> と遷移元 <code>from</code> を受け取ります.<br>
      (<code>RouteLocation</code>. <code>path</code> はベースパスを除いたパス)<br>
      そして、次のどれかを返します.<br>
      <code>Continue</code>(遷移する)・<code>Cancel</code>(取り消す)・<code>Redirect(url)</code>(別のパスへ遷移する)
    </li>
    <li>
      Router.goのほか、ブラウザの戻る・進むでも呼ばれます.<br>
      戻る・進むを取り消した場合はURLを元に戻します.
    </li>
    <li>
      最初に登録したときに、ページを開いた直後の場所も確認します.<br>
      (このとき <code>from</code> は <code>None</code>)<br>
      Redirectを返すと、そのパスへ置き換えます.
    </li>
    <li>
      複数登録した場合は登録順に呼ばれ、<br>
      最初にContinue以外を返したものが使われます.
    </li>
    <li><code>Router.afterEach</code> で、遷移の後に呼ばれる関数を登録できます.</li>
    <li>
      Router.goで遷移したときにウィンドウを先頭までスクロールしたくない場合は、<br>
      <code>ScalactiveConfig.scrollToTopOnNavigate = false</code> を設定してください.
    </li>
  </ul>

  <h2 id="Route">ルートごとのガード・meta</h2>

  <p>
    <code>Route</code> でルートを定義し、<br>
    <code>&lt;RouterComponent.of :routes="routes" /&gt;</code> に渡すと、<br>
    ルートごとのガード(<code>beforeEnter</code>)と情報(<code>meta</code>)を指定できます.<br>
    (ネストしたルートでは <code>&lt;RouterComponent.of :routes="routes" prefix="/guide" /&gt;</code>)
  </p>

  <pre><code class="language-st">{{ codeGuardedApp }}</code></pre>

  <ul>
    <li>
      <code>beforeEnter</code> は、そのルートに別のルートから入るときに呼ばれます.<br>
      同じルートの中での移動(パスパラメータの変化など)では呼ばれません.
    </li>
    <li>
      <code>meta</code> は、表示中のRouterComponentのルートのうち、<br>
      遷移先にマッチするものから集められます.<br>
      ガードの中で <code>to.meta</code> として参照できます(ネストしたルートは合わせたもの).
    </li>
    <li>
      グローバルなガード(<code>Router.beforeEach</code>)が先に呼ばれます.<br>
      すべてContinueの場合に、ルートごとのガードが呼ばれます.
    </li>
    <li>
      <code>&lt;script&gt;</code> のトップレベルに書いた処理は、<br>
      コンポーネント(object)が最初に使われたときに1度だけ実行されます.
    </li>
  </ul>

  <h2 id="Title">画面のタイトル</h2>

  <p>
    画面(タブ)のタイトルは、ルートの <code>title</code> か、コンポーネントの中の <code>pageTitle</code> で指定します.<br>
    上の <code>PageTitle.st</code> は、mountedの中で <code>pageTitle(props.title)</code> を呼んでいるので、<br>
    遷移すると、表示したページのタイトルに変わります.
  </p>

  <pre><code class="language-scala">{{ codeTitle }}</code></pre>

  <ul>
    <li>
      タイトルは、表示されている間だけ有効です.<br>
      コンポーネントが片付けられると取り下げ、前のタイトルに戻ります.
    </li>
    <li>
      ルートの <code>title</code> より、ルートのコンポーネントの中の <code>pageTitle</code> が優先されます.<br>
      レイアウトとページの両方が指定している場合は、ページ(子・ネストしたルートの側)のものを使います.
    </li>
    <li>
      どれも指定されていない場合は、<code>ScalactiveConfig.defaultTitle</code>、<br>
      それもなければ指定される前のタイトルに戻します.
    </li>
  </ul>

  <h2 id="Link">RouterLink</h2>

  <p>
    <code>&lt;RouterLink to="/x"&gt;</code> は、クリックでRouter.goするリンクです.<br>
    現在の場所に応じて、<br>
    <code>router-link-active</code> / <code>router-link-exact-active</code> クラスを付けます.<br>
    詳しくは <a @click='go("/builtinComponents")'>組込コンポーネント</a> を参照ください.
  </p>

  <div class="rendered">
    <style>.link-demo .router-link-active { font-weight: bold; } .link-demo .router-link-exact-active { color: tomato; }</style>
    <p class="link-demo"><RouterLink to="/">はじめに</RouterLink> / <RouterLink to="/routing">ルーティング(このページ)</RouterLink> / <RouterLink to="/component">コンポーネント</RouterLink></p>
  </div>

  <h2 id="BasePath">BasePath</h2>

  <p>
    Scalacitveがドメイン直下パスではなく、<br>
    特定のサブパスにホスティングされる場合はベースパス設定を行ってください.<br>
    以下はMain関数で設定する例です.
  </p>

  <pre><code class="language-scala">{{ codeBasePath }}</code></pre>

  <p>
    Router.goで遷移する時と、RouterComponentのパス判定時にベースパスが考慮されます.
  </p>
</template>
