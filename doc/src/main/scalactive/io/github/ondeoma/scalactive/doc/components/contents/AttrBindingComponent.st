<script lang="scala">
import Router.*
import io.github.ondeoma.scalactive.doc.components.contents.attr.{AttrBasic, AttrBoolean, ClassBinding, ModelInputs, ModelModifiers, StyleBinding}

val codeAttr = // language=html
  """<script lang="scala">
     |case class State(url: RV[String] = RV("https://www.scala-js.org/"))
     |
     |def initState() = State()
     |</script>
     |
     |<template>
     |  <p><input type="text" v-model="s.url" size="30"></p>
     |  <p><a :href="s.url" v-bind:title="s.url" target="_blank">リンク</a></p>
     |  <p><input type="text" :maxlength="10" placeholder="10文字まで"></p>
     |</template>
     |""".stripMargin

val codeBoolean = // language=html
  """<script lang="scala">
     |case class State(locked: RV[Boolean] = RV(true))
     |
     |def initState() = State()
     |</script>
     |
     |<template>
     |  <label><input type="checkbox" v-model="s.locked"> ロックする</label>
     |  <input type="text" :readonly="s.locked" value="編集できますか?">
     |  <button :disabled="s.locked">送信</button>
     |</template>
     |""".stripMargin

val codeClass = // language=html
  """<script lang="scala">
     |case class State(active: RV[Boolean] = RV(true),
     |                 size: RV[String] = RV("small"))
     |
     |def initState() = State()
     |</script>
     |
     |<template>
     |  <p>
     |    <label><input type="checkbox" v-model="s.active"> active</label>
     |    <select v-model="s.size">
     |      <option value="small">small</option>
     |      <option value="large">large</option>
     |    </select>
     |  </p>
     |  <!-- 文字列(空白区切りで複数指定できます) -->
     |  <p :class="s.size">文字列: sizeの値がクラスになります.</p>
     |  <!-- クラス名と条件の組 -->
     |  <p :class='"active" -> s.active'>組: activeの時だけ緑色になります.</p>
     |  <!-- オブジェクト記法(Map) -->
     |  <p :class='Map("active" -> s.active, "underline" -> true)'>Map: 下線は常に付きます.</p>
     |  <!-- 配列記法(文字列と組の混在). 静的なclass属性と併用できます -->
     |  <p class="box" :class='List("bold", "active" -> s.active)'>List: 枠と太字は常に付きます.</p>
     |</template>
     |
     |<style scoped>
     |.active { color: seagreen; }
     |.underline { text-decoration: underline; }
     |.bold { font-weight: bold; }
     |.small { font-size: 12px; }
     |.large { font-size: 18px; }
     |.box { border: 1px solid #999; padding: 4px; }
     |</style>
     |""".stripMargin

val codeStyle = // language=html
  """<script lang="scala">
     |case class State(color: RV[String] = RV("#ff6347"),
     |                 size: RV[Int] = RV(16),
     |                 bold: RV[Boolean] = RV(false))
     |
     |def initState() = State()
     |</script>
     |
     |<template>
     |  <p>
     |    <input type="color" v-model="s.color">
     |    <input type="number" v-model="s.size" style="width: 4em">px
     |    <label><input type="checkbox" v-model="s.bold"> 太字</label>
     |  </p>
     |  <!-- 文字列: style属性全体になります -->
     |  <p :style='s.color.mapC(c => "color: " + c)'>文字列で指定</p>
     |  <!-- オブジェクト記法(Map): プロパティごとに設定され、静的なstyle属性と併用できます -->
     |  <p style="border: 1px solid #999; padding: 4px" :style='Map("color" -> s.color, "fontSize" -> s.size.mapC(n => n.toString + "px"), "fontWeight" -> s.bold.mapC(b => if (b) "bold" else ""))'>Mapで指定</p>
     |</template>
     |""".stripMargin

