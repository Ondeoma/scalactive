<script lang="scala">
import Router.*
import io.github.ondeoma.scalactive.doc.components.contents.quickstart.Hello

val codeSetup = // language=scala
  s"""// project/plugins.sbt
     |addSbtPlugin("org.scala-js" % "sbt-scalajs" % "<version>")
     |addSbtPlugin("io.github.ondeoma" % "sbt-scalactive" % "<version>")
     |
     |// build.sbt
     |lazy val app = project
     |  .enablePlugins(ScalaJSPlugin, ScalactivePlugin)
     |  .settings(
     |    scalaJSUseMainModuleInitializer := true,
     |    libraryDependencies += "io.github.ondeoma" %%% "scalactive" % "<version>"
     |  )
     |""".stripMargin

val codeTree =
  """|src/main/
     |  scalactive/
     |    app/
     |      Hello.st   ... object app.Hello になります
     |  scala/
     |    app/
     |      App.scala  ... mainメソッド
     |""".stripMargin

val codeHello = // language=html
  """|<script lang="scala">
     |case class State(count: RV[Int] = RV(0))
     |
     |def initState() = State()
     |</script>
     |
     |<template>
     |  <p>Hello, Scalactive!</p>
     |  <button @click="s.count := s.count.v + 1">count: {{ s.count }}</button>
     |</template>
     |""".stripMargin

val codeMain = // language=scala
  s"""package app
     |
     |import io.github.ondeoma.scalactive.components.ComponentManager.*
     |import org.scalajs.dom.*
     |import org.scalajs.dom.document.body
     |
     |object App {
     |
     |  @main
     |  def main(): Unit = {
     |    Hello()(body, AddMethod.appendS(IdSelector("app"))).init()
     |  }
     |
     |}
     |""".stripMargin
</script>

<template>
  <h1>クイックスタート</h1>

  <ul class="toc">
    <li><a @click='setHash("env")'>推奨環境</a></li>
    <li><a @click='setHash("seed")'>セットアップ(シードプロジェクトの取得)</a></li>
    <li><a @click='setHash("plugin")'>sbtプラグインの設定</a></li>
    <li><a @click='setHash("files")'>.stファイルの置き場所</a></li>
    <li><a @click='setHash("first")'>はじめてのコンポーネント</a></li>
    <li><a @click='setHash("init")'>コンポーネントの初期化</a></li>
  </ul>

  <h2 id="env">推奨環境</h2>

  <ul>
    <li>・<a href="">sbt</a> 1.12系 最新</li>
    <li>・<a href="">node.js</a> v24系 最新</li>
  </ul>

  <h2 id="seed">セットアップ（シードプロジェクトの取得）</h2>

  <p>
    <a href="https://www.foundweekends.org/giter8/ja/index.html" target="_blank">Gitter8</a>テンプレートがあります.
    sbtコマンドで取得できます.
  </p>

  <pre><code class="shell">sbt new Ondeoma/scalactive-seed.g8</code></pre>

  <p>
    起動方法等はダウンロードしたプロジェクト内の
    <a href="https://github.com/Ondeoma/scalactive-seed.g8/blob/main/src/main/g8/READE.md" target="_blank">README.md</a>
    を参照ください.
  </p>

  <h2 id="plugin">sbtプラグインの設定</h2>

  <p>
    コンポーネントは <code>.st</code> ファイル(単一ファイルコンポーネント)で書きます.<br>
    .stファイルは、sbtプラグイン(sbt-scalactive)が、<br>
    ビルド時にScalaのソースへ変換します.<br>
    シードプロジェクトを使わない場合は、以下のように設定します.
  </p>

  <pre><code class="language-scala">{{ codeSetup }}</code></pre>

  <p>
    <code>&lt;version&gt;</code> の部分は、それぞれの最新のバージョンに置き換えてください.<br>
    sbt-scalactiveとscalactiveは同じバージョンを使います.
  </p>

  <h2 id="files">.stファイルの置き場所</h2>

  <p>
    <code>src/main/scalactive</code> 配下(または <code>src/main/scala</code> 配下)に置きます.<br>
    ディレクトリ構成がパッケージに、ファイル名がobject名になります.
  </p>

  <pre><code>{{ codeTree }}</code></pre>

  <h2 id="first">はじめてのコンポーネント</h2>

  <p>
    <code>src/main/scalactive/app/Hello.st</code> を作ります.
  </p>

  <pre><code class="language-html">{{ codeHello }}</code></pre>

  <p>
    <code>&lt;script&gt;</code> にScalaコード、<code>&lt;template&gt;</code> にHTMLを書きます.<br>
    <code>def initState()</code> で状態を作り、<br>
    テンプレートからは <code>s</code> で参照します.<br>
    以下のように表示されます.
  </p>

  <div class="rendered">
    <Hello />
  </div>

  <p>
    書き方の詳細は<a @click='go("/sfc")'>単一ファイルコンポーネント</a>を参照ください.
  </p>

  <h2 id="init">コンポーネントの初期化</h2>

  <p>
    作成したコンポーネント(起点コンポーネント)は以下のように呼び出します.
  </p>

  <pre><code>ComponentName()(ルート要素: HTMLElement, 追加方法: AddMethod).init()</code></pre>

  <p>
    以下はmainメソッドで利用した例です.<br>
    mainメソッドは通常のScalaファイル(<code>src/main/scala/app/App.scala</code>)に書きます.
  </p>

  <pre><code class="language-scala">{{ codeMain }}</code></pre>

  <p>
    上記の例では"Hello"を、<br>
    body要素内の"app"というIDを持つ要素に
    <a href="https://developer.mozilla.org/ja/docs/Web/API/Element/append" target="_blank">append</a>
    しています.
  </p>

  <p>
    殆どのケースでは上記例の、<br>
    コンポーネントとID部分を差し替えるだけで充分かと思いますが、<br>
    AddMethod/Selectorに関する詳細は以下ソースを参照ください.
  </p>

  <ul>
    <li>・<a href="https://github.com/Ondeoma/scalactive/blob/main/core/src/main/scala/io/github/ondeoma/scalactive/models/AddMethod.scala" target="_blank">AddMethod</a></li>
    <li>・<a href="https://github.com/Ondeoma/scalactive/blob/main/core/src/main/scala/io/github/ondeoma/scalactive/models/Selector.scala" target="_blank">Selector</a></li>
  </ul>

  <p>
    .stを使わずにScalaで書く方法は、<br>
    <a @click='go("/scalaApi")'>Scala API(従来の書き方)</a>を参照ください.
  </p>
</template>
