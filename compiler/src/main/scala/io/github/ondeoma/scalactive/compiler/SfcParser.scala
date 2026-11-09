package io.github.ondeoma.scalactive.compiler

import scala.collection.mutable.ListBuffer

/**
 * @param contentStart ブロック内容の開始オフセット(ファイル全体基準)
 */
final case class Block(name: String, attrs: List[Attr], contentStart: Int, contentEnd: Int, offset: Int) {

  def attr(n: String): Option[Attr] = attrs.find(_.name == n)

  def has(n: String): Boolean = attr(n).isDefined

  def content(src: SourceText): String = src.text.substring(contentStart, contentEnd)

}

final case class Sfc(script: Option[Block], template: Option[Block], styles: List[Block])

/**
 * .stファイルをトップレベルの<script>/<template>/<style>ブロックに分割します.
 */
object SfcParser {

  def parse(src: SourceText): Sfc = {
    val s = src.text
    val blocks = ListBuffer[Block]()
    var i = 0
    while (i < s.length) {
      if (s.charAt(i).isWhitespace) {
        i += 1
      } else if (s.startsWith("<!--", i)) {
        val e = s.indexOf("-->", i + 4)
        if (e < 0) src.fail(i, "unclosed comment")
        i = e + 3
      } else if (s.charAt(i) == '<') {
        val t = TagReader.readOpenTag(src, i)
        if (t.selfClosing) src.fail(i, s"<${t.name}/> must have content")
        val (contentEnd, next) = t.name match {
          case "script" =>
            // 文字列・コメントの中の </script> (コードサンプルなど)では終わらない. 見つからなければ(閉じていない文字列など)単純に探す
            val masked = StCompiler.codeOnly(s.substring(t.end))
            val inCode = masked.indexOf("</script")
            val close = if (inCode >= 0) t.end + inCode else s.indexOf("</script", t.end)
            if (close < 0) src.fail(i, s"unclosed <${t.name}>")
            (close, s.indexOf('>', close) + 1)
          case "style" =>
            val close = s.indexOf("</" + t.name, t.end)
            if (close < 0) src.fail(i, s"unclosed <${t.name}>")
            (close, s.indexOf('>', close) + 1)
          case "template" =>
            val close = findTemplateClose(src, t.end)
            (close, s.indexOf('>', close) + 1)
          case other =>
            src.fail(i, s"unknown top-level block <$other> (expected <script>, <template> or <style>)")
        }
        blocks += Block(t.name, t.attrs, t.end, contentEnd, i)
        i = next
      } else {
        src.fail(i, "unexpected text outside of <script>/<template>/<style> blocks")
      }
    }

    val scripts = blocks.filter(_.name == "script")
    val templates = blocks.filter(_.name == "template")
    if (scripts.size > 1) src.fail(scripts(1).offset, "only one <script> block is allowed")
    if (templates.size > 1) src.fail(templates(1).offset, "only one <template> block is allowed")
    Sfc(scripts.headOption, templates.headOption, blocks.filter(_.name == "style").toList)
  }

  /** 入れ子の<template>を考慮して対応する</template>の位置を返します. */
  private def findTemplateClose(src: SourceText, from: Int): Int = {
    val s = src.text
    def isBoundary(at: Int): Boolean = at < s.length && (s.charAt(at).isWhitespace || s.charAt(at) == '>' || s.charAt(at) == '/')
    var depth = 1
    var i = from
    while (i < s.length) {
      if (s.startsWith("</template", i) && isBoundary(i + 10)) {
        depth -= 1
        if (depth == 0) return i
        i += 10
      } else if (s.startsWith("<template", i) && isBoundary(i + 9)) {
        depth += 1
        i += 9
      } else {
        i += 1
      }
    }
    src.fail(from, "unclosed <template>")
  }

}