val codeModel = // language=html
  """<script lang="scala">
     |case class State(name: RV[String] = RV("Taro"),
     |                 age: RV[Int] = RV(20),
     |                 memo: RV[String] = RV(""),
     |                 agree: RV[Boolean] = RV(false),
     |                 color: RV[String] = RV("red"),
     |                 tags: RV[List[String]] = RV(List("scala")))
     |
     |def initState() = State()
     |</script>
     |
     |<template>
     |  <p><input type="text" v-model="s.name"> → {{ s.name }}</p>
     |  <p><input type="number" v-model="s.age" style="width: 5em"> → 来年は{{ s.age.mapC(_ + 1) }}歳</p>
     |  <p><textarea v-model="s.memo"></textarea> → {{ s.memo }}</p>
     |  <p><label><input type="checkbox" v-model="s.agree"> 同意する</label> → {{ s.agree }}</p>
     |  <p>
     |    <label><input type="radio" name="model-color" value="red" v-model="s.color"> red</label>
     |    <label><input type="radio" name="model-color" value="blue" v-model="s.color"> blue</label>
     |    <select v-model="s.color">
     |      <option value="red">red</option>
     |      <option value="blue">blue</option>
     |    </select>
     |    → {{ s.color }}
     |  </p>
     |  <p>
     |    <label><input type="checkbox" value="scala" v-model="s.tags"> scala</label>
     |    <label><input type="checkbox" value="js" v-model="s.tags"> js</label>
     |    <label><input type="checkbox" value="css" v-model="s.tags"> css</label>
     |    → {{ s.tags.mapC(_.mkString(", ")) }}
     |  </p>
     |</template>
     |""".stripMargin

val codeModifiers = // language=html
  """<script lang="scala">
     |case class State(trimmed: RV[String] = RV(""),
     |                 lazyText: RV[String] = RV(""))
     |
     |def initState() = State()
     |</script>
     |
     |<template>
     |  <p><input type="text" v-model.trim="s.trimmed"> → [{{ s.trimmed }}]</p>
     |  <p><input type="text" v-model.lazy="s.lazyText"> → {{ s.lazyText }}</p>
     |</template>
     |""".stripMargin
</script>

