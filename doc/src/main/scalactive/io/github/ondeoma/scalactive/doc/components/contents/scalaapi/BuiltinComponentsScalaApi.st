<script lang="scala">
import Router.*
import org.scalajs.dom.window.alert

val codeText = // language=scala
  s"""val rvS = RV("Some Text")
     |s\"\"\"$${%(TextComponent(rvS)(_ + "!!!"))}"\"\"\"
     |""".stripMargin

val codeHtml = // language=scala
  s"""val rvS = RV("Some Text")
     |s\"\"\"$${%(HtmlComponent(rvS)(s => s"<span style='color: skyblue'>$${s}</span>"))}\"\"\"
     |""".stripMargin

val codeButton = // language=scala
  s"""import org.scalajs.dom.window.alert
     |// ラッパー関数
     |def myPrimaryBtn(text: String,
     |                 onClick: Event => Unit) = {
     |  ButtonComponent(text, onClick, Map("style" -> "background-color: skyblue"), Map())
     |}
     |    
     |s\"\"\"$${%(myPrimaryBtn("button", _ => alert("clicked!")))}\"\"\"
     |""".stripMargin

val codeLink = // language=scala
  s"""import org.scalajs.dom.window.alert
     |// ラッパー関数
     |def myPrimaryLink(text: String,
     |                  onClick: Event => Unit) = {
     |  LinkComponent(text, onClick, Map("style" -> "color: skyblue"), Map())
     |}
     |    
     |s\"\"\"$${%(myPrimaryLink("Link", _ => alert("clicked!")))}\"\"\"
     |""".stripMargin

val codeTIS = // language=scala
  s"""val inputS = RV("TEXT")
     |// ラッパー関数
     |def textInput(text: RV[String]) = {
     |  import io.github.ondeoma.scalactive.enums.EventType.*
     |  TextInputStringComponent(HtmlInputType.text, text, Map(), Map(), List(blur))
     |}
     |s\"\"\"$${%(textInput(inputS))} => $${%(TextComponent(inputS)(identity))}\"\"\"
     |""".stripMargin

val codeTIG = // language=scala
  s"""val inputN = RV(1)
     |// ラッパー関数
     |def numberInput(n: RV[Int]) = {
     |  import io.github.ondeoma.scalactive.enums.EventType.*
     |  TextInputGeneralComponent(HtmlInputType.number, n, _.toIntOption.getOrElse(0), _.toString, Map(), Map(), List(input))
     |}
     |s\"\"\"$${%(numberInput(inputN))} => $${%(TextComponent(inputN)(_.toString))}\"\"\"
     |""".stripMargin

val codeTA = // language=scala
  s"""val inputS = RV("TEXT")
     |// ラッパー関数
     |def textArea(text: RV[String]) = {
     |  import io.github.ondeoma.scalactive.enums.EventType.*
     |  TextAreaComponent(text, Map(), Map(), List(keyup))
     |}
     |s\"\"\"$${%(textArea(inputS))} => $${%(TextComponent(inputS)(identity))}\"\"\"
     |""".stripMargin

val codeFI = // language=scala
  s"""val inputFs = RV(List[File]())
     |// ラッパー関数
     |def fileInput(fs: RV[List[File]]) = {
     |  FileInputComponent(fs ,Map(), Map())
     |}
     |
     |s\"\"\"$${%(fileInput(inputFs))} => $${%(TextComponent(inputFs)(_.map(_.name).mkString))}\"\"\"
     |""".stripMargin

val codeCB = // language=scala
  s"""val inputCh = RV(false)
     |// ラッパー関数
     |def checkbox(rv: RV[Boolean]) = {
     |  CheckboxBooleanComponent(rv ,Map(), Map())
     |}
     |
     |s\"\"\"$${%(checkbox(inputCh))} => $${%(TextComponent(inputCh)(_.toString))}\"\"\"
     |""".stripMargin     

val codeCG = // language=scala
  s"""val onOff = RV("OFF")
     |// ラッパー関数
     |def checkboxOnOff(rv: RV[String]) = {
     |  CheckboxGeneralComponent(rv, "", ch => if (ch.checked) "ON" else "OFF", _ == "ON" ,Map(), Map())
     |}
     |
     |s\"\"\"$${%(checkboxOnOff(onOff))} => $${%(TextComponent(onOff)(identity))}\"\"\"
     |""".stripMargin   

val codeCGroup = // language=scala
  s"""val chs = RV(List[String]())
     |// ラッパー関数
     |def checkGroup(values: RV[List[String]],
     |               value: String) = {
     |  CheckboxGroupPartComponent(values, value, Map(), Map())
     |}
     |
     |s\"\"\"$${%(checkGroup(chs, "A"))} $${%(checkGroup(chs, "B"))} => $${%(TextComponent(chs)(_.mkString))}\"\"\"
     |""".stripMargin   

