package io.github.ondeoma.scalactive.utils

import org.scalajs.dom.*

/**
 * 描画中のHTMLをSVGの中に入れるかどうか.
 *
 * コンポーネントのHTMLは文字列を解析して作るため、解析の文脈がないと、
 * `<svg>` の中に入れる要素(v-forの行、v-ifの中身、子コンポーネント)もHTMLの名前空間で作られて表示されません.
 * 子コンポーネントの目印(コメント)の親がSVGの要素(foreignObjectを除く)なら、その子はSVGの文脈で解析します.
 *
 * コントローラーは作られたときの値を覚えておき(ComponentController.svgContext)、
 * initや描き直しのときにその値に戻して描画します.
 */
private[scalactive] object SvgContext {

  val SvgNamespace = "http://www.w3.org/2000/svg"

  private var svg = false

  def current: Boolean = svg

  def within[A](inSvg: Boolean)(f: => A): A = {
    val old = svg
    svg = inSvg
    try f
    finally svg = old
  }

  /** nの子として入れるノードを、SVGの名前空間で作るべきか */
  def isSvgParent(n: Node): Boolean = {
    n match {
      case e: Element => e.namespaceURI == SvgNamespace && e.localName != "foreignObject"
      case _ => false
    }
  }

}
