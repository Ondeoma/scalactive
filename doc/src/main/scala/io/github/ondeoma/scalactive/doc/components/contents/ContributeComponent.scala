package io.github.ondeoma.scalactive.doc.components.contents

import io.github.ondeoma.scalactive.components.*
import org.scalajs.dom.*

object ContributeComponent extends StatelessComponent {

  def genHtml(implicit cm: ComponentManager): HTML = {
    // language=html
    s"""<h1>コントリビュート</h1>
       |
       |<p>
       |  大歓迎です. OSS管理が不慣れなので色々教えてください.<br>   
       |</p>
       |
       |<p>
       |  不具合・要望・提案などは、GitHub Issueとして起票いただけると幸いです.
       |</p>
       |
       |<p>
       |  プルリクエストはdevelopブランチ向けに作成いただけると幸いです.
       |</p>
       |
       |<h2>テスト</h2>
       |
       |<pre><code class="language-shell">npm install                  # coreのテストで使うjsdom(リポジトリ直下で実行)
       |sbt core/test                # StRuntime(.stの生成コードが使うヘルパー)
       |sbt compiler/test            # .stのパーサー/コード生成器
       |sbt sbtScalactive/scripted   # sbtプラグイン
       |sbt languageServer/test      # .stの言語サーバー(補完・型エラーなど)</code></pre>
       |
       |<p>
       |  エディタ拡張(editors/)のテスト方法は、リポジトリの <code>editors/README.md</code> を参照してください.
       |</p>
       |
       |""".stripMargin
  }
  
}
