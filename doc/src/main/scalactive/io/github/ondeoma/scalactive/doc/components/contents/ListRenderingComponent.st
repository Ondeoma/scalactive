<script lang="scala">
import Router.*
import io.github.ondeoma.scalactive.doc.components.contents.list.{ForBasic, ForIndex, ForKey, TemplateFor}

val codeFor = // language=html
  """<script lang="scala">
     |case class State(fruits: RVList[String] = RVList(List("りんご", "みかん")),
     |                 input: RV[String] = RV(""))
     |
     |def initState() = State()
     |
     |def add(s: State): Unit = {
     |  if (s.input.v.trim.nonEmpty) {
     |    s.fruits.add(s.input.v.trim)
     |    s.input := ""
     |  }
     |}
     |</script>
     |
     |<template>
     |  <p>
     |    <input type="text" v-model="s.input" @keydown.enter="add(s)">
     |    <button @click="add(s)">追加</button>
     |  </p>
     |  <ul>
     |    <li v-for="f in s.fruits">{{ f }}</li>
     |  </ul>
     |</template>
     |""".stripMargin

val codeIndex = // language=html
  """<script lang="scala">
     |case class State(fruits: RVList[String] = RVList(List("りんご", "みかん", "ぶどう")))
     |
     |def initState() = State()
     |</script>
     |
     |<template>
     |  <ul>
     |    <li v-for="(f, i) in s.fruits">
     |      {{ i + 1 }}. {{ f }}
     |      <button @click="s.fruits.rm(i)">削除</button>
     |    </li>
     |  </ul>
     |  <button @click='s.fruits := List("りんご", "みかん", "ぶどう")'>元に戻す</button>
     |</template>
     |""".stripMargin

val codeKey = // language=html
  """<script lang="scala">
     |case class Todo(id: Int, title: String)
     |
     |case class State(todos: RV[List[Todo]] = RV(List(Todo(1, "買い物"), Todo(2, "掃除"), Todo(3, "洗濯"))))
     |
     |def initState() = State()
     |</script>
     |
     |<template>
     |  <button @click="s.todos := s.todos.v.reverse">逆順にする</button>
     |  <ul>
     |    <li v-for="t in s.todos" :key="t.id">
     |      {{ t.id }}: {{ t.title }}
     |      <input type="text" placeholder="入力した値は行と一緒に動きます">
     |    </li>
     |  </ul>
     |</template>
     |""".stripMargin

val codeTemplateFor = // language=html
  """<script lang="scala">
     |case class Term(word: String, desc: String)
     |
     |val terms = List(Term("RV", "書き換えられるリアクティブな値"),
     |                 Term("CRV", "他の値から計算されるリアクティブな値"))
     |</script>
     |
     |<template>
     |  <dl>
     |    <template v-for="t in terms">
     |      <dt>{{ t.word }}</dt>
     |      <dd>{{ t.desc }}</dd>
     |    </template>
     |  </dl>
     |</template>
     |""".stripMargin
</script>

