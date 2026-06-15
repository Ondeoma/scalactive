package io.github.ondeoma.scalactive.doc.components.contents.introduction

import io.github.ondeoma.scalactive.components.*
import io.github.ondeoma.scalactive.reactive.*
import org.scalajs.dom.*
import SampleComponent.State

object SampleComponent extends StatefulComponent[State] {

  case class State(txt: RV[String] = RV(""))

  def initState() = State()

  def genHtml(s: State)
             (implicit cm: ComponentManager): HTML = {

    // 組込コンポーネントのTextInputStringComponentは、
    // 汎用的な実装になっているので、
    // 各プロジェクト毎に最適化してもらうと良さそうです。
    // その例が以下です。詳細は組込コンポーネントのページをご覧ください。
    def myTxtInputC(rv: RV[String]): HTML = {
      %(TextInputStringComponent(HtmlInputType.text, rv, Map(), Map(), List(EventType.input)))
    }

    // 組込コンポーネントのTextComponentは以下略です。
    def myTxtC(rv: RV[String]): HTML = {
      %(TextComponent(rv)(identity))
    }

    // HTMLをScala文字列として書くような形となっています。
    // 中に別のコンポーネントを埋め込むことができます。
    // language=html
    s"""<table>
       :  <tbody>
       :    <tr>
       :      <th>入力</th>
       :      <td>${myTxtInputC(s.txt)}</td>
       :    </tr>
       :    <tr>
       :      <th>状態</th>
       :      <td>${myTxtC(s.txt)}</td>
       :    </tr>
       :    <tr>
       :      <th>イベント</th>
       :      <td><button ${evClick(window.alert(s"入力値は${s.txt.v}です。"))}>ボタン</button></td>
       :    </tr>
       :  </tbody>
       :</table>
       :""".stripMargin(':')
  }
}  
