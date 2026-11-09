<script lang="scala">
import io.github.ondeoma.scalactive.doc.components.contents.component.*

val sample1Code = // language=scala
  s"""import io.github.ondeoma.scalactive.components.*
     |import org.scalajs.dom.window.alert
     |
     |object Sample1Component extends StatelessComponent {
     |
     |  def genHtml(implicit cm: ComponentManager): HTML = {
     |    // language=html
     |    s\"\"\"<div>
     |       :  <p>これは Sample1Component です。</p>
     |       :  <button $${evClick(alert("Sample1Component"))}>Click</button>
     |       :</div>
     |       :\"\"\".stripMargin(':')
     |  }
     |}
     |""".stripMargin

val sample2Code = // language=scala
  s"""import io.github.ondeoma.scalactive.components.*
     |import io.github.ondeoma.scalactive.reactive.*
     |import Sample2Component.State
     |
     |object Sample2Component extends StatefulComponent[State] {
     |
     |  case class State(count: RV[Int] = RV(0))
     |
     |  def initState() = State()
     |
     |  def genHtml(s: State)
     |             (implicit cm: ComponentManager): HTML = {
     |    // language=html
     |    s\"\"\"<div>
     |       :  <p>
     |       :    これは Sample2Component です。<br>
     |       :    可変値 => $${%(TextComponent(s.count)(_.toString))}
     |       :  </p>
     |       :  <button $${evClick(s.count := s.count.v + 1)}>Click</button>
     |       :</div>
     |       :\"\"\".stripMargin(':')
     |  }
     |}
     |""".stripMargin

val sample3Code = // language=scala
  s"""import io.github.ondeoma.scalactive.components.*
     |import org.scalajs.dom.HTMLElement
     |
     |object Sample3Component extends BaseComponent {
     |
     |  def apply(valueFromParent: String)
     |           (implicit parent: HTMLElement,
     |            am: AddMethod): NodesComponentController = {
     |    // mkNCC = NodesComponentControllerを作成する便利関数
     |    mkNCC(genHtml(valueFromParent))
     |  }
     |
     |  def genHtml(valueFromParent: String)
     |             (implicit cm: ComponentManager): HTML = {
     |    // language=html
     |    s\"\"\"<div>
     |       :  これは Sample3Component です。<br>
     |       :  親コンポーネントからは"$${valueFromParent}"を受け取りました。
     |       :</div>
     |       :\"\"\".stripMargin(':')
     |  }
     |}
     |""".stripMargin

val sample4Code = // language=scala
  s"""import io.github.ondeoma.scalactive.components.*
     |
     |object Sample4Component extends StatelessComponent {
     |
     |  def genHtml(implicit cm: ComponentManager): HTML = {
     |    // language=html
     |    s\"\"\"<div>
     |       :  <p>これは Sample4Component です。</p>
     |       :  <hr>
     |       :  $${%(Sample1Component())}
     |       :  $${%(Sample2Component())}
     |       :  $${%(Sample3Component("BySample4Component"))}
     |       :</div>
     |       :\"\"\".stripMargin(':')
     |  }
     |
     |}
     |""".stripMargin

val codeLifecycle = // language=scala
  s"""object ClockComponent extends StatelessComponent {
     |
     |  def genHtml(implicit cm: ComponentManager): HTML = {
     |    val now = RV(new js.Date().toLocaleTimeString())
     |    var timer = 0
     |    // DOMに挿入された後に実行される
     |    onMounted {
     |      timer = window.setInterval(() => now := new js.Date().toLocaleTimeString(), 1000)
     |    }
     |    // 片付けられた(DOMから取り除かれた)後に実行される
     |    onUnmounted {
     |      window.clearInterval(timer)
     |    }
     |    s\"\"\"<p>$${%(TextComponent(now)(identity))}</p>\"\"\"
     |  }
     |
     |}
     |""".stripMargin