<template>
  <h1>リストレンダリング</h1>

  <p>
    v-forはvue.jsにおけるv-forと同じく、<br>
    リストの要素ごとに要素を繰り返し描画します.
  </p>

  <ul class="toc">
    <li><a @click='setHash("vFor")'>v-for</a></li>
    <li><a @click='setHash("index")'>添字</a></li>
    <li><a @click='setHash("collections")'>利用できるコレクション</a></li>
    <li><a @click='setHash("update")'>リストが変わったとき</a></li>
    <li><a @click='setHash("key")'>key</a></li>
    <li><a @click='setHash("templateFor")'>&lt;template v-for&gt;</a></li>
  </ul>

  <h2 id="vFor">v-for</h2>

  <p>
    <code>v-for="item in list"</code> と書きます.<br>
    リストがReactiveの場合は、変化に追従して描き直します.
  </p>

  <pre><code class="language-st">{{ codeFor }}</code></pre>

  <div class="rendered">
    <ForBasic />
  </div>

  <p>
    v-ifと同じ要素には書けません.<br>
    行ごとに条件を付ける場合は、<code>&lt;template v-for&gt;</code> の中にv-ifを書いてください.
  </p>

  <h2 id="index">添字</h2>

  <p>
    <code>v-for="(item, i) in list"</code> と書くと、<br>
    0から始まる添字(Int)も受け取れます.
  </p>

  <pre><code class="language-st">{{ codeIndex }}</code></pre>

  <div class="rendered">
    <ForIndex />
  </div>

  <h2 id="collections">利用できるコレクション</h2>

  <p>
    利用できるコレクションと、itemの型は以下です.
  </p>

  <ul>
    <li>・List[A] : A</li>
    <li>・RVList[A] : RV[A]</li>
    <li>・RMList[A, RM] : RM(ReactiveModel)</li>
    <li>・CRV[List[A]] : A</li>
    <li>・RV[List[A]] : A</li>
    <li>・ReactiveList[A] : A</li>
  </ul>

  <p>
    RVList・RMListでは、itemがReactiveになります.<br>
    そのため、行の値を書き換えたり、<br>
    <code>v-model="item"</code> で入力欄と同期したりできます.<br>
    詳しくは<a @click='go("/reactive")'>リアクティブ</a>のページを参照してください.
  </p>

  <h2 id="update">リストが変わったとき</h2>

  <p>
    リストが変わったときは全行を作り直さず、変わった部分だけを画面に反映します.<br>
    行は、RVList・RMListでは要素のオブジェクト、<br>
    それ以外のリストでは要素の値で対応付けられます.<br>
    消えた行は取り除かれ、新しい行は作られます.<br>
    残った行は(必要なら位置を動かして)そのまま使われます.<br>
    ただし、以下の行は作り直されます.
  </p>

  <ul>
    <li>・RVListへの代入などで、値が変わった行</li>
    <li>
      ・添字を受け取る場合、位置(添字)が変わった行.<br>
      行のHTMLに埋め込んだ添字が古くならないようにするためです.
    </li>
  </ul>

  <p>
    添字を使わない場合は、添字の変数を書かない方が高速です.<br>
    行の削除や並べ替えで位置が変わった行も、作り直さずに済むためです.
  </p>

  <h2 id="key">key</h2>

  <p>
    <code>:key="式"</code> で、行を対応付けるキーを指定できます.<br>
    式はitemから計算します(例: <code>:key="t.id"</code>).<br>
    例えば <code>RV[List[Todo]]</code> を新しいcase classのリストで置き換えた場合、<br>
    キーを指定しなければ、値が変わった行はすべて作り直されます.<br>
    idをキーにすると、並べ替えなどで行を使い回せます.<br>
    キーが同じでも値が変わった行(RVListでは別のRVになった行)は作り直されます.
  </p>

  <p>
    以下の例では、入力欄に何か入力してから逆順にしてみてください.<br>
    行が使い回されるので、入力した値も行と一緒に動きます.
  </p>

  <pre><code class="language-st">{{ codeKey }}</code></pre>

  <div class="rendered">
    <ForKey />
  </div>

  <h2 id="templateFor">&lt;template v-for&gt;</h2>

  <p>
    複数の要素をまとめて繰り返す場合は、<code>&lt;template&gt;</code> で囲みます.<br>
    <code>&lt;template&gt;</code> 自体は出力されず、中身だけが描画されます.<br>
    <code>:key</code> も指定できます.
  </p>

  <pre><code class="language-st">{{ codeTemplateFor }}</code></pre>

  <div class="rendered">
    <TemplateFor />
  </div>

  <p>
    従来のScalaでの書き方は<a @click='go("/scalaApi")'>Scala API(従来の書き方)</a>を参照してください.
  </p>
</template>