<template>
  <h1>属性バインディング</h1>

  <p>
    HTML属性にリアクティブな値を設定できます.<br>
    値が変わると、要素を作り直さずにその属性だけを書き換えます.<br>
    class, styleは専用の書き方が使えます.<br>
    入力欄とRVを双方向に同期するv-modelもこのページで説明します.
  </p>

  <ul class="toc">
    <li><a @click='setHash("Attr")'>属性バインディング(:属性名)</a></li>
    <li><a @click='setHash("Boolean")'>真偽値の属性</a></li>
    <li><a @click='setHash("Class")'>Classバインディング</a></li>
    <li><a @click='setHash("Style")'>Styleバインディング</a></li>
    <li><a @click='setHash("Model")'>v-model</a></li>
  </ul>

  <h2 id="Attr">属性バインディング(:属性名)</h2>

  <p>
    属性名の前に <code>:</code> を付けると、値がScalaの式になります.<br>
    <code>v-bind:属性名</code> と書いても同じです.<br>
    Reactiveな値の場合は、値の変化に追従します.<br>
    Reactiveでない値の場合は、描画した時の値になります.
  </p>

  <pre><code class="language-st">{{ codeAttr }}</code></pre>

  <div class="rendered">
    <AttrBasic />
  </div>

  <p>
    属性値の中で <code>"</code> を使う場合は、<br>
    属性を <code>'</code> で囲むか、<code>&amp;quot;</code> と書いてください.<br>
    <code>&#123;&#123; &#125;&#125;</code> は属性値の中では使えません.
  </p>

  <h2 id="Boolean">真偽値の属性</h2>

  <p>
    値が <code>Reactive[Boolean]</code> の場合は、属性の有無を切り替えます.<br>
    disabledやreadonlyなどに使えます.<br>
    Reactiveでない値の場合、<code>true</code> は値なしの属性、<br>
    <code>false</code>・<code>None</code> は属性なしになります.<br>
    <code>Some(x)</code> はxを値にします.
  </p>

  <pre><code class="language-st">{{ codeBoolean }}</code></pre>

  <div class="rendered">
    <AttrBoolean />
  </div>

  <h2 id="Class">Classバインディング</h2>

  <p>
    <code>:class</code> には以下が使えます.<br>
    静的なclass属性と併用できます.
  </p>

  <ul>
    <li>・String(空白区切りで複数可)、List[String]と、そのReactive</li>
    <li>・クラス名と条件の組 <code>"active" -&gt; s.on</code></li>
    <li>・オブジェクト記法 <code>Map("active" -&gt; s.on, "big" -&gt; true)</code></li>
    <li>・配列記法(文字列と組の混在) <code>List("btn", "active" -&gt; s.on)</code></li>
  </ul>

  <p>
    条件はBooleanまたはReactive[Boolean]です.<br>
    Reactiveの場合は、変化に追従してクラスを付け外しします.
  </p>

  <pre><code class="language-st">{{ codeClass }}</code></pre>

  <div class="rendered">
    <ClassBinding />
  </div>

  <h2 id="Style">Styleバインディング</h2>

  <p>
    <code>:style</code> に文字列を指定すると、style属性全体になります.<br>
    オブジェクト記法 <code>Map("color" -&gt; s.color)</code>、<br>
    または組 <code>"display" -&gt; "none"</code> の場合は、<br>
    プロパティごとに設定され、静的なstyle属性と併用できます.<br>
    プロパティ名はキャメルケース(fontSize)でも書けます.<br>
    値にはReactiveも使えます.<br>
    値がNone・false・空文字のプロパティは外します.
  </p>

  <pre><code class="language-st">{{ codeStyle }}</code></pre>

  <div class="rendered">
    <StyleBinding />
  </div>

  <h2 id="Model">v-model</h2>

  <p>
    input・textarea・selectの値と、RVを双方向に同期します.<br>
    入力するとRVが更新され、RVに代入すると入力欄に反映されます.<br>
    RVの型によって同期方法が決まります.<br>
    対応していない型の場合はコンパイルエラーになります.
  </p>

  <ul>
    <li>・テキスト入力・textarea: <code>RV[String]</code></li>
    <li>・数値入力: <code>RV[Int]</code> / <code>RV[Double]</code>(数値として読めない入力は反映しません)</li>
    <li>・チェックボックス: <code>RV[Boolean]</code></li>
    <li>・チェックボックスのグループ、select multiple: <code>RV[List[String]]</code></li>
    <li>・ラジオボタン・select: <code>RV[String]</code></li>
  </ul>

  <pre><code class="language-st">{{ codeModel }}</code></pre>

  <div class="rendered">
    <ModelInputs />
  </div>

  <p>
    テキスト入力は <code>input</code> イベント、<br>
    チェックボックス・ラジオボタン・selectは <code>change</code> イベントで反映します.<br>
    IMEで変換中の入力は、確定するまで反映しません.
  </p>

  <h3 id="Modifiers">修飾子</h3>

  <ul>
    <li>
      <code>.trim</code> : 前後の空白を除いた値をRVへ反映します.<br>
      入力欄の表示はそのままです.
    </li>
    <li><code>.lazy</code> : 常に <code>change</code> イベント(フォーカスが外れた時など)で反映します.</li>
  </ul>

  <p>
    Vueの <code>.number</code> はありません.<br>
    <code>RV[Int]</code> / <code>RV[Double]</code> を指定すれば数値になります.
  </p>

  <pre><code class="language-st">{{ codeModifiers }}</code></pre>

  <div class="rendered">
    <ModelModifiers />
  </div>

  <p>
    子コンポーネントのタグにもv-modelを書けます.<br>
    詳しくは<a @click='go("/sfc")'>単一ファイルコンポーネント</a>のページを参照してください.
  </p>

  <p>
    従来のScalaでの書き方は<a @click='go("/scalaApi")'>Scala API(従来の書き方)</a>を参照してください.
  </p>
</template>