val codeRadioS = // language=scala
  s"""val radioSV = RV("")
     |// ラッパー関数
     |def radio(rv: RV[String],
     |          value: String) = {
     |  RadioStringComponent(rv, value, Map(), Map())
     |}
     |
     |s\"\"\"<label>A$${%(radio(radioSV, "A"))}</label>
     |   :<label>B$${%(radio(radioSV, "B"))}</label> 
     |   :=> $${%(TextComponent(radioSV)(identity))}\"\"\".stripMargin(":")
     |""".stripMargin   

val codeRadioG = // language=scala
  s"""val radioGV = RV(0)
     |// ラッパー関数
     |def radioN(rv: RV[Int],
     |           value: Int) = {
     |  RadioGeneralComponent(rv, value.toString, _ => value, _ == value, Map(), Map())
     |}
     |
     |s\"\"\"<label>1$${%(radioN(radioGV, 1))}</label>
     |   :<label>2$${%(radioN(radioGV, 2))}</label> 
     |   :=> $${%(TextComponent(radioGV)(_.toString))}\"\"\".stripMargin(":")
     |""".stripMargin

val codeSelectS = // language=scala
  s"""val selectSV = RV("")
     |// ラッパー関数
     |def select(rv: RV[String],
     |           selects: List[(SelectValue, SelectDisplayName)]) = {
     |  SelectStringComponent(rv, selects, Map(), Map())
     |}
     |
     |val selects = List(
     |  "A" -> "a",
     |  "B" -> "b"
     |)
     |
     |s\"\"\"$${%(select(selectSV, selects))} => $${%(TextComponent(selectSV)(identity))}\"\"\"
     |""".stripMargin

val codeSelectG = // language=scala
  s"""val selectGV = RV(0)
     |// ラッパー関数
     |def selectN(rv: RV[Int],
     |            selectsN: List[(Int, SelectDisplayName)]) = {
     |  val selects = selectsN.map(s => (s._1.toString, s._2))
     |  SelectGeneralComponent(rv, selects, _.toInt, _.toString, Map(), Map())
     |}
     |val selectsN = List(
     |  1 -> "One",
     |  2 -> "Two"
     |)
     |
     |s\"\"\"$${%(selectN(selectGV, selectsN))} => $${%(TextComponent(selectGV)(_.toString))}\"\"\"
     |""".stripMargin           

val codeTeleport = // language=scala
  s"""val open = RV(false)
     |// 中身をbodyの末尾に描画する. 元の位置には目印のコメントだけが残る
     |val modal = ifC(open) { implicit cm =>
     |  teleportC("body") { implicit cm =>
     |    s"<div class='modal'>...<button $${evClick(open := false)}>閉じる</button></div>"
     |  }
     |}
     |""".stripMargin

// デモ(サンプルコードと同じ組込コンポーネントをScalaで呼び出す)

val rvS = RV("Some Text")
def myPrimaryBtn(text: String,
                 onClick: Event => Unit) = {
  ButtonComponent(text, onClick, Map("style" -> "background-color: skyblue"), Map())
}

def myPrimaryLink(text: String,
                  onClick: Event => Unit) = {
  LinkComponent(text, onClick, Map("style" -> "color: skyblue"), Map())
}

val inputS = RV("TEXT")
def textInput(text: RV[String]) = {
  import io.github.ondeoma.scalactive.enums.EventType.*
  TextInputStringComponent(HtmlInputType.text, text, Map(), Map(), List(blur))
}

val inputN = RV(1)
def numberInput(n: RV[Int]) = {
  import io.github.ondeoma.scalactive.enums.EventType.*
  TextInputGeneralComponent(HtmlInputType.number, n, _.toIntOption.getOrElse(0), _.toString, Map(), Map(), List(input))
}

def textArea(text: RV[String]) = {
  import io.github.ondeoma.scalactive.enums.EventType.*
  TextAreaComponent(text, Map(), Map(), List(keyup))
}

val inputFs = RV(List[File]())
def fileInput(fs: RV[List[File]]) = {
  FileInputComponent(fs ,Map(), Map())
}

val inputCh = RV(false)
def checkbox(rv: RV[Boolean]) = {
  CheckboxBooleanComponent(rv ,Map(), Map())
}

val onOff = RV("OFF")
def checkboxOnOff(rv: RV[String]) = {
  CheckboxGeneralComponent(rv, "", ch => if (ch.checked) "ON" else "OFF", _ == "ON" ,Map(), Map())
}

val chs = RV(List[String]())
def checkGroup(values: RV[List[String]],
               value: String) = {
  CheckboxGroupPartComponent(values, value, Map(), Map())
}

val radioSV = RV("")
def radio(rv: RV[String],
          value: String) = {
  RadioStringComponent(rv, value, Map(), Map())
}

val radioGV = RV(0)
def radioN(rv: RV[Int],
           value: Int) = {
  RadioGeneralComponent(rv, value.toString, _ => value, _ == value, Map(), Map())
}

