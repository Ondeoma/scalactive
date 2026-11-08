package io.github.ondeoma.scalactive.components

import org.scalajs.dom.*

/**
 * 親から値(props)を受け取る状態ありコンポーネント.
 * 状態はコンポーネントの生成ごとに initState(props) で初期化されます.
 */
trait StatefulPropsComponent[P, S] extends BaseComponent {

  def initState(props: P): S

  def genHtml(props: P,
              s: S)
             (implicit cm: ComponentManager): HTML

  def apply(props: P)
           (implicit parent: HTMLElement,
            am: AddMethod): NodesComponentController = {
    mkNCC(genHtml(props, initState(props))(_))
  }

}
