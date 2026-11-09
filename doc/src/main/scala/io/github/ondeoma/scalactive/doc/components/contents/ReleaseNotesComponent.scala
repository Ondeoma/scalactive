package io.github.ondeoma.scalactive.doc.components.contents

import io.github.ondeoma.scalactive.components.*

object ReleaseNotesComponent extends StatelessComponent {

  def genHtml(implicit cm: ComponentManager): HTML = {

    import Router.*

    def page(path: String, label: String): String = s"""<a ${evClick(go(path))}>$label</a>"""

    // language=html
    s"""<h1>リリースノート</h1>
       |
       |<p>
       |  バージョンごとの変更点です. 0.1.15以前の変更は
       |  <a href="https://github.com/Ondeoma/scalactive/tags" target="_blank" rel="noopener">GitHubのタグ</a> を参照してください.
       |</p>
       |
       |<ul class="toc">
       |  <li><a ${evClick(setHash("v0_2_0"))}>0.2.0(未リリース)</a></li>
       |</ul>
       |
       |<h2 id="v0_2_0">0.2.0(未リリース)</h2>
       |
       |<p>
       |  0.1.15からの変更です. 単一ファイルコンポーネント(.st)とエディタ対応、Vueに近いコンポーネントの機能、描画の高速化が中心です.
       |  描き直しのタイミングなど、動きが変わる点があるので、先に「互換性に関わる変更」を確認してください.
       |</p>
       |
       |<h3>互換性に関わる変更</h3>
       |<ul>
       |  <li>
       |    描き直しのタイミング: <code>ScalactiveConfig.reloadTimerLazyDuration</code> の既定値を100msから0にしました.
       |    0のときは、同じ処理の中での変更をまとめて、処理の直後(マイクロタスク)に描き直します. 親と子の両方が描き直しを予約したら、親から描き直します.
       |    以前のように最後の変更から待つには、<code>ScalactiveConfig.reloadTimerLazyDuration = 100</code> を設定してください.
       |    (${page("/reactive", "リアクティビティ")}の「画面に反映されるタイミング」)
       |  </li>
       |  <li>
       |    <code>RV</code> に前の値と等しい(<code>==</code>)値を代入しても、変更を知らせないようにしました. <code>CRV</code>(<code>mapC</code>)も、計算し直した値が前と等しければ知らせません.
       |    値を変えずに知らせたいときは <code>RV.trigger()</code> を使います.
       |  </li>
       |  <li><code>ReactiveModel</code> は、フィールドの監視を(100ms待たずに)最初に監視されたときに始めるようにしました.</li>
       |  <li><code>Router.go</code> は、pushStateの直後に <code>Router.href</code> を変えるようにしました(以前は最初の確認まで最大50ms掛かっていました).</li>
       |  <li>
       |    <code>forC</code> / <code>forEachC</code> は、リストが変わったときに全行を作り直さず、キーで対応付けた行を使い回すようにしました(差分更新).
       |    行の中に状態を持たせている場合は、<code>key</code> の指定(.stでは <code>:key</code>)を確認してください.
       |  </li>
       |  <li>描画の目印の属性(<code>data-cm-N</code>)が要素に残るようになりました. 目印のIDはUUIDから、コンポーネントごとの連番になりました.</li>
       |  <li>内部APIの変更: <code>ComponentController.reloadTimer</code> を削除しました. 描き直しの予約は <code>ReloadScheduler</code> が行います.</li>
       |</ul>
       |
       |<h3>単一ファイルコンポーネント(.st)</h3>
       |<ul>
       |  <li>
       |    <code>&lt;script lang="scala"&gt;</code>・<code>&lt;template&gt;</code>・<code>&lt;style scoped&gt;</code> を1つのファイルに書く、Vueのような単一ファイルコンポーネントを追加しました.
       |    sbtプラグイン(<code>ScalactivePlugin</code>)がScalaのソースに変換します. (${page("/sfc", "単一ファイルコンポーネント")})
       |  </li>
       |  <li>
       |    テンプレート: <code>{{ }}</code>、<code>v-if</code> / <code>v-else-if</code> / <code>v-else</code>、<code>v-for</code>(<code>:key</code>)、<code>v-show</code>、<code>v-model</code>(修飾子、子コンポーネントへの <code>v-model</code>)、
       |    <code>v-html</code>、<code>:attr</code>(<code>:class</code> / <code>:style</code> のオブジェクト記法)、<code>@event</code>(<code>.prevent</code> / <code>.stop</code> / <code>.self</code> / <code>.once</code> / <code>.capture</code> / <code>.passive</code>・キー修飾子)、
       |    <code>ref</code>、<code>&lt;template&gt;</code>
       |  </li>
       |  <li>Props(<code>case class Props</code>)、スロット(デフォルト・名前付き・スコープ付き)、イベント(<code>@close</code> → <code>onClose</code>)、子コンポーネントのルート要素への <code>class</code> / <code>style</code>・属性の引き継ぎ</li>
       |  <li><code>def mounted</code> / <code>def unmounted</code> でライフサイクルフックを使えます.</li>
       |</ul>
       |
       |<h3>エディタ対応(VS Code・IntelliJ)</h3>
       |<p>リポジトリの <code>editors/</code> に、VS Codeの拡張とIntelliJ IDEA(2025.2以降)のプラグインを追加しました. どちらも同じ言語サーバーを使います.</p>
       |<ul>
       |  <li>シンタックスハイライト(script・テンプレートの中のScala、style)</li>
       |  <li>Scalaの補完・型エラー・ホバー・引数のヒント・定義へのジャンプ(Scala 3のpresentation compilerを使い、.st上の位置で表示)</li>
       |  <li>テンプレートの補完: 子コンポーネントのタグ・props・イベント・<code>v-model</code>・スロット名、ディレクティブ、HTMLの標準のタグ・属性・イベント・属性値</li>
       |  <li>子コンポーネントのタグ・属性からPropsへのジャンプ、ホバー(Propsの一覧・propの型、HTMLのタグ・属性の説明)</li>
       |  <li>子コンポーネントのタグの検査(未知のprop・スロット、必須のpropの不足)と、そのクイックフィックス</li>
       |  <li>propの参照の検索と名前の変更(属性・名前付きスロット・<code>@close</code> などの書き方に合わせて書き換え)</li>
       |  <li>推論した型のインレイヒント</li>
       |  <li>整形: テンプレートのインデント、<code>&lt;style&gt;</code> のCSS、<code>&lt;script&gt;</code> のscalafmt(<code>.scalafmt.conf</code> があればその設定)</li>
       |  <li>
       |    IntelliJ: テンプレートをHTMLとして扱い、IntelliJのHTML・CSSの補完・Emmet・構造ビューが使えます.
       |    ファイルのアイコン、新規作成のひな形、ファイル名の変更に合わせたタグの書き換え、生成コードを開く操作、
       |    sbtでのコンパイルの案内、script・styleの改行時のインデントと括弧の自動補完もあります.
       |  </li>
       |</ul>
       |
       |<h3>コンポーネント</h3>
       |<ul>
       |  <li>ライフサイクルフック: <code>onMounted</code> / <code>onUnmounted</code></li>
       |  <li>テンプレート参照: <code>ElementRef</code> / <code>ref</code></li>
       |  <li><code>Teleport</code>(<code>teleportC</code>): 別の場所(bodyの直下など)に描画する</li>
       |  <li>エラーの境界: <code>ErrorBoundary</code>(<code>errorBoundaryC</code>). 境界で受け止められなかった例外は <code>ScalactiveConfig.errorHandler</code> で受け取れます.</li>
       |  <li>非同期データ: <code>Await</code>(<code>awaitC</code>)、遅延読み込み: <code>asyncComponent</code></li>
       |  <li>スコープ付きスロット、子コンポーネントへの <code>class</code> / <code>style</code>・属性の引き継ぎ</li>
       |  <li><code>forEachC</code>(添字を渡さない <code>forC</code>)と、<code>forC</code> / <code>forEachC</code> の <code>key</code> 指定</li>
       |</ul>
       |
       |<h3>ルーティング</h3>
       |<ul>
       |  <li>ネストしたルート(<code>RouterComponent.nested</code>)と、ナビゲーションガード(<code>Router.beforeEach</code> / <code>afterEach</code>)</li>
       |  <li><code>RouterLink</code>(<code>linkC</code>): 現在の場所に応じて <code>router-link-active</code> / <code>router-link-exact-active</code> を付けるリンク</li>
       |  <li><code>Route</code> のルートごとのガード(<code>beforeEnter</code>)と <code>meta</code></li>
       |</ul>
       |<p>(${page("/routing", "ルーティング")})</p>
       |
       |<h3>テスト・デバッグ</h3>
       |<ul>
       |  <li><code>TestKit</code>: コンポーネントを描画して操作・確認するヘルパー(テストフレームワークに依存しません)</li>
       |  <li><code>Devtools</code>: 描き直しのログ・描き直した部分の強調・表示中のコンポーネントの数・監視の数</li>
       |</ul>
       |<p>(${page("/devtools", "テスト・デバッグ")})</p>
       |
       |<h3>性能の改善</h3>
       |<p>リポジトリの <code>bench/</code>(ヘッドレスChromeでの計測)で確かめた値です. 環境によって変わります.</p>
       |<ul>
       |  <li>描画の目印をコンポーネントごとの連番にし、同じ形の描画の解析結果を使い回すようにしました: 1,000行の作成 約79ms→約47ms、全行の置き換え 約149ms→約66ms、10,000行の作成 約2.1〜3.8秒→約0.45秒</li>
       |  <li>監視・イベントハンドラーのIDを連番にしました(さらに、1,000行の作成 -22%、全行の置き換え -28%、10,000行の作成 -30%)</li>
       |  <li>全削除で、取り除いた要素のイベントリスナーを個別に外さないようにしました: 1,000行の全削除 約30ms→約3.6ms</li>
       |  <li><code>forC</code> の差分更新(上記)と、行のノードの持ち方の見直し</li>
       |  <li><code>RVList</code> への代入・<code>update</code> の通知を1回にまとめました(n件の代入がO(n²)でした)</li>
       |  <li>値が変わらない <code>RV</code> / <code>CRV</code> は知らせないので、<code>count.mapC(_ &gt; 3)</code> のような条件の描き直しが減ります.</li>
       |</ul>
       |
       |<h3>不具合の修正</h3>
       |<ul>
       |  <li>描き直しのたびに、空のノードがDOMに残っていた</li>
       |  <li><code>RVList</code> / <code>RMList</code> で、要素の監視が正しく解除されなかった</li>
       |  <li><code>EventManager</code> のハンドラーが、コンポーネントの片付けで解除されなかった</li>
       |  <li><code>forC</code> の片付けで、後ろにある無関係なノードまで取り除くことがあった</li>
       |</ul>
       |
       |<h3>開発</h3>
       |<ul>
       |  <li>性能計測の仕組み(<code>bench/</code>)を追加しました. (${page("/contribute", "コントリビュート")})</li>
       |  <li>テストを追加しました: compiler・core(jsdom)・言語サーバー・sbtプラグイン(scripted)・VS Code(e2e)・IntelliJ.</li>
       |</ul>
       |""".stripMargin
  }

}
