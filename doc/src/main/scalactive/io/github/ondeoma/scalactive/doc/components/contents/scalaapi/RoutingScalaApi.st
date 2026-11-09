<script lang="scala">
import Router.*

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

val codeLink = // language=scala
  s"""s\"\"\"<nav>$${linkC("/")(_ => "TOP")} $${linkC("/guide")(_ => "ガイド")}</nav>\"\"\"
     |""".stripMargin

val codeRoute = // language=scala
  s"""val routes = List(
     |  Route("/", _ => HomeComponent()),
     |  Route("/admin", _ => AdminComponent(), meta = Map("requiresAuth" -> true)),
     |)
     |
     |s\"\"\"<main>$${%(RouterComponent.of(routes))}</main>\"\"\"
     |""".stripMargin
</script>

<template>
  <h2 id="routing">ルーティング</h2>

  <p>
    Scalaの文字列補間でHTMLを書く場合の、<br>
    RouterComponent・RouterLinkの使い方です.<br>
    ルートの定義・ガード・ベースパスは、.stの場合と同じです.
  </p>

  <h3 id="routing-RouterComponent">RouterComponent</h3>

  <p>
    パスとコンポーネントの組のリストを、<br>
    <code>%(RouterComponent(routes))</code> で埋め込みます.
  </p>

  <pre><code class="language-scala">{{ code }}</code></pre>

  <h3 id="routing-Router">Router.go</h3>

  <p>
    画面遷移には、evClickなどからRouter.go関数を呼び出します.
  </p>

  <pre><code class="language-scala">{{ code2 }}</code></pre>

  <h3 id="routing-Nested">ネストしたルート</h3>

  <p>
    レイアウトのコンポーネントの中で、<br>
    <code>RouterComponent.nested("/guide")(routes)</code> を埋め込みます.
  </p>

  <pre><code class="language-scala">{{ codeNested }}</code></pre>

  <h3 id="routing-Link">linkC</h3>

  <p>
    RouterLinkは <code>linkC(to)(...)</code> で埋め込みます.
  </p>

  <pre><code class="language-scala">{{ codeLink }}</code></pre>

  <h3 id="routing-Route">RouterComponent.of</h3>

  <p>
    <code>Route</code> のリストは <code>RouterComponent.of(routes)</code> で埋め込みます.<br>
    (ネストしたルートでは <code>RouterComponent.nestedOf(prefix)(routes)</code>)
  </p>

  <pre><code class="language-scala">{{ codeRoute }}</code></pre>
</template>
