package io.github.ondeoma.scalactive.doc.components.contents

import io.github.ondeoma.scalactive.components.*
import org.scalajs.dom.*

object StateComponent extends StatelessComponent {

  def genHtml(implicit cm: ComponentManager): HTML = {
    // language=html
    s"""<h1>状態管理</h1>
       |
       |<p>
       |  状態管理に特化した専用機能はありません.<br>
       |  任意のobjectにReactive系の値を定義して、<br>
       |  各コンポーネントから参照する方法を検討ください.
       |</p>
       |""".stripMargin
  }
  
}
