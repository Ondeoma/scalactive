<template>
  <h1>コントリビュート</h1>

  <p>
    大歓迎です. OSS管理が不慣れなので色々教えてください.<br>
  </p>

  <p>
    不具合・要望・提案などは、GitHub Issueとして起票いただけると幸いです.
  </p>

  <p>
    プルリクエストはdevelopブランチ向けに作成いただけると幸いです.
  </p>

  <h2>テスト</h2>

  <pre><code class="language-shell">npm install                  # coreのテストで使うjsdom(リポジトリ直下で実行)
sbt core/test                # StRuntime(.stの生成コードが使うヘルパー)
sbt compiler/test            # .stのパーサー/コード生成器
sbt sbtScalactive/scripted   # sbtプラグイン
sbt languageServer/test      # .stの言語サーバー(補完・型エラーなど)</code></pre>

  <p>
    エディタ拡張(editors/)のテスト方法は、<br>
    リポジトリの <code>editors/README.md</code> を参照してください.
  </p>

  <h2>性能計測</h2>

  <p>
    描画やリアクティブ値の性能に関わる変更では、<br>
    変更の前後でベンチマークを実行して比較してください.<br>
    ヘッドレスのChrome(インストール済みのGoogle Chrome)で計測します.<br>
    詳しくは <code>bench/README.md</code> を参照してください.
  </p>

  <pre><code class="language-bash">sbt bench/fullLinkJS
cd bench && npm install
node run.mjs --out results/before.json      # 変更前
node run.mjs --compare results/before.json  # 変更後(sbt bench/fullLinkJSしてから)</code></pre>
</template>
