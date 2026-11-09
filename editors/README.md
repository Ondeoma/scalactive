# エディタ対応(.st)

`.st`(単一ファイルコンポーネント)のシンタックスハイライトと、言語サーバー(LSP)による補完・型エラー・ホバー・定義ジャンプを、
VS CodeとIntelliJ IDEAに提供します.

- ハイライト: 1つのTextMate文法(`vscode/syntaxes/scalactive.tmLanguage.json`)を両方で共有しています.
- 言語サーバー: リポジトリ直下の `language-server/`(sbtの `languageServer` プロジェクト)のjarを、両方の拡張に同梱します.

| ディレクトリ | 内容 |
| --- | --- |
| `vscode/` | VS Code拡張. この形式のまま、IntelliJのTextMateバンドルとしても使います. |
| `intellij/` | IntelliJプラグイン. `.st` をHTMLの方言として解析し、色付けには `vscode/` の文法と埋め込み用のScala文法(TextMateバンドル)を使います. |
| `../language-server/` | 言語サーバー(Scala 2.12, lsp4j). |

## 言語サーバーの仕組み

1. `.st` を、sbtプラグインと同じコンパイラ(`compiler/`)で生成コードに変換します.
   生成コードには、ユーザーが書いたコードの位置の対応表(`SourceMap`)が付きます.
2. 生成コード上で、Scala 3のpresentation compiler(Metalsと同じもの)に補完・ホバー・定義を問い合わせ、
   Scala 3コンパイラの `InteractiveDriver` で型エラーを求めます.
3. 結果の位置を `SourceMap` で `.st` 上の位置に戻します.

- クラスパスやScalaのバージョンは、sbtプラグインがコンパイル時に書き出す `target/scalactive-lsp.json` から読みます.
  `.st` の親ディレクトリを遡ってこのファイルを探します.
- コンパイル完了時には `target/scalactive-lsp.stamp` が更新され、言語サーバーはクラスディレクトリを読み直します.
- presentation compilerは、対象プロジェクトのScalaバージョンのもの(`scala3-presentation-compiler_3`, 3.3.2以降)を
  coursierで取得し、別のクラスローダーで動かします(言語サーバー自身はScala 2.12のため).
  環境変数 `SCALACTIVE_PC_CLASSPATH` でjarを直接指定することもできます.
- テンプレートのHTMLの部分(タグ名・属性名・属性値・スロット名)の補完は `TemplateCompletion` が行います. HTMLの標準のタグ・属性・イベントの一覧は `HtmlData` にあります. テンプレートの定義ジャンプ・ホバーは `TemplateNavigation` が、子コンポーネントのpropsの検査は `PropsCheck` が、propの参照の検索・名前の変更は `PropRefs` が行います.
  書きかけでコンパイルできない `.st` でも動くよう、コンパイルせずにテキストを走査して文脈を求めます.
  子コンポーネントのpropsは、同じディレクトリの `.st` の `case class Props(...)` から読み取ります(`StCompiler.propsOf`).
- クイックフィックス(`textDocument/codeAction`)は `PropsCheck` の問題に付けた修正です. 未知のprop・スロットは編集距離の近いpropの名前に
  (`@close` や `v-model:x` などの書き方は `PropRefs.renamed` で合わせます)、足りない必須のpropはタグ名の直後に書き足します.
- インレイヒント(`textDocument/inlayHint`)は、presentation compilerの推論した型のヒントを生成コード全体について求め、
  ユーザーが書いたコードの位置に対応するものだけを返します. ラベルの中の生成コードへのリンクと、型を書き足す編集は除きます.
  Scala 3.3のpresentation compilerはラムダの引数にヒントを出さないため、`v-for` の変数とスコープ付きスロットの変数は、
  その位置のホバーから型を求めて出します.
- テンプレートのScalaの部分(`{{ }}`、`:attr`・`v-if`・`v-model`・`ref` などの属性値、`v-for` の `in` の右側、`@event` の文)も、
  まとめて1回scalafmtに掛けて整形します(`ScriptFormatter.formatTemplate`. 1行に収まるものだけ. 文字参照を含む値と複数行の値は対象外).
