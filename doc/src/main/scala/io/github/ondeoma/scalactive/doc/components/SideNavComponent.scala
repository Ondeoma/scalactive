package io.github.ondeoma.scalactive.doc.components

import io.github.ondeoma.scalactive.components.*

object SideNavComponent extends StatelessComponent {

  def genHtml(implicit cm: ComponentManager): HTML = {
    
    val go = Router.go 
    
    // language=html
    s"""<nav class="side-nav">
       |  <ul class="links">
       |    <li ${evClick(go("/"))}>はじめに</li>
       |    <li ${evClick(go("/start"))}>クイックスタート</li>
       |    <li ${evClick(go("/component"))}>コンポーネント</li>
       |    <li ${evClick(go("/reactive"))}>リアクティビティ</li>
       |    <li ${evClick(go("/attr"))}>属性バインディング</li>
       |    <li ${evClick(go("/conditionalRendering"))}>条件付きレンダリング</li>
       |    <li ${evClick(go("/listRendering"))}>リストレンダリング</li>
       |    <li ${evClick(go("/event"))}>イベントハンドリング</li>
       |    <li ${evClick(go("/builtinComponents"))}>組込コンポーネント</li>
       |    <li ${evClick(go("/routing"))}>ルーティング</li>
       |    <li ${evClick(go("/state"))}>状態管理</li>
       |    <li ${evClick(go("/deploy"))}>本番ビルド</li>
       |    <li ${evClick(go("/contribute"))}>コントリビュート</li>
       |  </ul>
       |</nav>
       |""".stripMargin
  }
  
}
