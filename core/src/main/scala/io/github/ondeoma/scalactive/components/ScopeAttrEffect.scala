package io.github.ondeoma.scalactive.components

import io.github.ondeoma.scalactive.controllers.ComponentController
import io.github.ondeoma.scalactive.controllers.attrs.AttrController
import org.scalajs.dom.HTMLElement

/**
 * `<style scoped>` の属性(data-st-xxxx)を、子コンポーネントのルート要素に付ける効果.
 * 親のscopedのCSSが子のルート要素(RouterLinkの<a>など)にも当たるようにします(Vueと同じ).
 */
private[scalactive] final class ScopeAttrEffect(val name: String) extends (HTMLElement => ComponentController[?]) {

  def apply(ele: HTMLElement): ComponentController[?] = AttrController { ac =>
    ac.element = Some(ele)
    if (!ele.hasAttribute(name)) {
      ele.setAttribute(name, "")
      ac.onClear = () => ele.removeAttribute(name)
    }
    Right(ac)
  }

}