- コンポーネントの抽出(`ComponentExtractor`): テンプレートで完結したHTMLを選ぶと、コードアクション「Extract component」が出ます.
  実行すると、言語サーバーが同じディレクトリに `Extracted.st`(既にあれば `Extracted2.st` …)を書き、元の場所を `<Extracted :s="s" />` に置き換える編集を
  クライアントに頼みます(`workspace/executeCommand` → `workspace/applyEdit`). 名前はあとでファイル名の変更で変えてください.
  範囲の外で定義された値(`s`・`props`・外側の `v-for` の変数など)はpropsに、元のscriptで定義したものは `元の名前.名前` に書き換えます
  (名前の定義の位置と型はpresentation compilerに問い合わせます). 使っているクラスの `<style scoped>` のルールも写します.
- CSSのクラス(`CssClasses`): `class="..."` で同じファイルの `<style>` のクラスを補完し、クラス名からセレクタへジャンプできます.
  `<style scoped>` のクラスで、テンプレートとscriptのどこにも名前が出てこないものは、使われていないことを薄く表示します(Hint).
- 定義ジャンプは、同じ `.st` 内に加えて、プロジェクトのソースディレクトリにある `.scala` / `.st` を名前で探します.
  子コンポーネントのタグ名・属性名からは、同じディレクトリの `.st` のProps(のパラメータ)へジャンプします(`TemplateNavigation`).
- 整形(ドキュメントのフォーマット)は `compiler/` の `StFormatter` が行います.
  テンプレートは入れ子に合わせてインデントし直し、`<style>` のCSSは `CssFormatter` で整えます(1行のルールは1行のまま、
  複数行のルールは宣言を1行ずつ. `lang` 付きのstyleは対象外).
  `<script>` は言語サーバーが scalafmt で整形します(`ScriptFormatter`. 中身を `object` の本体として整形し、全体のインデントは保ちます).
  .stのディレクトリから上に `.scalafmt.conf` があればその設定を使い(`runner.dialect` がなければ `scala3`、`version` がなければ既定のものを補います)、
  なければ組み込みの設定(書いた人の改行の位置を保つ `newlines.source = keep` など. `ScriptFormatter.DefaultConfig`)を使います.
  scalafmt本体は設定のバージョンのものを初回にcoursierで取得します. 構文エラーなどでscalafmtが整形できないときは、
  `compiler/` の `ScalaSpacing` で行の途中の余分な空白だけを取り除きます(連続した空白を1つに、`(` `[` の直後と `)` `]` `,` の直前の空白をなしに.
  文字列・コメントの中は変えません).
  IntelliJでは「コードの整形」を言語サーバーだけに任せます(IntelliJのHTMLの整形は `{{ }}` などを崩すため使いません).
  言語サーバーが起動していないときは、`ScalactiveFormattingGuard` が何もせずに引き受け、通知を出します.
  ライブラリ(jar)内の定義、ドキュメント、自動importには対応していません.

```sh
sbt languageServer/test       # 言語サーバーのテスト(実際のpresentation compilerを使います)
sbt languageServer/assembly   # language-server/target/scala-2.12/scalactive-language-server.jar を作成
```

エディタ拡張をビルドする前に、`sbt languageServer/assembly` でjarを作っておいてください.

## VS Code拡張

```sh
cd editors/vscode
npm install
npm test          # 文法のトークン化テスト(shiki)
npm run test:e2e  # VS Codeを起動して言語サーバーを確認する結合テスト(事前に sbt languageServer/assembly doc/compile)
npm run package   # scalactive-st-<version>.vsix を作成(言語サーバーのjarを同梱)
code --install-extension scalactive-st-0.1.0.vsix
```

