<script lang="scala">
import Router.*
import io.github.ondeoma.scalactive.doc.components.contents.directive.*

val codeBuiltin = // language=html
  """<div class="menu-wrap" v-click-outside="s.open := false">
    |  <button @click="s.open := !s.open.v">メニュー</button>
    |  <ul v-if="s.open" class="menu">...</ul>
    |</div>
    |
    |<p v-if="!s.editing.v">名前: {{ s.name }} <button @click="s.editing := true">編集</button></p>
    |<p v-else>名前: <input v-model="s.name" v-focus.select @keydown.enter="s.editing := false"></p>
    |
    |<li v-for="m in s.messages" :key="m.id" v-scroll-into-view.smooth="s.selected.mapC(_ == m.id)">...</li>
    |""".stripMargin

val codeCustom = // language=html
  """<script lang="scala">
    |// mounted と updated の両方で呼ぶ
    |val tint: Directive[String] = Directive((el, c) => el.style.color = c)
    |
    |// v-html で描いた中のコードを色付けする(中身が入れ替わった後に updated が呼ばれる)
    |val highlight: Directive[String] = Directive((el, _) => Prism.highlightAllUnder(el))
    |
    |// 3つのフックを個別に書く
    |val autoSize: Directive[Unit] = new Directive[Unit] {
    |  override def mounted(el: HTMLElement, value: Unit): Unit = ...
    |  override def unmounted(el: HTMLElement): Unit = ...
    |}
    |</script>
    |
    |<template>
    |  <span v-tint="s.color">色付きの文字</span>
    |  <article v-html="s.body" v-highlight="s.body"></article>
    |  <textarea v-auto-size></textarea>
    |</template>
    |""".stripMargin
</script>

<template>
  <h1>ディレクティブ</h1>

  <p>
    要素に付けて、DOMに挿入された後や値が変わった後の処理を行う仕組みです(Vue.jsのディレクティブ).<br>
    <code>setTimeout</code> で描画を待ったり、<code>document.querySelector</code> で要素を探したりする代わりに使えます.
  </p>

  <h2 id="Builtin">組み込みのディレクティブ</h2>

  <table>
    <tr><th>書き方</th><th>内容</th></tr>
    <tr><td><code>v-focus</code><br><code>v-focus="cond"</code></td><td>
      DOMに挿入されたとき(condが真なら)と、condが真に変わったときにフォーカスします.<br>
      <code>.select</code> を付けると、中の文字列を選択します.
    </td></tr>
    <tr><td><code>v-scroll-into-view</code><br><code>v-scroll-into-view="cond"</code></td><td>
      DOMに挿入されたとき(condが真なら)と、condが真に変わったときに、要素が見える位置までスクロールします.<br>
      修飾子: <code>.smooth</code>(なめらかに)、<code>.start</code>・<code>.center</code>・<code>.end</code>・<code>.nearest</code>(位置. 既定はnearest)
    </td></tr>
    <tr><td><code>v-click-outside="stmt"</code></td><td>
      要素の外を押したときに文を実行します(メニュー・ポップアップを閉じるなど). 文の中では <code>event</code> を参照できます.<br>
      押し始め(pointerdown)で判定するので、押した要素がその後の描き直しで消えても正しく判定できます.
    </td></tr>
  </table>

  <pre><code class="language-st">{{ codeBuiltin }}</code></pre>

  <div class="rendered">
    <DirectiveDemo />
  </div>

  <h2 id="Custom">独自のディレクティブ</h2>

  <p>
    組み込みでない <code>v-名前="値"</code> は、スコープにある <code>名前</code> の <code>Directive</code> として扱われます.<br>
    ケバブケースの名前はキャメルケースに変換します(<code>v-auto-size</code> → <code>autoSize</code>).<br>
    値を省略した場合は <code>()</code> を渡します(<code>Directive[Unit]</code>).
  </p>

  <ul>
    <li><code>mounted(el, value)</code>: 要素がDOMに挿入された後(onMountedと同じタイミング).</li>
    <li>
      <code>updated(el, value, oldValue)</code>: 値の式で読んだリアクティブ値が変わり、<br>
      その変更による描き直しが終わった後(<code>v-html</code> の中身も入れ替わった後).
    </li>
    <li><code>unmounted(el)</code>: 要素が片付けられたとき.</li>
  </ul>

  <pre><code class="language-st">{{ codeCustom }}</code></pre>

  <ul>
    <li>
      1つのDirectiveを複数の要素で使います.<br>
      要素ごとの状態は要素(dataset など)に持たせるか、<code>def</code> にして描画のたびに作ってください.
    </li>
    <li>子コンポーネントのタグには付けられません. 要素に付けてください.</li>
  </ul>
</template>
