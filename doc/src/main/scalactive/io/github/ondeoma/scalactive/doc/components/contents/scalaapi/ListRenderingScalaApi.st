<script lang="scala">
val codeForC = // language=scala
  s"""val list = RVList(List("A", "B", "C"))
     |val loop = forC(list) { implicit (cm, s, index) =>
     |  s\"\"\"<p>$${index}:$${s.v}</p>\"\"\"
     |}
     |
     |\"\"\"<div>$${loop}</div>\"\"\"
     |""".stripMargin

// サンプルコードと同じforCのデモ
val list = RVList(List("A", "B", "C"))

def demoForC(using cm: ComponentManager): HTML = forC(list) { implicit (cm, n, index) =>
  s"""<p>${index}:${n.v}</p>"""
}

val codeForEachC = // language=scala
  s"""val list = RVList(List("A", "B", "C"))
     |// 添字を使わない場合はforEachC
     |val loop = forEachC(list) { implicit (cm, s) =>
     |  s\"\"\"<p>$${s.v}</p>\"\"\"
     |}
     |""".stripMargin

val codeKey = // language=scala
  s"""case class Todo(id: Int, title: String)
     |val todos = RV(List(Todo(1, "A"), Todo(2, "B")))
     |// keyで行を対応付ける(Vueのv-forの:key)
     |val loop = forEachC(todos, key = _.id) { implicit (cm, t) =>
     |  s\"\"\"<p>$${t.title}</p>\"\"\"
     |}
     |""".stripMargin
</script>

<template>
  <h2 id="list">リストレンダリング</h2>

  <p>
    forC関数はvue.jsにおけるv-for機能に対応するものです.<br>
    ※文字列中のインライン記述は苦しいので、以下のような分けた記述を推奨します.
  </p>

  <pre><code class="language-scala">{{ codeForC }}</code></pre>

  <div class="rendered" v-html="demoForC"></div>

  <h3 id="list-forEachC">forEachC</h3>

  <p>
    添字を使わない場合は、forEachCを使うと高速です.<br>
    行の削除や並べ替えで位置が変わった行も、作り直さずに済むためです.<br>
    stファイル(.st)の <code>v-for</code> では、<br>
    添字の変数を書かなければ自動的にforEachCになります.
  </p>

  <pre><code class="language-scala">{{ codeForEachC }}</code></pre>

  <h3 id="list-key">key</h3>

  <p>
    <code>forC(list, key = ...)</code> / <code>forEachC(list, key = ...)</code> のように、<br>
    行を対応付けるキーを指定できます.<br>
    例えば <code>RV[List[Todo]]</code> を新しいcase classのリストで置き換えた場合、<br>
    キーを指定しなければ、値が変わった行はすべて作り直されます.<br>
    idをキーにすると、並べ替えなどで行を使い回せます.<br>
    キーが同じでも値が変わった行(RVListでは別のRVになった行)は作り直されます.<br>
    stファイル(.st)では、<br>
    <code>&lt;li v-for="t in todos" :key="t.id"&gt;</code> と書きます.
  </p>

  <pre><code class="language-scala">{{ codeKey }}</code></pre>

  <p>
    利用できるコレクションは以下です(forC・forEachC共通).
  </p>

  <ul>
    <li>・List[?]</li>
    <li>・RVList[?]</li>
    <li>・RMList[?, ?]</li>
    <li>・CRV[List[?]]</li>
    <li>・RV[List[?]]</li>
    <li>・ReactiveList[?]</li>
  </ul>
</template>