- Scalaのハイライトには [Scala Syntax (official)](https://marketplace.visualstudio.com/items?itemName=scala-lang.scala) を使います(`extensionDependencies` で自動インストールされます).
- 拡張のコード(`src/extension.js`)はesbuildで `dist/extension.js` にまとめます. 言語サーバーはJava 11以降で動かします
  (`scalactive.languageServer.javaHome` → `JAVA_HOME` → `PATH`).
- `npm run test:e2e` は、インストール済みのVS Code(macOSの既定の場所、または環境変数 `VSCODE_PATH`)を使います.
  なければ安定版をダウンロードします.
- Marketplaceへの公開は `npx vsce publish`(publisher `ondeoma` のアクセストークンが必要).

VS Code拡張のコマンド:

- **New Scalactive Component**(エクスプローラーのフォルダの右クリック・コマンドパレット): ひな形(`templates/*.st`)から.stを作ります.
- **Open Generated Scala Source**(.stの右クリック・コマンドパレット): .stから生成されたScalaのソースを開きます.
- stripMarginの文字列の中でEnterを押すと、次の行の同じ位置に `|` を補います(`src/margin.js`). `<script>` の中は埋め込みのScalaの設定が使われ、
  `onEnterRules` が効かないため、改行の入力を見て補います. 複数行の文字列は、VS Codeのインデントによる折りたたみで折りたためます.
- .stを開いたときに `target/scalactive-lsp.json` がなければ通知し、「sbt compile を実行」でタスクとして実行して、終わったら言語サーバーを再起動します.

## IntelliJプラグイン

```sh
cd editors/intellij
./gradlew test          # IntelliJ上でのハイライト・言語サーバーのテスト(事前に sbt languageServer/assembly doc/compile)
./gradlew buildPlugin   # build/distributions/scalactive-st-intellij-<version>.zip を作成
./gradlew runIde        # プラグインを入れたIntelliJを起動して確認
```

- インストール: Settings → Plugins → ⚙ → Install Plugin from Disk... でzipを選択.
- IntelliJ 2025.2以降に対応しています(`gradle.properties` の `platformVersion` と `build.gradle.kts` の `sinceBuild`).
- IntelliJのTextMateプラグインにはScalaの文法が含まれないため、`intellij/bundles/scala/` に
  [scala/vscode-scala-syntax](https://github.com/scala/vscode-scala-syntax)(MIT)の文法を同梱しています.
  `.scala` ファイルには関連付けないので、Scalaプラグインとは競合しません.
- CSSの文法はIntelliJ同梱のTextMateバンドルを使います.
- `.st` は「Scalactive」のファイルタイプ(`ScalactiveLanguage`、HTMLの方言)として開かれます.
  - 解析はIntelliJのHTMLのものを使うため、テンプレートでHTMLのタグ・属性の補完、閉じタグの挿入、Emmet(Tabキーでの展開)、
    構造ビューが使え、`<style>` の中はCSS(補完・検査)として扱われます(CSSの機能にはCSSプラグインが必要で、IntelliJ IDEA Ultimateなどに含まれます).
  - `<script>` の中はJavaScriptとして扱わず、そのままのテキストにします(`ScalactiveLexer`).
    Scalaの文字列・コメントの中の `</script>` では `<script>` を閉じません(コンパイラーの `SfcParser` と同じ判定. `ScalaScan`).
  - `<script>` の中の複数行の三重引用符の文字列を折りたためます(`ScalactiveFoldingBuilder`. HTMLのタグの折りたたみに加えて登録しています).
  - stripMarginの文字列の中でEnterを押すと、次の行の同じ位置に `|` を補い、`|` の後ろのインデントも引き継ぎます(`ScalactiveEnterHandler`).
  - `<script>` の中の `// language=xxx` に続く三重引用符の文字列に、その言語を注入します(`ScalactiveLanguageInjector`).
    IntelliJが扱える言語ならどれでも指定でき(言語のIDか表示名. 大文字・小文字は区別しない)、色付け・補完・検査が効きます.
    `html` / `st` / `scalactive` は `.st` として注入します. `.stripMargin` が続く場合は行頭の余白と `|` を、
    補間子の付いた文字列では `$x`・`${...}` を除いて注入します. 言語が見つからなければ、TextMateの文法の色付けだけになります.
  - 色付けは解析とは別に、同梱のTextMateの文法で行います(`TextMateSyntaxHighlighterFactory` / `TextMateEditorHighlighterProvider` を登録).
  - ディレクティブ(`v-if`・`:attr`・`@event`・`#slot`・`ref`)、子コンポーネントのタグとその属性、`<script lang>`・`<style scoped>` は、
    HTMLの検査で「未知のタグ・属性」と警告しないようにしています(`ScalactiveInspectionSuppressor`. propの間違いは言語サーバーが警告します).
  - コメントの切り替え(Cmd+/)は、scriptでは `//`、styleでは `/* */`、templateではHTMLのコメントです(`ScalactiveCommentProvider`).
  - script・styleの中で改行したときは、前の行のインデントを引き継ぎます(閉じていない括弧の直後にそろえる、`{` `=` `=>` の後は1段深く、
    閉じ括弧の前は1段浅く. `ScalactiveLineIndentProvider`). HTMLの規則のままだと、中身がタグより1段深くなるためです.
    対になった括弧の間(`{|}` や `{ x =>|}`)でEnterを押すと、1段深い空の行を作って閉じ括弧を次の行に置き(`ScalactiveEnterHandler`)、
    空白だけの行で閉じ括弧を入力すると、対応する開き括弧の行のインデントにそろえます.
  - script・styleの中では `(` `[` `{` `"` を入力すると閉じる文字を補い、閉じる文字は上書きせずに通り過ぎ、空の組はBackspaceでまとめて消します
    (`ScalactiveBracketHandler`. 単語の直前では補いません). テンプレートでも、Scalaを書く部分(`{{ }}` と、ディレクティブ・`:attr`・`@event`・`#slot`
    の属性値)では同じように補います(属性値の中の `"` は、属性を `'` で囲んだときだけ).
- New → Scalactive Component で、.stのひな形(ふつう・Props付き・State付き)から作れます(`NewScalactiveComponentAction`).
  ひな形はVS Code拡張と同じ `vscode/templates/*.st` を、ビルド時に `fileTemplates/internal/` へ写して使います.
- `.st` のファイル名(コンポーネントの名前)を変えると、同じディレクトリの `.st` のテンプレートにあるそのタグも書き換えます
  (`ScalactiveRenameListenerProvider`. `.scala` からの参照と、別のパッケージへの移動は対象外).
- `.st` を開いたときに `target/scalactive-lsp.json` が見つからなければ、通知を出します. 通知の「sbt compile を実行」で、
  build.sbtのある最も外側のディレクトリで `sbt -batch compile` を実行し、終わったら言語サーバーを再起動します(`ScalactiveSbtCompile`).
- エディタ・プロジェクトビューの右クリックメニューと Navigate メニューの「Open Generated Scala Source」で、`.st` から生成された
  Scalaのソース(`scalactive-lsp.json` の `generatedDirectory` の下)を開けます(`OpenGeneratedSourceAction`).
  - 言語サーバーには `initializationOptions` で `{"htmlCompletion": false}` を渡し、HTMLの標準のタグ・属性・属性値の補完を
    IntelliJの補完と重ならないようにしています(子コンポーネント・prop・ディレクティブ・`:attr`・`@event` は言語サーバーが補完します).
- `.st` のファイルのアイコンは `intellij/src/main/resources/icons/st.svg`(ダークテーマ用は `st_dark.svg`)です.
- 言語サーバーはIntelliJのLSP API(2025.2以降のプラットフォームに含まれます)で起動し、IDEのJava(JBR)で動かします.
  jarはプラグインの `server/` に同梱されます.
- テストのサンドボックスの状態がおかしくなった場合は `.intellijPlatform/sandbox` を削除してください.
- JetBrains Marketplaceへの公開は `./gradlew publishPlugin`(トークンの設定が必要).

## 文法を変更したら

テンプレートの構文を変えた場合は、言語サーバーの位置の対応(`compiler/` の `Marks` / `SourceMap`)も確認してください.
ユーザーが書いたコードを生成コードに埋め込む箇所は、`marks.mark(...)` で囲む必要があります.

1. `vscode/syntaxes/scalactive.tmLanguage.json` を編集
2. `cd editors/vscode && npm test`
3. `cd editors/intellij && ./gradlew test`

属性値の中のScalaは、閉じ引用符の直前の識別子を先に照合しています(`#scala-before-quote`).
Scala文法の文字列補間(`id"..."`)が属性の閉じ引用符を飲み込むのを防ぐためです.
