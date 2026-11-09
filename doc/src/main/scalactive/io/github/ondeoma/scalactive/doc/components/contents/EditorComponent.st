<script lang="scala">
import Router.*

val codeServer =
  """|# リポジトリ直下で、言語サーバーのjarを作ります(両方のエディタに同梱されます)
     |sbt languageServer/assembly
     |""".stripMargin

val codeVsCode =
  """|cd editors/vscode
     |npm install
     |npm run package   # scalactive-st-<version>.vsix を作成
     |code --install-extension scalactive-st-<version>.vsix
     |""".stripMargin

val codeIntelliJ =
  """|cd editors/intellij
     |./gradlew buildPlugin   # build/distributions/scalactive-st-intellij-<version>.zip を作成
     |""".stripMargin
</script>

<template>
  <h1>エディタ</h1>

  <p>
    VS Code拡張とIntelliJプラグインで、<code>.st</code> のシンタックスハイライトと、<br>
    Scalaの部分の言語機能が使えます.<br>
    どちらも同じ言語サーバーを使います.
  </p>

  <ul class="toc">
    <li><a @click='setHash("features")'>機能</a></li>
    <li><a @click='setHash("setup")'>使い始める</a></li>
    <li><a @click='setHash("install")'>インストール</a></li>
    <li><a @click='setHash("update")'>更新</a></li>
    <li><a @click='setHash("limitations")'>制限事項</a></li>
  </ul>

  <h2 id="features">機能</h2>

  <h3>ハイライト</h3>

  <p>
    <code>&lt;script&gt;</code> と、テンプレート内の <code>&#123;&#123; &#125;&#125;</code>・ディレクティブ・<code>:attr</code>・<code>@event</code> の値がScalaとして、<br>
    <code>&lt;style&gt;</code> がCSSとしてハイライトされます.<br>
    <code>&lt;script&gt;</code> の中で <code>// language=scala</code> / <code>html</code> / <code>css</code> のコメントに続く三重引用符の文字列は、<br>
    その言語としてハイライトされます(<code>html</code> は.stとして扱います).<br>
    IntelliJでは、IntelliJが扱える言語ならどれでも指定でき(<code>// language=json</code> など)、<br>
    その言語の補完や検査も使えます.
  </p>

  <h3>言語サーバーの機能</h3>

  <p>
    Scalaの部分では、言語サーバーにより次の機能が使えます.
  </p>

  <ul>
    <li>
      補完<br>
      (テンプレートのHTMLの部分では、同じパッケージのコンポーネント名、<br>
      子コンポーネントのprops・イベント・v-model、ディレクティブ、スロット名、<br>
      HTMLの標準のタグ・属性・イベント・列挙された属性値(inputのtypeなど)、<br>
      <code>class="..."</code> では同じファイルの <code>&lt;style&gt;</code> のクラスも補完します).<br>
      importしていないクラスも候補に出し、選ぶと <code>&lt;script&gt;</code> の先頭にimportを足します.
    </li>
    <li>型エラーなどの表示(.st上の位置で表示されます)</li>
    <li>
      子コンポーネントのpropの参照の検索と名前の変更<br>
      (定義・属性・名前付きスロット・定義した.stの中の参照をまとめて書き換え.<br>
      @closeやv-model:xなどの書き方に合わせます)
    </li>
    <li>
      子コンポーネントのタグの検査(未知のprop・スロット、必須のpropの不足を警告)と、<br>
      そのクイックフィックス(近い名前への修正、足りない必須のpropの書き足し)
    </li>
    <li>「Not found: X」のエラーに、Xをimportするクイックフィックス</li>
    <li>推論した型のインレイヒント(scriptの型を書いていない val / def、v-forの変数など)</li>
    <li>
      ホバーでの型とドキュメント(scaladoc・javadoc)の表示<br>
      (子コンポーネントのタグではPropsの一覧、属性ではpropの型、<br>
      HTMLのタグ・属性・イベントでは説明)、引数のヒント.<br>
      補完の候補にもドキュメントを表示します
    </li>
    <li>
      定義へのジャンプ<br>
      (同じ.st内と、プロジェクト内の .scala / .st、ライブラリ(jar)のソース.<br>
      子コンポーネントのタグ名・属性名からは、その.stのPropsへ.<br>
      クラス名からは <code>&lt;style&gt;</code> のセレクタへ)
    </li>
    <li><code>&lt;style&gt;</code> のクラスのうち、テンプレートで使っていないものの表示</li>
    <li>
      コンポーネントの抽出<br>
      (テンプレートで要素を選び、新しい.stに切り出して元の場所をタグに置き換えます.<br>
      使っている値はpropsになります)
    </li>
    <li>
      ドキュメントのフォーマット(整形).<br>
      テンプレートは、要素の入れ子に合わせて2スペースずつインデントし直します.<br>
      <code>&lt;style&gt;</code> のCSSは、1行で書いたルールは1行のまま空白を整え、<br>
      複数行のルールは宣言を1行ずつインデントします.<br>
      <code>&lt;script&gt;</code> の中はscalafmtで整形します<br>
      (<code>.scalafmt.conf</code> があればその設定、なければ改行の位置を保つ組み込みの設定).<br>
      構文エラーなどで整形できないときは、<br>
      行の途中の余分な空白(連続した空白、括弧の内側と , の前の空白)だけを取り除きます.
    </li>
  </ul>

  <h3>編集の補助</h3>

  <ul>
    <li>
      新しいコンポーネントの作成<br>
      (VS Codeではエクスプローラーの右クリック、IntelliJでは New → Scalactive Component.<br>
      ふつう・Props付き・State付きのひな形から作れます)
    </li>
    <li>.stから生成されたScalaのソースを開く(Open Generated Scala Source)</li>
    <li>stripMarginの文字列の中で改行すると、次の行の同じ位置に <code>|</code> を補います.</li>
    <li>
      IntelliJでは、HTMLの補完・閉じタグの挿入・Emmet、<code>&lt;style&gt;</code> のCSSの補完も使えます.<br>
      scriptの中の複数行の文字列は折りたためます.<br>
      .stのファイル名を変えると、同じディレクトリの.stにあるそのタグも書き換えます.
    </li>
  </ul>

  <h2 id="setup">使い始める</h2>

  <p>
    言語サーバーは、ScalactivePluginを有効にしたsbtプロジェクトで、<br>
    <code>sbt compile</code> を1度実行すると使えるようになります.<br>
    (コンパイル時に、クラスパスなどを <code>target/scalactive-lsp.json</code> に書き出します.)<br>
    エディタで.stを開いたときにこのファイルがなければ通知が出て、そこから <code>sbt compile</code> を実行できます.
  </p>

  <ul>
    <li>他の.stやScalaソースの定義は、最後にコンパイルした時点の内容で参照されます.</li>
    <li>言語サーバーの実行にはJava 11以降が必要です(IntelliJではIDEのJavaを使います).</li>
    <li>初回はScalaのpresentation compilerをMaven Centralから取得します.</li>
  </ul>

  <h2 id="install">インストール</h2>

  <p>
    今はMarketplaceに公開していないため、リポジトリからビルドしてインストールします.<br>
    まず、言語サーバーのjarを作ります.
  </p>

  <pre><code class="language-bash">{{ codeServer }}</code></pre>

  <h3>VS Code</h3>

  <pre><code class="language-bash">{{ codeVsCode }}</code></pre>

  <p>
    Scalaのハイライトには、Scala Syntax (official) の拡張を使います(自動でインストールされます).
  </p>

  <h3>IntelliJ IDEA</h3>

  <pre><code class="language-bash">{{ codeIntelliJ }}</code></pre>

  <p>
    Settings → Plugins → ⚙ → Install Plugin from Disk... で、作ったzipを選び、<br>
    IntelliJを再起動します.<br>
    IntelliJ 2025.2以降に対応しています.
  </p>

  <h2 id="update">更新</h2>

  <p>
    リポジトリを更新したときは、インストールと同じ手順でビルドし直して、入れ直してください.<br>
    言語サーバーを変更した場合は、先に <code>sbt languageServer/assembly</code> を実行します.<br>
    ハイライトの文法は、エディタを再起動しないと読み直されません.
  </p>

  <h2 id="limitations">制限事項</h2>

  <ul>
    <li>
      ライブラリの定義へのジャンプとドキュメントの表示には、ライブラリのソースjarを使います.<br>
      初回はMaven Centralなどから取得し、<code>~/.cache/scalactive/sources/</code> に展開します.<br>
      ソースjarが公開されていないライブラリや、JDKのクラスには使えません.
    </li>
    <li>
      自動importの候補は、クラスパスのjarとプロジェクトのクラス(object・traitを含む)だけです.<br>
      JDKのクラス、拡張メソッド、トップレベルの関数は出ません.
    </li>
    <li>
      VS Codeでは、<code>// language=</code> に指定できるのは scala・html・css だけで、色付けだけです.<br>
      文字列の中では補完やエラーの表示は使えません.
    </li>
  </ul>

  <p>
    ビルドやテストの詳細は、リポジトリの <code>editors/README.md</code> を参照してください.
  </p>
</template>
