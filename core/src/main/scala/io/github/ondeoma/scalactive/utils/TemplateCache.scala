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

  private def parse(html: String): DocumentFragment = {
    val t = document.createElement("template").asInstanceOf[HTMLTemplateElement]
    t.innerHTML = html
    t.content
  }

  def nodes(html: String): List[Node] = {
    val fragment =
      if (html.length > maxLength) parse(html)
      else {
        val f = cache.remove(html) match {
          case Some(cached) => cached
          case None => parse(html)
        }
        // 最近使ったものとして末尾に置き直す
        cache.update(html, f)
        if (cache.size > maxEntries) cache.remove(cache.head._1)
        f.cloneNode(true).asInstanceOf[DocumentFragment]
      }
    val ns = fragment.childNodes
    List.tabulate(ns.length)(ns(_))
  }

  def clear(): Unit = cache.clear()

}
