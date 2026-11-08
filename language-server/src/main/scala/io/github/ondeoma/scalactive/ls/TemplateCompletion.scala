package io.github.ondeoma.scalactive.ls

import io.github.ondeoma.scalactive.compiler.{HtmlParser, SfcParser, SourceText, StCompiler}
import org.eclipse.lsp4j.{CompletionItem, CompletionItemKind, InsertTextFormat, Range, TextEdit}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.collection.JavaConverters._
import scala.util.control.NonFatal

/**
 * テンプレートのHTMLの部分の補完(コンポーネント名・HTMLのタグ・属性・属性値・イベント・ディレクティブ・スロット名).
 * 書きかけでコンパイルできない.stでも動くよう、コンパイルせずにテキストを走査して文脈を求めます.
 */
object TemplateCompletion {

  /** カーソル位置の文脈 */
  sealed trait Context

  /** タグ名(`<` の後). stackは外側の要素(内側が先頭) */
  final case class TagName(start: Int, stack: List[String]) extends Context

  /** タグの属性名 */
  final case class AttrName(tag: String, start: Int, stack: List[String]) extends Context

  /** 属性値(引用符の中) */
  final case class AttrValue(tag: String, attr: String, start: Int) extends Context

  case object Other extends Context

  private val rawTextElements = Set("script", "style", "textarea")

  /**
   * テンプレートの中のoffsetの文脈を求めます.
   *
   * @param from テンプレートの内容の開始位置
   */
  def context(text: String, from: Int, offset: Int): Context = {
    var i = from
    var stack: List[String] = Nil
    def isNameChar(c: Char) = c.isLetterOrDigit || c == '-' || c == '_' || c == ':' || c == '.'
    while (i < offset) {
      if (text.startsWith("<!--", i)) {
        val end = text.indexOf("-->", i + 4)
        if (end < 0 || end + 3 > offset) return Other
        i = end + 3
      } else if (text.startsWith("</", i)) {
        val end = text.indexOf('>', i)
        if (end < 0 || end >= offset) return Other
        val name = text.substring(i + 2, end).trim
        stack.indexOf(name) match {
          case -1 => ()
          case k => stack = stack.drop(k + 1)
        }
        i = end + 1
      } else if (text.charAt(i) == '<' && (i + 1 >= offset || text.charAt(i + 1).isLetter)) {
        // 開始タグ
        val nameStart = i + 1
        var j = nameStart
        while (j < text.length && isNameChar(text.charAt(j))) j += 1
        if (offset <= j) return TagName(nameStart, stack)
        val tag = text.substring(nameStart, j)
        var done = false
        var selfClosing = false
        while (!done) {
          while (j < offset && text.charAt(j).isWhitespace) j += 1
          if (j >= offset) return AttrName(tag, offset, stack)
          text.charAt(j) match {
            case '>' =>
              j += 1
              done = true
            case '/' if j + 1 < text.length && text.charAt(j + 1) == '>' =>
              j += 2
              done = true
              selfClosing = true
            case _ =>
              val attrStart = j
              while (j < text.length && !text.charAt(j).isWhitespace && text.charAt(j) != '=' && text.charAt(j) != '>' &&
                !text.startsWith("/>", j)) j += 1
              if (offset <= j) return AttrName(tag, attrStart, stack)
              val attr = text.substring(attrStart, j)
              var k = j
              while (k < offset && text.charAt(k).isWhitespace) k += 1
              if (k < text.length && text.charAt(k) == '=') {
                k += 1
                while (k < offset && text.charAt(k).isWhitespace) k += 1
                if (k >= offset) return Other
                val q = text.charAt(k)
                if (q == '"' || q == '\'') {
                  val close = text.indexOf(q, k + 1)
                  if (close < 0 || close >= offset) return AttrValue(tag, attr, k + 1)
                  j = close + 1
                } else {
                  while (k < text.length && !text.charAt(k).isWhitespace && text.charAt(k) != '>') k += 1
                  if (k >= offset) return Other
                  j = k
                }
              }
          }
        }
        i = j
        val lower = tag.toLowerCase
        if (rawTextElements.contains(lower)) {
          val close = text.indexOf(s"</$tag", i)
          if (close < 0 || close >= offset) return Other
          i = close
        } else if (!selfClosing && !HtmlParser.voidElements.contains(lower)) {
          stack = tag :: stack
        }
      } else {
        i += 1
      }
    }
    Other
  }

