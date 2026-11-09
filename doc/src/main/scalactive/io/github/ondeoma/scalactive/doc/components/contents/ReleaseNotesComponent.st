<script lang="scala">
import Router.*
</script>

<template>
  <h1>リリースノート</h1>

  <p>
    バージョンごとの変更点です.<br>
    0.1.15以前の変更は、
    <a href="https://github.com/Ondeoma/scalactive/tags" target="_blank" rel="noopener">GitHubのタグ</a> を参照してください.
  </p>

  <ul class="toc">
    <li><a @click='setHash("v0_2_0")'>0.2.0(未リリース)</a></li>
  </ul>

  <h2 id="v0_2_0">0.2.0(未リリース)</h2>

  <p>
    0.1.15からの変更です.<br>
    単一ファイルコンポーネント(.st)とエディタ対応、<br>
    Vueに近いコンポーネントの機能、描画の高速化が中心です.<br>
    描き直しのタイミングなど、動きが変わる点があるので、<br>
    先に「互換性に関わる変更」を確認してください.
  </p>

  <h3>互換性に関わる変更</h3>
  <ul>
    <li>
      描き直しのタイミング: <code>ScalactiveConfig.reloadTimerLazyDuration</code> の既定値を、<br>
      100msから0にしました.<br>
      0のときは、同じ処理の中での変更をまとめて、<br>
      処理の直後(マイクロタスク)に描き直します.<br>
      親と子の両方が描き直しを予約したら、親から描き直します.<br>
      以前のように最後の変更から待つには、<br>
      <code>ScalactiveConfig.reloadTimerLazyDuration = 100</code> を設定してください.<br>
      (<a @click='go("/reactive")'>リアクティビティ</a>の「画面に反映されるタイミング」)
    </li>
    <li>
      <code>RV</code> に前の値と等しい(<code>==</code>)値を代入しても、変更を知らせないようにしました.<br>
      <code>CRV</code>(<code>mapC</code>)も、計算し直した値が前と等しければ知らせません.<br>
      値を変えずに知らせたいときは <code>RV.trigger()</code> を使います.
    </li>
    <li>
      <code>ReactiveModel</code> は、フィールドの監視を(100ms待たずに)、<br>
      最初に監視されたときに始めるようにしました.
    </li>
    <li>
      <code>Router.go</code> は、pushStateの直後に <code>Router.href</code> を変えるようにしました<br>
      (以前は最初の確認まで最大50ms掛かっていました).
    </li>
    <li>
      <code>forC</code> / <code>forEachC</code> は、リストが変わったときに全行を作り直さず、<br>
      キーで対応付けた行を使い回すようにしました(差分更新).<br>
      行の中に状態を持たせている場合は、<br>
      <code>key</code> の指定(.stでは <code>:key</code>)を確認してください.
    </li>
    <li>
      描画の目印の属性(<code>data-cm-N</code>)が要素に残るようになりました.<br>
      目印のIDはUUIDから、コンポーネントごとの連番になりました.
    </li>
    <li>
      内部APIの変更: <code>ComponentController.reloadTimer</code> を削除しました.<br>
      描き直しの予約は <code>ReloadScheduler</code> が行います.
    </li>
  </ul>

  <h3>単一ファイルコンポーネント(.st)</h3>
  <ul>
    <li>
      <code>&lt;script lang="scala"&gt;</code>・<code>&lt;template&gt;</code>・<code>&lt;style scoped&gt;</code> を1つのファイルに書く、<br>
      Vueのような単一ファイルコンポーネントを追加しました.<br>
      sbtプラグイン(<code>ScalactivePlugin</code>)がScalaのソースに変換します.<br>
      (<a @click='go("/sfc")'>単一ファイルコンポーネント</a>)
    </li>
    <li>
      テンプレート: <code>&#123;&#123; &#125;&#125;</code>、<code>v-if</code> / <code>v-else-if</code> / <code>v-else</code>、<code>v-for</code>(<code>:key</code>)、<code>v-show</code>、<br>
      <code>v-model</code>(修飾子、子コンポーネントへの <code>v-model</code>)、<br>
      <code>v-html</code>、<code>:attr</code>(<code>:class</code> / <code>:style</code> のオブジェクト記法)、<br>
      <code>@event</code>(<code>.prevent</code> / <code>.stop</code> / <code>.self</code> / <code>.once</code> / <code>.capture</code> / <code>.passive</code>・キー修飾子)、<br>
      <code>ref</code>、<code>&lt;template&gt;</code>
    </li>
    <li>
      Props(<code>case class Props</code>)、スロット(デフォルト・名前付き・スコープ付き)、<br>
      イベント(<code>@close</code> → <code>onClose</code>)、<br>
      子コンポーネントのルート要素への <code>class</code> / <code>style</code>・属性の引き継ぎ
    </li>
    <li><code>def mounted</code> / <code>def unmounted</code> でライフサイクルフックを使えます.</li>
  </ul>

  <h3>エディタ対応(VS Code・IntelliJ)</h3>
  <p>
    リポジトリの <code>editors/</code> に、<br>
    VS Codeの拡張とIntelliJ IDEA(2025.2以降)のプラグインを追加しました.<br>
    どちらも同じ言語サーバーを使います.
  </p>
  <ul>
    <li>シンタックスハイライト(script・テンプレートの中のScala、style)</li>
    <li>
      Scalaの補完・型エラー・ホバー・引数のヒント・定義へのジャンプ<br>
      (Scala 3のpresentation compilerを使い、.st上の位置で表示)
    </li>
    <li>
      テンプレートの補完: 子コンポーネントのタグ・props・イベント・<code>v-model</code>・スロット名、<br>
      ディレクティブ、HTMLの標準のタグ・属性・イベント・属性値
    </li>
    <li>
      子コンポーネントのタグ・属性からPropsへのジャンプ、<br>
      ホバー(Propsの一覧・propの型、HTMLのタグ・属性の説明)
    </li>
    <li>
      子コンポーネントのタグの検査(未知のprop・スロット、必須のpropの不足)と、<br>
      そのクイックフィックス
    </li>
    <li>
      propの参照の検索と名前の変更<br>
      (属性・名前付きスロット・<code>@close</code> などの書き方に合わせて書き換え)
    </li>
    <li>
      推論した型のインレイヒント<br>
      (<code>v-for</code> の変数と、スコープ付きスロットの変数にも出ます)
    </li>
    <li>
      整形: テンプレートのインデント、<code>&lt;style&gt;</code> のCSS、<br>
      <code>&lt;script&gt;</code> のscalafmt(<code>.scalafmt.conf</code> があればその設定).<br>
      テンプレートの <code>&#123;&#123; &#125;&#125;</code> や属性値のScalaも、scalafmtで整えます.
    </li>
    <li>
      CSSのクラス: <code>class="..."</code> で、同じファイルの <code>&lt;style&gt;</code> のクラスを補完します.<br>
      クラス名からセレクタへジャンプでき、<br>
      使われていない <code>&lt;style scoped&gt;</code> のクラスは薄く表示します.
    </li>
    <li>
      コンポーネントの抽出: テンプレートで選んだ範囲を、新しい.stに切り出します.<br>
      範囲の外の値はpropsになり、元の場所は新しいコンポーネントのタグになります.
    </li>
    <li>
      新規作成のひな形、生成コードを開く操作、<br>
      sbtでのコンパイルの案内(<code>target/scalactive-lsp.json</code> がないとき)
    </li>
    <li>
      IntelliJ: テンプレートをHTMLとして扱い、<br>
      IntelliJのHTML・CSSの補完・Emmet・構造ビューが使えます.<br>
      ファイルのアイコン、ファイル名の変更に合わせたタグの書き換え、<br>
      script・styleの改行時のインデントと括弧の自動補完もあります.
    </li>
  </ul>

  <h3>コンポーネント</h3>
  <ul>
    <li>ライフサイクルフック: <code>onMounted</code> / <code>onUnmounted</code></li>
    <li>テンプレート参照: <code>ElementRef</code> / <code>ref</code></li>
    <li><code>Teleport</code>(<code>teleportC</code>): 別の場所(bodyの直下など)に描画する</li>
    <li>
      エラーの境界: <code>ErrorBoundary</code>(<code>errorBoundaryC</code>).<br>
      境界で受け止められなかった例外は、<br>
      <code>ScalactiveConfig.errorHandler</code> で受け取れます.
    </li>
    <li>非同期データ: <code>Await</code>(<code>awaitC</code>)、遅延読み込み: <code>asyncComponent</code></li>
    <li>スコープ付きスロット、子コンポーネントへの <code>class</code> / <code>style</code>・属性の引き継ぎ</li>
    <li>
      <code>forEachC</code>(添字を渡さない <code>forC</code>)と、<br>
      <code>forC</code> / <code>forEachC</code> の <code>key</code> 指定
    </li>
  </ul>

  <h3>ルーティング</h3>
  <ul>
    <li>
      ネストしたルート(<code>RouterComponent.nested</code>)と、<br>
      ナビゲーションガード(<code>Router.beforeEach</code> / <code>afterEach</code>)
    </li>
    <li>
      <code>RouterLink</code>(<code>linkC</code>): 現在の場所に応じて、<br>
      <code>router-link-active</code> / <code>router-link-exact-active</code> を付けるリンク
    </li>
    <li><code>Route</code> のルートごとのガード(<code>beforeEnter</code>)と <code>meta</code></li>
  </ul>
  <p>(<a @click='go("/routing")'>ルーティング</a>)</p>

  <h3>テスト・デバッグ</h3>
  <ul>
    <li>
      <code>TestKit</code>: コンポーネントを描画して操作・確認するヘルパー<br>
      (テストフレームワークに依存しません)
    </li>
    <li>
      <code>Devtools</code>: 描き直しのログ・描き直した部分の強調・<br>
      表示中のコンポーネントの数・監視の数
    </li>
  </ul>
  <p>(<a @click='go("/devtools")'>テスト・デバッグ</a>)</p>

  <h3>性能の改善</h3>
  <p>
    リポジトリの <code>bench/</code>(ヘッドレスChromeでの計測)で確かめた値です.<br>
    環境によって変わります.
  </p>
  <ul>
    <li>
      描画の目印をコンポーネントごとの連番にし、<br>
      同じ形の描画の解析結果を使い回すようにしました:<br>
      1,000行の作成 約79ms→約47ms、<br>
      全行の置き換え 約149ms→約66ms、<br>
      10,000行の作成 約2.1〜3.8秒→約0.45秒
    </li>
    <li>
      監視・イベントハンドラーのIDを連番にしました<br>
      (さらに、1,000行の作成 -22%、全行の置き換え -28%、10,000行の作成 -30%)
    </li>
    <li>
      全削除で、取り除いた要素のイベントリスナーを個別に外さないようにしました:<br>
      1,000行の全削除 約30ms→約3.6ms
    </li>
    <li><code>forC</code> の差分更新(上記)と、行のノードの持ち方の見直し</li>
    <li>
      <code>RVList</code> への代入・<code>update</code> の通知を1回にまとめました<br>
      (n件の代入がO(n²)でした)
    </li>
    <li>
      値が変わらない <code>RV</code> / <code>CRV</code> は知らせないので、<br>
      <code>count.mapC(_ &gt; 3)</code> のような条件の描き直しが減ります.
    </li>
  </ul>

  <h3>不具合の修正</h3>
  <ul>
    <li>描き直しのたびに、空のノードがDOMに残っていた</li>
    <li><code>RVList</code> / <code>RMList</code> で、要素の監視が正しく解除されなかった</li>
    <li><code>EventManager</code> のハンドラーが、コンポーネントの片付けで解除されなかった</li>
    <li><code>forC</code> の片付けで、後ろにある無関係なノードまで取り除くことがあった</li>
    <li>
      .stのscriptで、文字列やコメントの中の <code>def initState</code>・<code>case class Props</code>・<code>&lt;/script&gt;</code> を、<br>
      定義やscriptの終わりとして読んでいた
    </li>
  </ul>

  <h3>開発</h3>
  <ul>
    <li>
      性能計測の仕組み(<code>bench/</code>)を追加しました.<br>
      (<a @click='go("/contribute")'>コントリビュート</a>)
    </li>
    <li>
      このドキュメントのコンポーネントを、原則.stで書くようにしました.<br>
      (コードサンプルとして表示するScalaのコンポーネントは、Scalaのままです)<br>
      説明も.stの書き方を中心に書き直し、<br>
      Scalaだけで書く従来の書き方は「<a @click='go("/scalaApi")'>Scala API(従来の書き方)</a>」のページにまとめました.
    </li>
    <li>
      テストを追加しました:<br>
      compiler・core(jsdom)・言語サーバー・sbtプラグイン(scripted)・VS Code(e2e)・IntelliJ.
    </li>
  </ul>
</template>