val selectSV = RV("")
def select(rv: RV[String],
           selects: List[(SelectValue, SelectDisplayName)]) = {
  SelectStringComponent(rv, selects, Map(), Map())
}
val selects = List(
  "A" -> "a",
  "B" -> "b"
)

val selectGV = RV(0)
def selectN(rv: RV[Int],
            selectsN: List[(Int, SelectDisplayName)]) = {
  val selects = selectsN.map(s => (s._1.toString, s._2))
  SelectGeneralComponent(rv, selects, _.toInt, _.toString, Map(), Map())
}
val selectsN = List(
  1 -> "One",
  2 -> "Two"
)

def demoText(using cm: ComponentManager): HTML =
  %(TextComponent(rvS)(_ + "!!!"))

def demoHtml(using cm: ComponentManager): HTML =
  %(HtmlComponent(rvS)(s => s"<span style='color: skyblue'>${s}</span>"))

def demoButton(using cm: ComponentManager): HTML =
  %(myPrimaryBtn("button", _ => alert("clicked!")))

def demoLink(using cm: ComponentManager): HTML =
  %(myPrimaryLink("Link", _ => alert("clicked!")))

def demoTIS(using cm: ComponentManager): HTML =
  s"""${%(textInput(inputS))} => ${%(TextComponent(inputS)(identity))}"""

def demoTIG(using cm: ComponentManager): HTML =
  s"""${%(numberInput(inputN))} => ${%(TextComponent(inputN)(_.toString))}"""

def demoTA(using cm: ComponentManager): HTML =
  s"""${%(textArea(inputS))} => ${%(TextComponent(inputS)(identity))}"""

def demoFI(using cm: ComponentManager): HTML =
  s"""${%(fileInput(inputFs))} => ${%(TextComponent(inputFs)(_.map(_.name).mkString))}"""

def demoCB(using cm: ComponentManager): HTML =
  s"""${%(checkbox(inputCh))} => ${%(TextComponent(inputCh)(_.toString))}"""

def demoCG(using cm: ComponentManager): HTML =
  s"""${%(checkboxOnOff(onOff))} => ${%(TextComponent(onOff)(identity))}"""

def demoCGroup(using cm: ComponentManager): HTML =
  s"""${%(checkGroup(chs, "A"))} ${%(checkGroup(chs, "B"))} => ${%(TextComponent(chs)(_.mkString))}"""

def demoRadioS(using cm: ComponentManager): HTML =
  s"""<label>A${%(radio(radioSV, "A"))}</label>
     |<label>B${%(radio(radioSV, "B"))}</label>
     |=> ${%(TextComponent(radioSV)(identity))}""".stripMargin

def demoRadioG(using cm: ComponentManager): HTML =
  s"""<label>1${%(radioN(radioGV, 1))}</label>
     |<label>2${%(radioN(radioGV, 2))}</label>
     |=> ${%(TextComponent(radioGV)(_.toString))}""".stripMargin

def demoSelectS(using cm: ComponentManager): HTML =
  s"""${%(select(selectSV, selects))} => ${%(TextComponent(selectSV)(identity))}"""

def demoSelectG(using cm: ComponentManager): HTML =
  s"""${%(selectN(selectGV, selectsN))} => ${%(TextComponent(selectGV)(_.toString))}"""
</script>