val codeRef = // language=scala
  s"""object SearchComponent extends StatelessComponent {
     |
     |  def genHtml(implicit cm: ComponentManager): HTML = {
     |    val input = ElementRef[HTMLInputElement]()
     |    // DOMに挿入された後にフォーカスする
     |    onMounted(input.foreach(_.focus()))
     |    s\"\"\"<input type="search" $${ref(input)}>
     |       :<button $${evClick(input.foreach(_.select()))}>全選択</button>\"\"\".stripMargin(':')
     |  }
     |
     |}
     |""".stripMargin
</script>

<template>
  <h2 id="component">コンポーネント</h2>

  <p>
    コンポーネントはHTML＋処理＋状態のような概念です.
  </p>

  <h3 id="component-StatelessComponent">StatelessComponent</h3>

  <p>
    最も単純な作成方法は、<br>
    StatelessComponentを実装したオブジェクトを作成することです.<br>
    状態(可変値)を持たないコンポーネントに適しています.<br>
    以下のようにgetHtmlを実装します.
  </p>

  <pre><code class="language-scala">{{ sample1Code }}</code></pre>

  <div class="rendered">
    <Sample1Component />
  </div>

  <h3 id="component-StatefulComponent">StatefulComponent</h3>

  <p>
    StatefulComponentは状態を持つコンポーネントの作成に便利です.<br>

    <ul>
      <li>・状態の型</li>
      <li>・状態の初期化(initState)</li>
      <li>・getHtml</li>
    </ul>

    を実装します。
  </p>

  <pre><code class="language-scala">{{ sample2Code }}</code></pre>

  <div class="rendered">
    <Sample2Component />
  </div>

  <h3 id="component-BaseComponent">BaseComponent</h3>

  <p>
    その他のコンポーネントはBaseComponentで作成します.<br>
    自由度が高く、親コンポーネントから値を受け取ること等ができるようになります.<br>
    原則、BaseComponentでは以下のシグネチャを持ったapplyとgetHtmlを実装します.
  </p>

  <pre><code class="language-scala">def apply(任意引数)(implicit parent: HTMLElement, am: AddMethod): NodesComponentController
def genHtml(任意引数)(implicit cm: ComponentManager): HTML // HTML = String
</code></pre>

  <p>以下実装例です.</p>

  <pre><code class="language-scala">{{ sample3Code }}</code></pre>

  <p>親から"ParentValue"を受けとった場合は以下のように表示されます.</p>

  <div class="rendered">
    <Sample3Component value-from-parent="ParentValue" />
  </div>

  <h3 id="component-Use">コンポーネントの呼出し</h3>

  <p>
    作成したコンポーネントは`%`関数で呼び出すことができます.<br>
    ※ComponentManagerが裏で色々やっています.
  </p>

  <pre><code class="language-scala">{{ sample4Code }}</code></pre>

  <div class="rendered">
    <Sample4Component />
  </div>

  <h3 id="component-Lifecycle">ライフサイクルフック</h3>

  <p>
    genHtmlの中で <code>onMounted</code> / <code>onUnmounted</code> を呼ぶと、<br>
    コンポーネントのライフサイクルに合わせて処理を実行できます.<br>
    Vue.jsのmounted / unmountedに当たります.
  </p>

  <ul>
    <li>
      <code>onMounted</code> : コンポーネントがDOMに挿入された後(現在の処理の直後)に実行されます.<br>
      子コンポーネントのものから順に実行されます.<br>
      要素の参照(フォーカスなど)や、外部ライブラリの組み込みに使います.
    </li>
    <li>
      <code>onUnmounted</code> : コンポーネントが片付けられた(DOMから取り除かれた)後に実行されます.<br>
      描き直し・ifCの切り替え・forCの行の削除などでも実行されます.<br>
      タイマーや外部のイベントの解除に使います.
    </li>
    <li>
      描き直し(リアクティブ値の変化によるifCなどの再描画)では、<br>
      古い内容のonUnmountedの後に、新しい内容のonMountedが実行されます.
    </li>
  </ul>

  <pre><code class="language-scala">{{ codeLifecycle }}</code></pre>

  <h3 id="component-Ref">テンプレート参照</h3>

  <p>
    <code>ElementRef</code> を <code>ref(...)</code> で要素に結び付けると、その要素を直接参照できます.<br>
    (Vueのテンプレート参照、ReactのuseRef)<br>
    要素が作られたときに設定され、<br>
    コンポーネントが片付けられると空(<code>get</code> がNone)に戻ります.<br>
    DOMに挿入された後の処理(フォーカスなど)は、<br>
    <code>onMounted</code> の中で行ってください.
  </p>

  <pre><code class="language-scala">{{ codeRef }}</code></pre>

  <h3 id="component-Attrs">属性の引き継ぎ</h3>

  <p>
    <code>c(コンポーネント, 属性...)</code> のように、<br>
    子コンポーネントのルート要素(最初の要素)に属性を付け加えられます.<br>
    (Vueのフォールスルー属性)<br>
    属性は <code>StRuntime</code> の <code>rootCls</code> / <code>rootStyle</code> / <code>rootAttr</code> で作ります.<br>
    classとstyleは子のものと合わされ、子が描き直されたときも付け直されます.<br>
    値にはReactiveも使えます.
  </p>

  <pre><code class="language-scala">import io.github.ondeoma.scalactive.st.StRuntime.{rootAttr, rootCls, rootStyle}

