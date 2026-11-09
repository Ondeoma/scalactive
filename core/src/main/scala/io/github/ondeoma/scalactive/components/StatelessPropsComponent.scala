package io.github.ondeoma.scalactive.components

import org.scalajs.dom.*

/**
 * 親から値(props)を受け取る状態なしコンポーネント.
 */
trait StatelessPropsComponent[P] extends BaseComponent {

  def genHtml(props: P)
             (implicit cm: ComponentManager): HTML

  def apply(props: P)
           (implicit parent: HTMLElement,
            am: AddMethod): NodesComponentController = {
    mkNCC(genHtml(props)(_))
  }

}
