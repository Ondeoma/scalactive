# Scalactive Single File Component (.st)

[Scalactive](https://ondeoma.github.io/scalactive/) の単一ファイルコンポーネント(`.st`)の言語サポートです.

## シンタックスハイライト

- `<script lang="scala">` : Scalaとしてハイライト
- `<style>` : CSSとしてハイライト
- `<template>` : HTMLとしてハイライトし、以下の中身をScalaとしてハイライト
  - `{{ expr }}`
  - `v-if` / `v-else-if` / `v-show` / `v-model` / `v-for`
  - `:attr="expr"` / `v-bind:attr="expr"`
  - `@event.modifier="stmt"` / `v-on:event="stmt"`
- 大文字で始まるタグ(子コンポーネント)を区別して表示

Scalaのハイライトには [Scala Syntax (official)](https://marketplace.visualstudio.com/items?itemName=scala-lang.scala) 拡張機能を利用します(自動でインストールされます).

## 言語サーバー

上記のScalaの部分で、次の機能が使えます.

- 補完(テンプレートのHTMLの部分では、コンポーネント名・子コンポーネントのprops・ディレクティブ・スロット名・HTMLの標準のタグ・属性・属性値も補完します)
- 型エラーなどの表示
- 子コンポーネントのタグの検査(未知のprop・スロット、必須のpropの不足を警告)
- ホバーでの型の表示(子コンポーネントのタグではPropsの一覧、属性ではpropの型、HTMLのタグ・属性では説明)、引数のヒント
- 定義へのジャンプ(同じ`.st`内と、プロジェクト内の `.scala` / `.st`. 子コンポーネントのタグ名・属性名からは、その `.st` のPropsへ)
- ドキュメントのフォーマット(テンプレートのインデントを入れ子に合わせて整え、行末の空白などを取り除きます)

使うには:

1. sbtプロジェクトで `ScalactivePlugin` を有効にし、`sbt compile` を1度実行します
   (クラスパスなどが `target/scalactive-lsp.json` に書き出されます).
2. Java 11以降が必要です. `scalactive.languageServer.javaHome`、`JAVA_HOME`、`PATH` の順に探します.

初回はScalaのpresentation compilerをMaven Centralから取得します.
sbtで再コンパイルすると、他のファイルの変更が反映されます.

### 設定

| 設定 | 既定値 | 内容 |
| --- | --- | --- |
| `scalactive.languageServer.enabled` | `true` | 言語サーバーを使う |
| `scalactive.languageServer.javaHome` | `""` | 言語サーバーを動かすJDK |
| `scalactive.languageServer.jvmOptions` | `["-Xss4m", "-Xmx2G"]` | 言語サーバーのJVMオプション |
| `scalactive.trace.server` | `off` | 通信のログ |

コマンド `Scalactive: Restart Language Server` で再起動できます.
