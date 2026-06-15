package io.github.ondeoma.scalactive.doc.components.contents

import io.github.ondeoma.scalactive.components.*
import org.scalajs.dom.*

object ContributeComponent extends StatelessComponent {

  def genHtml(implicit cm: ComponentManager): HTML = {
    // language=html
    s"""<h1>コントリビュート</h1>
       |
       |<p>
       |  大歓迎です. OSS管理が不慣れなので色々教えてください.<br>   
       |</p>
       |
       |<p>
       |  不具合・要望・提案などは、GitHub Issueとして起票いただけると幸いです.
       |</p>
       |
       |<p>
       |  プルリクエストはdevelopブランチ向けに作成いただけると幸いです.
       |</p>
       |
       |""".stripMargin
  }
  
}
