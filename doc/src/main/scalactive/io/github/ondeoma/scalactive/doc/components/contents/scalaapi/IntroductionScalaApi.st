<script lang="scala">
import io.github.ondeoma.scalactive.doc.components.contents.introduction.SampleComponent

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
</script>

<template>
  <h2 id="introduction">はじめに(Scala APIでの実装例)</h2>

  <p>
    「はじめに」の実装例を、Scala APIで書いたものです.<br>
    HTMLをScalaの文字列として書き、<br>
    組込コンポーネントを埋め込む形になります.
  </p>

  <pre><code class="language-scala">{{ code }}</code></pre>

  <p>
    これを呼び出すと以下のように表示されます.
  </p>

  <div class="rendered">
    <SampleComponent />
  </div>
</template>
