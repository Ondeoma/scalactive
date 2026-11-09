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
- 定義ジャンプは、同じ `.st` 内に加えて、プロジェクトのソースディレクトリにある `.scala` / `.st` を名前で探します.
  子コンポーネントのタグ名・属性名からは、同じディレクトリの `.st` のProps(のパラメータ)へジャンプします(`TemplateNavigation`).
- 整形(ドキュメントのフォーマット)は `compiler/` の `StFormatter` が行います.
  テンプレートは入れ子に合わせてインデントし直し、`<style>` のCSSは `CssFormatter` で整えます(1行のルールは1行のまま、
  複数行のルールは宣言を1行ずつ. `lang` 付きのstyleは対象外). `<script>` は前後の空行と行末の空白だけを整えます.
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
  - 色付けは解析とは別に、同梱のTextMateの文法で行います(`TextMateSyntaxHighlighterFactory` / `TextMateEditorHighlighterProvider` を登録).
  - ディレクティブ(`v-if`・`:attr`・`@event`・`#slot`・`ref`)、子コンポーネントのタグとその属性、`<script lang>`・`<style scoped>` は、
    HTMLの検査で「未知のタグ・属性」と警告しないようにしています(`ScalactiveInspectionSuppressor`. propの間違いは言語サーバーが警告します).
  - コメントの切り替え(Cmd+/)は、scriptでは `//`、styleでは `/* */`、templateではHTMLのコメントです(`ScalactiveCommentProvider`).
  - script・styleの中で改行したときは、前の行のインデントを引き継ぎます(閉じていない括弧の直後にそろえる、`{` `=` `=>` の後は1段深く、
    閉じ括弧の前は1段浅く. `ScalactiveLineIndentProvider`). HTMLの規則のままだと、中身がタグより1段深くなるためです.
  - script・styleの中では `(` `[` `{` `"` を入力すると閉じる文字を補い、閉じる文字は上書きせずに通り過ぎ、空の組はBackspaceでまとめて消します
    (`ScalactiveBracketHandler`. 単語の直前では補いません). テンプレートでも、Scalaを書く部分(`{{ }}` と、ディレクティブ・`:attr`・`@event`・`#slot`
    の属性値)では同じように補います(属性値の中の `"` は、属性を `'` で囲んだときだけ).
- New → Scalactive Component で、.stのひな形(ふつう・Props付き・State付き)から作れます(`NewScalactiveComponentAction`、
  `src/main/resources/fileTemplates/internal/`).
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