c(ButtonComponent(...), rootCls("primary" -&gt; isActive), rootStyle(Map("width" -&gt; "100%")), rootAttr("aria-label", "送信"))</code></pre>

  <h3 id="component-ErrorBoundary">エラーの境界</h3>

  <p>
    <code>errorBoundaryC</code> で囲んだ中身で起きたエラーを受け止め、<br>
    代わりの表示(fallback)に切り替えます.<br>
    (ReactのError Boundary、VueのerrorCaptured)<br>
    エラーが起きても画面全体が壊れず、その部分だけを差し替えられます.
  </p>

  <pre><code class="language-scala">errorBoundaryC(err =&gt; implicit cm =&gt;
  s&quot;&lt;p&gt;読み込めませんでした: ${err.message} &lt;button ${evClick(err.retry())}&gt;再試行&lt;/button&gt;&lt;/p&gt;&quot;
) { implicit cm =&gt;
  s&quot;&lt;div&gt;${%(RiskyComponent())}&lt;/div&gt;&quot;
}</code></pre>

  <ul>
    <li>
      受け止めるのは、中身を描く途中の例外(子コンポーネントを含む)、<br>
      後から起きる子孫の描き直しの中の例外、<br>
      子孫の <code>onMounted</code> とイベントハンドラーの中の例外です.
    </li>
    <li>
      <code>err.retry()</code> で中身を描き直します.<br>
      <code>onError</code> を指定すると、エラーを受け止めたときに呼ばれます(既定ではコンソールに出力).
    </li>
  </ul>

  <h3 id="component-Await">非同期データ</h3>

  <p>
    <code>awaitC</code> は、非同期の処理(Future)の状態に応じて表示を切り替えます(VueのSuspense).<br>
    完了するまでpending、<br>
    完了したら結果を受け取った表示、<br>
    失敗したらerrorの表示になります.
  </p>

  <pre><code class="language-scala">awaitC(api.fetchUser(id),
  pending = _ =&gt; "&lt;p&gt;読み込み中...&lt;/p&gt;",
  error = err =&gt; implicit cm =&gt; s&quot;&lt;p&gt;${err.message} &lt;button ${evClick(err.retry())}&gt;再試行&lt;/button&gt;&lt;/p&gt;&quot;
)(user =&gt; implicit cm =&gt; s&quot;&lt;p&gt;${user.name}&lt;/p&gt;&quot;)</code></pre>

  <ul>
    <li>
      Futureは描画されたときに評価されます.<br>
      <code>err.retry()</code> でもう1度評価します.
    </li>
    <li>完了する前にコンポーネントが片付けられた場合は、完了しても描き直しません.</li>
  </ul>
</template>