  // ---- 補完の候補 ----

  private[ls] final case class Prop(name: String, tpe: String, hasDefault: Option[Boolean] = None) {
    def isSlot: Boolean = tpe.startsWith("Slot")
    def isScopedSlot: Boolean = tpe.startsWith("ScopedSlot")
    def isEvent: Boolean = name.length > 2 && name.startsWith("on") && name.charAt(2).isUpper && tpe.contains("=>")
    def isModel: Boolean = tpe.startsWith("RV[")
  }

  /** 組み込みのコンポーネントのprops */
  private[ls] val builtins: Map[String, List[Prop]] = Map(
    "Teleport" -> List(Prop("to", "String"), Prop("disabled", "Boolean"), Prop("slot", "Slot")),
    "ErrorBoundary" -> List(Prop("onError", "Throwable => Unit"), Prop("fallback", "ScopedSlot[ErrorInfo]"), Prop("slot", "Slot")),
    "Await" -> List(Prop("load", "() => Future[A]"), Prop("pending", "Slot"), Prop("error", "ScopedSlot[ErrorInfo]"), Prop("slot", "ScopedSlot[A]")),
    "RouterLink" -> List(Prop("to", "String"), Prop("activeClass", "String"), Prop("exactActiveClass", "String"), Prop("slot", "Slot")),
  )

  /** 値を書かない(真偽値の)属性 */
  private val booleanAttributes = Set(
    "hidden", "disabled", "readonly", "required", "checked", "selected", "multiple", "autofocus", "autoplay", "controls",
    "loop", "muted", "open", "novalidate", "reversed", "download", "inert", "playsinline", "default")

  private val elementDirectives = List(
    "v-if", "v-else-if", "v-else", "v-for", ":key", "v-show", "v-model", "v-html", "ref", ":class", ":style",
    "@click", "@input", "@change", "@submit.prevent", "@keydown.enter")

  private val componentDirectives = List("v-if", "v-else-if", "v-else", "v-for", ":key", "class", ":class", "style", ":style", "id")

  private[ls] def kebab(s: String): String = s.flatMap(c => if (c.isUpper) "-" + c.toLower else c.toString)