<template>
  <h2 id="builtin">組込コンポーネント</h2>

  <p>
    Scalaの文字列補間でHTMLを書く場合に使う、<br>
    標準のコンポーネント群です.<br>
    .stでは、これらの多くはHTMLの要素と、<br>
    <code>v-model</code>・<code>&#123;&#123; &#125;&#125;</code>・<code>v-html</code>・<code>@click</code> などで書けます.
  </p>

  <ul class="toc">
    <li>
      テキスト/HTML描画系
      <ul>
        <li><a @click='setHash("builtin-Text")'>TextComponent</a></li>
        <li><a @click='setHash("builtin-Html")'>HtmlComponent</a></li>
      </ul>
    </li>

    <li>
      ボタン/リンク系
      <ul>
        <li><a @click='setHash("builtin-Button")'>ButtonComponent</a></li>
        <li><a @click='setHash("builtin-Link")'>LinkComponent</a></li>
      </ul>
    </li>

    <li>
      テキスト入力系
      <ul>
        <li><a @click='setHash("builtin-TextInputString")'>TextInputStringComponent</a></li>
        <li><a @click='setHash("builtin-TextInputGeneral")'>TextInputGeneralComponent </a></li>
        <li><a @click='setHash("builtin-TextArea")'>TextAreaComponent</a></li>
      </ul>
    </li>

    <li>
      ファイル入力系
      <ul>
        <li><a @click='setHash("builtin-FileInput")'>FileInputComponent</a></li>
      </ul>
    </li>

    <li>
      チェックボックス系
      <ul>
        <li><a @click='setHash("builtin-CheckB")'>CheckboxBooleanComponent</a></li>
        <li><a @click='setHash("builtin-CheckG")'>CheckboxGeneralComponent</a></li>
        <li><a @click='setHash("builtin-CheckGroup")'>CheckboxGroupPartComponent</a></li>
      </ul>
    </li>

    <li>
      ラジオボタン系
      <ul>
        <li><a @click='setHash("builtin-RadioS")'>RadioStringComponent</a></li>
        <li><a @click='setHash("builtin-RadioG")'>RadioGeneralComponent</a></li>
      </ul>
    </li>

    <li>
      セレクトボックス系
      <ul>
        <li><a @click='setHash("builtin-SelectS")'>SelectStringComponent</a></li>
        <li><a @click='setHash("builtin-SelectG")'>SelectGeneralComponent</a></li>
      </ul>
    </li>

    <li>
      その他
      <ul>
        <li><a @click='setHash("builtin-Teleport")'>Teleport</a></li>
      </ul>
    </li>

  </ul>

  <h3 id="builtin-Text">TextComponent</h3>

  <p>
    リアクティブなテキストを描画するためのコンポーネントです.<br>
    テキストはHTML解釈されずにそのまま描画されます.<br>
  </p>

  <pre><code class="language-scala">TextComponent[A](rv1: Reactive[A])
                (formatter: A => String)</code></pre>

  <table class="disc-table">
    <thead>
      <tr>
        <th>引数</th>
        <th>説明</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <th>rv1</th>
        <td>リアクティブな任意の変数</td>
      </tr>
      <tr>
        <th>formatter</th>
        <td>描画文字列への変換関数</td>
      </tr>
    </tbody>
  </table>

  <p>
    複数値を利用したい場合は以下のような派生コンポーネントを利用できます.
  </p>

  <pre><code class="language-scala">TextComponent2[A, B](rv1: Reactive[A], rv2: Reactive[B])
                    (formatter: (A, B) => String)</code></pre>

  <p>
    ※TextComponent6まで実装されています.<br>
  </p>

  <h4>利用例</h4>

  <pre><code class="language-scala">{{ codeText }}</code></pre>

  <div class="rendered" v-html="demoText"></div>

  <h3 id="builtin-Html">HtmlComponent</h3>

  <p>
    リアクティブなHTMLを描画するためのコンポーネントです.<br>
    使い方はTextComponentと同様です.<br>
    ※利用の際はXSSにご注意ください.
  </p>

  <pre><code class="language-scala">HtmlComponent[A](rv1: Reactive[A])
                (formatter: A => String)</code></pre>

  <table class="disc-table">
    <thead>
      <tr>
        <th>引数</th>
        <th>説明</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <th>rv1</th>
        <td>リアクティブな任意の変数</td>
      </tr>
      <tr>
        <th>formatter</th>
        <td>描画HTML文字列への変換関数</td>
      </tr>
    </tbody>
  </table>

  <p>
    ※TextComponent同様にHtmlComponent6まで実装されています.<br>
  </p>

  <h4>利用例</h4>

  <pre><code class="language-scala">{{ codeHtml }}</code></pre>

  <div class="rendered" v-html="demoHtml"></div>

  <h3 id="builtin-Button">ButtonComponent</h3>

  <p>
    ボタンです.
  </p>

  <pre><code class="language-scala">ButtonComponent(text: String | Reactive[String],
                onClick: Event => Unit,
                attrs: Map[AttrName, String | Boolean],
                attrRs: Map[AttrName, Reactive[String] | Reactive[Boolean]])</code></pre>

  <table class="disc-table">
    <thead>
      <tr>
        <th>引数</th>
        <th>説明</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <th>text</th>
        <td>ボタンに表示するテキスト</td>
      </tr>
      <tr>
        <th>onClick</th>
        <td>ボタンクリック時のイベントハンドラ関数</td>
      </tr>
      <tr>
        <th>attrs</th>
        <td>
          要素の属性.<br>
          キーが属性名で、<br>
          値がStringの場合は属性値、<br>
          Booleanの場合は属性の有無と解釈されます.
        </td>
      </tr>
      <tr>
        <th>attrRs</th>
        <td>
          上記同様に要素の属性ですが、<br>
          リアクティブな属性を指定します.
        </td>
      </tr>
    </tbody>
  </table>

  <h4>利用例</h4>

  <p>
    引数が多いので、そのまま文字列内で利用するのは推奨しません。<br>
    各プロジェクトで使いやすいラッパー関数をご用意ください.
  </p>

  <pre><code class="language-scala">{{ codeButton }}</code></pre>

  <div class="rendered" v-html="demoButton"></div>


  <h3 id="builtin-Link">LinkComponent</h3>

  <p>
    リンク(aタグ)です.<br>
  </p>

  <pre><code class="language-scala">LinkComponent(text: String | Reactive[String],
              onClick: Event => Unit,
              attrs: Map[AttrName, String | Boolean],
              attrRs: Map[AttrName, Reactive[String] | Reactive[Boolean]])</code></pre>

  <table class="disc-table">
    <thead>
      <tr>
        <th>引数</th>
        <th>説明</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <th>text</th>
        <td>ボタンに表示するテキスト</td>
      </tr>
      <tr>
        <th>onClick</th>
        <td>ボタンクリック時のイベントハンドラ関数</td>
      </tr>
      <tr>
        <th>attrs</th>
        <td>
          要素の属性.<br>
          キーが属性名で、<br>
          値がStringの場合は属性値、<br>
          Booleanの場合は属性の有無と解釈されます.
        </td>
      </tr>
      <tr>
        <th>attrRs</th>
        <td>
          上記同様に要素の属性ですが、<br>
          リアクティブな属性を指定します.
        </td>
      </tr>
    </tbody>
  </table>

  <h4>利用例</h4>

  <pre><code class="language-scala">{{ codeLink }}</code></pre>

  <div class="rendered" v-html="demoLink"></div>

  <h3 id="builtin-TextInputString">TextInputStringComponent</h3>

  <p>
    inputタグです. RV[String]をバインドできます.<br>
    名前はTextとなっていますが、任意のinputタグのtypeを指定できます.
  </p>

  <pre><code class="language-scala">TextInputStringComponent(inputType: StringInputType,
                         value: RV[String],
                         attrs: Map[AttrName, String | Boolean],
                         attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
                         updateEvents: List[EventType])</code></pre>

  <table class="disc-table">
    <thead>
      <tr>
        <th>引数</th>
        <th>説明</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <th>inputType</th>
        <td>input要素のtype値.<br>
            cf. <a href="https://github.com/Ondeoma/scalactive/blob/main/core/src/main/scala/io/github/ondeoma/scalactive/enums/HtmlInputType.scala" target="_blank">StringInputType</a></td>
      </tr>
      <tr>
        <th>value</th>
        <td>入力値とバインドする値</td>
      </tr>
      <tr>
        <th>attrs</th>
        <td>
          要素の属性.<br>
          キーが属性名で、<br>
          値がStringの場合は属性値、<br>
          Booleanの場合は属性の有無と解釈されます.
        </td>
      </tr>
      <tr>
        <th>attrRs</th>
        <td>
          上記同様に要素の属性ですが、<br>
          リアクティブな属性を指定します.
        </td>
      </tr>
      <tr>
        <th>updateEvents</th>
        <td>
          画面で入力した値がvalue引数値に設定されるタイミングのイベントを指定. <br>
          基本的な用途ではblur, change, input等を想定しています.<br>
          cf. <a href="https://github.com/Ondeoma/scalactive/blob/main/core/src/main/scala/io/github/ondeoma/scalactive/enums/EventType.scala" target="_blank">EventType</a>
        </td>
      </tr>
    </tbody>
  </table>

  <h4>利用例</h4>

  <pre><code class="language-scala">{{ codeTIS }}</code></pre>

  <div class="rendered" v-html="demoTIS"></div>



  <h3 id="builtin-TextInputGeneral">TextInputGeneralComponent</h3>

  <p>
    TextInputStringComponentのジェネリック版です.
  </p>

  <pre><code class="language-scala">TextInputGeneralComponent[A](inputType: StringInputType,
                             value: RV[A],
                             fromS: SelectValue => A,
                             toS: A => SelectValue,
                             attrs: Map[AttrName, String | Boolean],
                             attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
                             updateEvents: List[EventType])</code></pre>

  <table class="disc-table">
    <thead>
      <tr>
        <th>引数</th>
        <th>説明</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <th>inputType</th>
        <td>input要素のtype値.<br>
            cf. <a href="https://github.com/Ondeoma/scalactive/blob/main/core/src/main/scala/io/github/ondeoma/scalactive/enums/HtmlInputType.scala" target="_blank">StringInputType</a></td>
      </tr>
      <tr>
        <th>value</th>
        <td>入力値とバインドする値</td>
      </tr>
      <tr>
        <th>fromS</th>
        <td>入力値のStringからジェネリック型への変換関数</td>
      </tr>
      <tr>
        <th>toS</th>
        <td>ジェネリック型からString型への変換関数</td>
      </tr>
      <tr>
        <th>attrs</th>
        <td>
          要素の属性.<br>
          キーが属性名で、<br>
          値がStringの場合は属性値、<br>
          Booleanの場合は属性の有無と解釈されます.
        </td>
      </tr>
      <tr>
        <th>attrRs</th>
        <td>
          上記同様に要素の属性ですが、<br>
          リアクティブな属性を指定します.
        </td>
      </tr>
      <tr>
        <th>updateEvents</th>
        <td>
          画面で入力した値がvalue引数値に設定されるタイミングのイベントを指定. <br>
          基本的な用途ではblur, change, input等を想定しています.<br>
          cf. <a href="https://github.com/Ondeoma/scalactive/blob/main/core/src/main/scala/io/github/ondeoma/scalactive/enums/EventType.scala" target="_blank">EventType</a>
        </td>
      </tr>
    </tbody>
  </table>

  <h4>利用例</h4>

  <pre><code class="language-scala">{{ codeTIG }}</code></pre>

  <div class="rendered" v-html="demoTIG"></div>



  <h3 id="builtin-TextArea">TextAreaComponent</h3>

  <p>
    テキストエリアです. RV[String]をバインドできます.<br>
  </p>

  <pre><code class="language-scala">TextAreaComponent(value: RV[String],
                  attrs: Map[AttrName, String | Boolean],
                  attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
                  updateEvents: List[EventType])</code></pre>

  <table class="disc-table">
    <thead>
      <tr>
        <th>引数</th>
        <th>説明</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <th>value</th>
        <td>入力値とバインドする値</td>
      </tr>
      <tr>
        <th>attrs</th>
        <td>
          要素の属性.<br>
          キーが属性名で、<br>
          値がStringの場合は属性値、<br>
          Booleanの場合は属性の有無と解釈されます.
        </td>
      </tr>
      <tr>
        <th>attrRs</th>
        <td>
          上記同様に要素の属性ですが、<br>
          リアクティブな属性を指定します.
        </td>
      </tr>
      <tr>
        <th>updateEvents</th>
        <td>
          画面で入力した値がvalue引数値に設定されるタイミングのイベントを指定. <br>
          基本的な用途ではblur, change, input等を想定しています.<br>
          cf. <a href="https://github.com/Ondeoma/scalactive/blob/main/core/src/main/scala/io/github/ondeoma/scalactive/enums/EventType.scala" target="_blank">EventType</a>
        </td>
      </tr>
    </tbody>
  </table>

  <h4>利用例</h4>

  <pre><code class="language-scala">{{ codeTA }}</code></pre>

  <div class="rendered" v-html="demoTA"></div>


  <h3 id="builtin-FileInput">FileInputComponent</h3>

  <p>
    ファイルインプットです. RV[org.scalajs.dom.File]をバインドできます.<br>
  </p>

  <pre><code class="language-scala">TextAreaComponent(files: RV[List[File]],
                  attrs: Map[AttrName, String | Boolean],
                  attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]])</code></pre>

  <table class="disc-table">
    <thead>
      <tr>
        <th>引数</th>
        <th>説明</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <th>files</th>
        <td>入力値とバインドするRV[List[File]]</td>
      </tr>
      <tr>
        <th>attrs</th>
        <td>
          要素の属性.<br>
          キーが属性名で、<br>
          値がStringの場合は属性値、<br>
          Booleanの場合は属性の有無と解釈されます.
        </td>
      </tr>
      <tr>
        <th>attrRs</th>
        <td>
          上記同様に要素の属性ですが、<br>
          リアクティブな属性を指定します.
        </td>
      </tr>
    </tbody>
  </table>

  <h4>利用例</h4>

  <pre><code class="language-scala">{{ codeFI }}</code></pre>

  <div class="rendered" v-html="demoFI"></div>


  <h3 id="builtin-CheckB">CheckboxBooleanComponent</h3>

  <p>
    チェックボックスです. RV[Boolean]をバインドできます(checked = true).<br>
  </p>

  <pre><code class="language-scala">CheckboxBooleanComponent(rv: RV[Boolean],
                         attrs: Map[AttrName, String | Boolean],
                         attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]])</code></pre>

  <table class="disc-table">
    <thead>
      <tr>
        <th>引数</th>
        <th>説明</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <th>rv</th>
        <td>入力値とバインドするRV[Boolean]</td>
      </tr>
      <tr>
        <th>attrs</th>
        <td>
          要素の属性.<br>
          キーが属性名で、<br>
          値がStringの場合は属性値、<br>
          Booleanの場合は属性の有無と解釈されます.
        </td>
      </tr>
      <tr>
        <th>attrRs</th>
        <td>
          上記同様に要素の属性ですが、<br>
          リアクティブな属性を指定します.
        </td>
      </tr>
    </tbody>
  </table>

  <h4>利用例</h4>

  <pre><code class="language-scala">{{ codeCB }}</code></pre>

  <div class="rendered" v-html="demoCB"></div>


  <h3 id="builtin-CheckG">CheckboxGeneralComponent</h3>

  <p>
    チェックボックスです. CheckboxBooleanComponentのジェネリック版です.<br>
  </p>

  <pre><code class="language-scala">CheckboxGeneralComponent[A](rv: RV[A],
                            value: String,
                            fromElement: HTMLInputElement => A,
                            toChecked: A => Boolean,
                            attrs: Map[AttrName, String | Boolean],
                            attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]])</code></pre>

  <table class="disc-table">
    <thead>
      <tr>
        <th>引数</th>
        <th>説明</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <th>rv</th>
        <td>入力値とバインドするRV[Boolean]</td>
      </tr>
      <tr>
        <th>value</th>
        <td>
          input要素のvalue属性値の値<br>
          ※空文字列も可.
        </td>
      </tr>
      <tr>
        <th>fromElement</th>
        <td>チェックされた要素(チェックボックス)からジェネリック型への変換関数</td>
      </tr>
      <tr>
        <th>toChecked</th>
        <td>
          ジェネリック型からBoolean型への変換関数<br>
          trueの場合にchecked.
        </td>
      </tr>
      <tr>
        <th>attrs</th>
        <td>
          要素の属性.<br>
          キーが属性名で、<br>
          値がStringの場合は属性値、<br>
          Booleanの場合は属性の有無と解釈されます.
        </td>
      </tr>
      <tr>
        <th>attrRs</th>
        <td>
          上記同様に要素の属性ですが、<br>
          リアクティブな属性を指定します.
        </td>
      </tr>
    </tbody>
  </table>

  <h4>利用例</h4>

  <pre><code class="language-scala">{{ codeCG }}</code></pre>

  <div class="rendered" v-html="demoCG"></div>


  <h3 id="builtin-CheckGroup">CheckboxGroupPartComponent</h3>

  <p>
    チェックボックスです. 複数配置を想定したものです.<br>
    RV[List[String]]型を複数箇所でバインドし、<br>
    そのうちのチェックされているチェックボックスのvalue値がListとして格納されます.<br>
    ※格納順に保証はなく、重複値はdistinctされます.
  </p>

  <pre><code class="language-scala">CheckboxGroupPartComponent[A](values: RV[List[String]],
                              value: String,
                              attrs: Map[AttrName, String | Boolean],
                              attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]])</code></pre>

  <table class="disc-table">
    <thead>
      <tr>
        <th>引数</th>
        <th>説明</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <th>values</th>
        <td>入力値とバインドするRV[List[String]]</td>
      </tr>
      <tr>
        <th>value</th>
        <td>
          この要素がチェックされた時にvaluesに格納される文字列
        </td>
      </tr>
      <tr>
        <th>attrs</th>
        <td>
          要素の属性.<br>
          キーが属性名で、<br>
          値がStringの場合は属性値、<br>
          Booleanの場合は属性の有無と解釈されます.
        </td>
      </tr>
      <tr>
        <th>attrRs</th>
        <td>
          上記同様に要素の属性ですが、<br>
          リアクティブな属性を指定します.
        </td>
      </tr>
    </tbody>
  </table>

  <h4>利用例</h4>

  <pre><code class="language-scala">{{ codeCGroup }}</code></pre>

  <div class="rendered" v-html="demoCGroup"></div>


  <h3 id="builtin-RadioS">RadioStringComponent</h3>

  <p>
    ラジオボタンです. RV[String]型をバインドできます.
  </p>

  <pre><code class="language-scala">RadioStringComponent[A](rv: RV[String],
                        value: String,
                        attrs: Map[AttrName, String | Boolean],
                        attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]])</code></pre>

  <table class="disc-table">
    <thead>
      <tr>
        <th>引数</th>
        <th>説明</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <th>rv</th>
        <td>入力値とバインドするRV[String]</td>
      </tr>
      <tr>
        <th>value</th>
        <td>
          この要素がチェックされた時に格納される文字列
        </td>
      </tr>
      <tr>
        <th>attrs</th>
        <td>
          要素の属性.<br>
          キーが属性名で、<br>
          値がStringの場合は属性値、<br>
          Booleanの場合は属性の有無と解釈されます.
        </td>
      </tr>
      <tr>
        <th>attrRs</th>
        <td>
          上記同様に要素の属性ですが、<br>
          リアクティブな属性を指定します.
        </td>
      </tr>
    </tbody>
  </table>

  <h4>利用例</h4>

  <pre><code class="language-scala">{{ codeRadioS }}</code></pre>

  <div class="rendered" v-html="demoRadioS"></div>


  <h3 id="builtin-RadioG">RadioGeneralComponent</h3>

  <p>
    ラジオボタンです. RadioStringComponentのジェネリック版です.
  </p>

  <pre><code class="language-scala">RadioGeneralComponent[A](rv: RV[A],
                         value: String,
                         fromElement: HTMLInputElement => A,
                         toChecked: A => Boolean,
                         attrs: Map[AttrName, String | Boolean],
                         attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]])</code></pre>

  <table class="disc-table">
    <thead>
      <tr>
        <th>引数</th>
        <th>説明</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <th>rv</th>
        <td>入力値とバインドするRV</td>
      </tr>
      <tr>
        <th>value</th>
        <td>
          この要素がチェックされた時に格納される文字列
        </td>
      </tr>
      <tr>
        <th>fromElement</th>
        <td>チェックされた要素(ラジオボタン)からジェネリック型への変換関数</td>
      </tr>
      <tr>
        <th>toChecked</th>
        <td>
          ジェネリック型からBoolean型への変換関数<br>
          trueの場合にchecked.
        </td>
      </tr>
      <tr>
        <th>attrs</th>
        <td>
          要素の属性.<br>
          キーが属性名で、<br>
          値がStringの場合は属性値、<br>
          Booleanの場合は属性の有無と解釈されます.
        </td>
      </tr>
      <tr>
        <th>attrRs</th>
        <td>
          上記同様に要素の属性ですが、<br>
          リアクティブな属性を指定します.
        </td>
      </tr>
    </tbody>
  </table>

  <h4>利用例</h4>

  <pre><code class="language-scala">{{ codeRadioG }}</code></pre>

  <div class="rendered" v-html="demoRadioG"></div>


  <h3 id="builtin-SelectS">SelectStringComponent</h3>

  <p>
    セレクトボックスです. RV[String]をバインドできます.
  </p>

  <pre><code class="language-scala">SelectStringComponent(rv: RV[String],
                      selects: Reactive[List[(SelectValue, SelectDisplayName)]] | List[(SelectValue, SelectDisplayName)],
                      attrs: Map[AttrName, String | Boolean],
                      attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]])</code></pre>

  <table class="disc-table">
    <thead>
      <tr>
        <th>引数</th>
        <th>説明</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <th>rv</th>
        <td>入力値とバインドするRV[String]</td>
      </tr>
      <tr>
        <th>selects</th>
        <td>
          選択肢のListまたはReacitve[List]です.<br>
          値と表示名をタプルとしてList化して指定します.<br>
          SelectValue, SelectDisplayNameはどちらもString型のエイリアスです.
        </td>
      </tr>
      <tr>
        <th>attrs</th>
        <td>
          要素の属性.<br>
          キーが属性名で、<br>
          値がStringの場合は属性値、<br>
          Booleanの場合は属性の有無と解釈されます.
        </td>
      </tr>
      <tr>
        <th>attrRs</th>
        <td>
          上記同様に要素の属性ですが、<br>
          リアクティブな属性を指定します.
        </td>
      </tr>
    </tbody>
  </table>

  <h4>利用例</h4>

  <pre><code class="language-scala">{{ codeSelectS }}</code></pre>

  <div class="rendered" v-html="demoSelectS"></div>





  <h3 id="builtin-SelectG">SelectGeneralComponent</h3>

  <p>
    セレクトボックスです. SelectStringComponentのジェネリック版です.
  </p>

  <pre><code class="language-scala">SelectGeneralComponent[A](rv: RV[A],
                          selects: Reactive[List[(SelectValue, SelectDisplayName)]] | List[(SelectValue, SelectDisplayName)],
                          fromSelected: SelectValue => A,
                          toSelected: A => SelectValue,
                          attrs: Map[AttrName, String | Boolean],
                          attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]])</code></pre>

  <table class="disc-table">
    <thead>
      <tr>
        <th>引数</th>
        <th>説明</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <th>rv</th>
        <td>入力値とバインドするRV[String]</td>
      </tr>
      <tr>
        <th>selects</th>
        <td>
          選択肢のListまたはReacitve[List]です.<br>
          値と表示名をタプルとしてList化して指定します.<br>
          SelectValue, SelectDisplayNameはどちらもString型のエイリアスです.
        </td>
      </tr>
      <tr>
        <th>fromSelected</th>
        <td>選択値からジェネリック型への変換関数</td>
      </tr>
      <tr>
        <th>toSelected</th>
        <td>ジェネリック型から選択値への変換関数</td>
      </tr>
      <tr>
        <th>attrs</th>
        <td>
          要素の属性.<br>
          キーが属性名で、<br>
          値がStringの場合は属性値、<br>
          Booleanの場合は属性の有無と解釈されます.
        </td>
      </tr>
      <tr>
        <th>attrRs</th>
        <td>
          上記同様に要素の属性ですが、<br>
          リアクティブな属性を指定します.
        </td>
      </tr>
    </tbody>
  </table>

  <h4>利用例</h4>

  <pre><code class="language-scala">{{ codeSelectG }}</code></pre>

  <div class="rendered" v-html="demoSelectG"></div>

  <h3 id="builtin-Teleport">Teleport</h3>

  <p>
    中身を、別の要素(CSSセレクターで指定)の末尾に描画します.<br>
    VueのTeleport、ReactのPortalにあたります.<br>
    モーダル・トースト・ツールチップなどを、<br>
    親要素のoverflowやz-indexの影響を受けない場所に描く場合に使います.<br>
    元の位置には、目印のコメントだけが残ります.<br>
    中身はこのコンポーネントのものとして扱われます.<br>
    そのため、リアクティブな表示やイベントはそのまま動き、<br>
    片付けられると描画先から取り除かれます.<br>
    <code>disabled = true</code> の場合は、元の位置に描画します.
  </p>

  <pre><code class="language-scala">{{ codeTeleport }}</code></pre>
</template>
