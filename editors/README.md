# エディタ対応(.st)

`.st`(単一ファイルコンポーネント)のシンタックスハイライトと、言語サーバー(LSP)による補完・型エラー・ホバー・定義ジャンプを、
VS CodeとIntelliJ IDEAに提供します.

- ハイライト: 1つのTextMate文法(`vscode/syntaxes/scalactive.tmLanguage.json`)を両方で共有しています.
- 言語サーバー: リポジトリ直下の `language-server/`(sbtの `languageServer` プロジェクト)のjarを、両方の拡張に同梱します.

| ディレクトリ | 内容 |
| --- | --- |
| `vscode/` | VS Code拡張. この形式のまま、IntelliJのTextMateバンドルとしても使います. |
| `intellij/` | IntelliJプラグイン. `vscode/` の文法と、埋め込み用のScala文法をTextMateバンドルとして同梱します. |
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
- テンプレートのHTMLの部分(タグ名・属性名・スロット名)の補完は `TemplateCompletion` が行います.
  書きかけでコンパイルできない `.st` でも動くよう、コンパイルせずにテキストを走査して文脈を求めます.
  子コンポーネントのpropsは、同じディレクトリの `.st` の `case class Props(...)` から読み取ります(`StCompiler.propsOf`).
- 定義ジャンプは、同じ `.st` 内に加えて、プロジェクトのソースディレクトリにある `.scala` / `.st` を名前で探します.
  子コンポーネントのタグ名・属性名からは、同じディレクトリの `.st` のProps(のパラメータ)へジャンプします(`TemplateNavigation`).
- 整形(ドキュメントのフォーマット)は `compiler/` の `StFormatter` が行います.
  テンプレートは入れ子に合わせてインデントし直し、`<script>` / `<style>` は前後の空行と行末の空白だけを整えます.
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