  private[ls] def readProps(file: Path): List[Prop] = {
    try StCompiler.propsInfo(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)).map(p => Prop(p.name, p.tpe, Some(p.hasDefault)))
    catch {
      case NonFatal(_) => Nil
    }
  }

  /** 同じディレクトリ(同じパッケージ)の.stコンポーネント(名前 → ファイル) */
  private[ls] def siblingComponents(file: Path): Map[String, Path] = {
    try {
      val dir = file.getParent
      val s = Files.list(dir)
      try s.iterator().asScala.filter(p => p.toString.endsWith(".st") && p != file).map { p =>
        p.getFileName.toString.stripSuffix(".st") -> p
      }.toMap
      finally s.close()
    } catch {
      case NonFatal(_) => Map.empty
    }
  }

  private[ls] def propsOfComponent(name: String, file: Path): List[Prop] = {
    builtins.get(name).orElse(siblingComponents(file).get(name).map(readProps)).getOrElse(Nil)
  }

  private def item(label: String,
                   kind: CompletionItemKind,
                   detail: String,
                   range: Range,
                   snippet: Option[String] = None,
                   sort: String = "1"): CompletionItem = {
    val ci = new CompletionItem(label)
    ci.setKind(kind)
    ci.setDetail(detail)
    ci.setSortText(sort + label)
    ci.setFilterText(label)
    snippet match {
      case Some(s) =>
        ci.setInsertTextFormat(InsertTextFormat.Snippet)
        ci.setTextEdit(org.eclipse.lsp4j.jsonrpc.messages.Either.forLeft(new TextEdit(range, s)))
      case None =>
        ci.setTextEdit(org.eclipse.lsp4j.jsonrpc.messages.Either.forLeft(new TextEdit(range, label)))
    }
    ci
  }

  /**
   * テンプレートの中なら補完の候補を返します(テンプレートの外・Scalaの部分ならNone).
   *
   * @param file 補完している.stのパス(同じパッケージのコンポーネントを探すため)
   */
  def complete(text: String, offset: Int, file: Path, index: LineIndex): Option[List[CompletionItem]] = {
    val template = try SfcParser.parse(new SourceText(text)).template catch {
      case NonFatal(_) => None
    }
    template.filter(t => offset >= t.contentStart && offset <= t.contentEnd).map { t =>
      def range(start: Int) = index.range(start, offset)
      def attr(label: String, detail: String, r: Range, value: Boolean = true, sort: String = "1") = {
        item(label, CompletionItemKind.Property, detail, r, if (value) Some(label + "=\"$1\"") else None, sort)
      }
      context(text, t.contentStart, offset) match {
        case TagName(start, stack) =>
          val r = range(start)
          val components = (siblingComponents(file).keys ++ builtins.keys).toList.sorted.map { n =>
            item(n, CompletionItemKind.Class, "component", r, sort = "0")
          }
          // 子コンポーネントの中の<template>は、スロットに渡す内容
          components ++ HtmlData.tags.map { case (n, d) => item(n, CompletionItemKind.Keyword, s"<$n> $d", r) }
        case AttrName(tag, start, stack) =>
          val r = range(start)
          if (tag.headOption.exists(_.isUpper)) {
            val props = propsOfComponent(tag, file)
            val fromProps = props.flatMap { p =>
              if (p.isSlot || p.isScopedSlot) Nil
              else if (p.isEvent) {
                val ev = kebab(p.name.drop(2).head.toLower + p.name.drop(3))
                List(attr("@" + ev, p.tpe, r, sort = "0"))
              } else if (p.isModel) {
                val model = if (p.name == "model") "v-model" else s"v-model:${p.name}"
                List(attr(model, p.tpe, r, sort = "0"), attr(":" + p.name, p.tpe, r, sort = "0"))
              } else List(attr(p.name, p.tpe, r, sort = "0"), attr(":" + p.name, p.tpe, r, sort = "0"))
            }
            fromProps ++ componentDirectives.map(d => attr(d, "directive", r, value = d != "v-else"))
          } else if (tag == "template" && stack.headOption.exists(_.headOption.exists(_.isUpper))) {
            // 子コンポーネントの中の<template #name>
            propsOfComponent(stack.head, file).collect {
              case p if p.isSlot => item(if (p.name == "slot") "#default" else "#" + p.name, CompletionItemKind.Property, p.tpe, r, sort = "0")
              case p if p.isScopedSlot =>
                val n = if (p.name == "slot") "#default" else "#" + p.name
                item(n, CompletionItemKind.Property, p.tpe, r, Some(n + "=\"$1\""), sort = "0")
            } ++ List("v-if", "v-else-if", "v-else", "v-for").map(d => attr(d, "directive", r, value = d != "v-else"))
          } else if (tag == "slot") {
            List(attr("name", "slot name", r), attr(":value", "value for the scoped slot", r))
          } else {
            // 要素の属性 → ディレクティブ → 全体の属性 → イベントの順
            def htmlAttr(n: String, d: String, sort: String) = attr(n, d, r, value = !booleanAttributes.contains(n), sort = sort)
            val own = HtmlData.attributesOf(tag)
            own.map { case (n, d) => htmlAttr(n, d, "0") } ++
              own.map { case (n, d) => attr(":" + n, d + "(バインド)", r, sort = "1") } ++
              elementDirectives.map(d => attr(d, "directive", r, value = d != "v-else", sort = "1")) ++
              HtmlData.globalAttributes.map { case (n, d) => htmlAttr(n, d, "2") } ++
              HtmlData.events.filterNot { case (e, _) => elementDirectives.contains("@" + e) }.map { case (e, d) =>
                attr("@" + e, d, r, sort = "3")
              }
          }
        case AttrValue("slot", "name", start) =>
          // <slot name="..."> は自分のPropsのスロット
          val r = range(start)
          readProps(file).collect {
            case p if (p.isSlot || p.isScopedSlot) && p.name != "slot" => item(kebab(p.name), CompletionItemKind.Property, p.tpe, r)
          }
        case AttrValue(tag, attrName, start) if !attrName.startsWith(":") && !attrName.startsWith("v-") && !attrName.startsWith("@") =>
          // 列挙された属性値(input の type など)
          val r = range(start)
          HtmlData.valuesOf(tag, attrName).map(v => item(v, CompletionItemKind.EnumMember, attrName + "=\"" + v + "\"", r))
        case _ => Nil
      }
    }.filter(_.nonEmpty)
  }

}
