package io.github.ondeoma.scalactive.doc.components.contents

import io.github.ondeoma.scalactive.components.*
import io.github.ondeoma.scalactive.doc.components.contents.introduction.SampleComponent
import io.github.ondeoma.scalactive.reactive.*
import io.github.ondeoma.scalactive.doc.utils.Utility
import org.scalajs.dom.*

object IntroductionComponent extends StatelessComponent {

  def genHtml(implicit cm: ComponentManager): HTML = {

    import Router.*
    import Utility.esc
    
    // language=scala
    val code = {
      s"""import io.github.ondeoma.scalactive.doc.components.contents.introduction.SampleComponent.State
         |import io.github.ondeoma.scalactive.components.*
         |import io.github.ondeoma.scalactive.reactive.*
         |import org.scalajs.dom.*
         |
         |object SampleComponent extends StatefulComponent[State] {
         |
         |  case class State(txt: RV[String] = RV(""))
         |
         |  def initState() = State()
         |
         |  def genHtml(s: State)
         |             (implicit cm: ComponentManager): HTML = {
         |
         |    // 組込コンポーネントのTextInputStringComponentは、
         |    // 汎用的な実装になっているので、
         |    // 各プロジェクト毎に最適化してもらうと良さそうです。
         |    // (※実際は別のファイルに分けて管理いただくのが良さそうです。)
         |    // その例が以下です。詳細は組込コンポーネントのページをご覧ください。
         |    def myTxtInputC(rv: RV[String]): HTML = {
         |      %(TextInputStringComponent(HtmlInputType.text, rv, Map(), Map(), List(EventType.input)))
         |    }
         |
         |    // 組込コンポーネントのTextComponentも同様です。
         |    def myTxtC(rv: RV[String]): HTML = {
         |      %(TextComponent(rv)(identity))
         |    }
         |
         |    // HTMLをScala文字列として書くような形となっています。
         |    // 中に別のコンポーネントを埋め込むことができます。
         |    // language=html
         |    s\"\"\"<table>
         |       :  <tbody>
         |       :    <tr>
         |       :      <th>入力</th>
         |       :      <td>$${myTxtInputC(s.txt)}</td>
         |       :    </tr>
         |       :    <tr>
         |       :      <th>状態</th>
         |       :      <td>$${myTxtC(s.txt)}</td>
         |       :    </tr>
         |       :    <tr>
         |       :      <th>イベント</th>
         |       :      <td><button $${evClick(window.alert(s"入力値は$${s.txt.value}です。"))}>ボタン</button></td>
         |       :    </tr>
         |       :  </tbody>
         |       :</table>
         |       :\"\"\".stripMargin(':')
         |    // ※stripMargin(":")なのは本ドキュメント作成上の都合なので、
         |    // 通常は"|"のままで問題ありません。
         |  }
         |}
         |""".stripMargin
    }
    
    // language=html
    s"""<h1>はじめに</h1>
       |<p>
       |  Scalactive(スカラクティブ)は主に<a href="https://ja.vuejs.org/">Vue.js</a>の仕様を参考にした<a href="https://www.scala-js.org/">Scala.js</a>のライブラリ？フレームワーク？です.<br>
       |  Scalaでフロントエンドを快適に実装したいという動機で開発を進めています.
       |</p>
       |<p>
       |  現在、技術検証段階のため、本番利用は避けていただくのが無難ですが、<br>
       |  使っていただき、ご意見賜れますと喜びます.
       |</p>
       |<p>
       |  ※このドキュメントはScalactiveで実装されています.
       |</p>
       |
       |<h2>どんな感じ？</h2>
       |
       |<p>
       |  基本的には<a ${evClick(go("/component"))}>コンポーネント</a>を組合わせる実装スタイルとなります.<br>
       |  以下実装例です.
       |</p>
       |
       |<pre><code class="language-scala">${esc(code)}</code></pre>
       |
       |<p>
       |  これを呼び出すと以下のように表示されます.
       |</p>
       |
       |<div class="rendered">
       |  ${%(SampleComponent())}
       |</div>
       |
       |""".stripMargin
  }

}
