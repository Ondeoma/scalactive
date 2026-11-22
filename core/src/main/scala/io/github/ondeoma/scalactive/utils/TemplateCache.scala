package io.github.ondeoma.scalactive.utils

import org.scalajs.dom.*

import scala.collection.mutable

/**
 * HTML文字列の解析結果のキャッシュ.
 *
 * 同じHTML(同じ形の描画. 例えば値をコンポーネントで表示する行や、ifCの切り替えで戻ってきた内容)は、
 * 2回目からは解析せず、覚えておいた結果を複製(cloneNode)します.
 * 最近使ったものから maxEntries 件まで覚えます. 長すぎるHTMLは覚えません.
 */
private[scalactive] object TemplateCache {

  var maxEntries: Int = 500

  /** これより長いHTMLは覚えない(使い回される見込みが低く、メモリを使うため) */
  var maxLength: Int = 20000

  private val cache = mutable.LinkedHashMap[String, DocumentFragment]()

  /** SVGの文脈で解析したもののキー(HTMLの文脈のものと分ける). HTMLの先頭には現れない文字で始める */
  private def keyOf(html: String, svg: Boolean): String = if (svg) "\u0000svg:" + html else html

  /**
   * @param svg trueならSVGの要素の中に入れるものとして解析します(要素はSVGの名前空間で作られます)
   */
  def nodes(html: String, svg: Boolean = false): List[Node] = {
    val fragment =
      if (html.length > maxLength) DomUtil.parseHtml(html, svg)
      else {
        val key = keyOf(html, svg)
        val f = cache.remove(key) match {
          case Some(cached) => cached
          case None => DomUtil.parseHtml(html, svg)
        }
        // 最近使ったものとして末尾に置き直す
        cache.update(key, f)
        if (cache.size > maxEntries) cache.remove(cache.head._1)
        f.cloneNode(true).asInstanceOf[DocumentFragment]
      }
    val ns = fragment.childNodes
    List.tabulate(ns.length)(ns(_))
  }

  def clear(): Unit = cache.clear()

}
