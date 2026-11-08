package io.github.ondeoma.scalactive.ls

import io.github.ondeoma.scalactive.compiler.{SfcParser, SourceText}
import org.eclipse.lsp4j.{Hover, Location, MarkupContent, MarkupKind}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.util.control.NonFatal

/**
 * テンプレートからの定義ジャンプ.
 *  - 子コンポーネントのタグ名(`<Card>` / `</Card>`) → 同じパッケージの Card.st(Propsの定義の位置)
 *  - 子コンポーネントのタグの属性名(`title` / `:count` / `@close` / `v-model:count`) → Card.st のPropsのパラメータ
 * とホバー(子コンポーネントのPropsの一覧・属性の型、HTMLのタグ・属性・イベントの説明).
 */
object TemplateNavigation {

  private def isNameChar(c: Char): Boolean = c.isLetterOrDigit || c == '-' || c == '_' || c == ':' || c == '.' || c == '@' || c == '#'

  /** offsetを含む名前の範囲 */
  private def wordAt(text: String, offset: Int): Option[(Int, Int)] = {
    var s = offset
    while (s > 0 && isNameChar(text.charAt(s - 1))) s -= 1
    var e = offset
    while (e < text.length && isNameChar(text.charAt(e))) e += 1
    if (s < e) Some((s, e)) else None
  }

  private def kebabToCamel(s: String): String = {
    val parts = s.split('-')
    (parts.head :: parts.tail.toList.filter(_.nonEmpty).map(p => p.head.toUpper.toString + p.tail)).mkString
  }

  /** 属性名 → Propsのパラメータ名 */
  private def propName(attr: String): Option[String] = {
    val raw =
      if (attr == "v-model") Some("model")
      else if (attr.startsWith("v-model:")) Some(attr.drop("v-model:".length))
      else if (attr.startsWith("v-bind:")) Some(attr.drop("v-bind:".length))
      else if (attr.startsWith(":")) Some(attr.drop(1))
      else if (attr.startsWith("@") || attr.startsWith("v-on:")) {
        val ev = if (attr.startsWith("@")) attr.drop(1) else attr.drop("v-on:".length)
        Some("on-" + ev.takeWhile(_ != '.'))
      }
      else if (attr.startsWith("v-") || attr.startsWith("#")) None
      else Some(attr)
    raw.map(kebabToCamel).filter(_.nonEmpty)
  }

  private def componentFile(name: String, file: Path): Option[Path] = {
    Option(file.getParent).map(_.resolve(name + ".st")).filter(p => Files.exists(p))
  }

  private def read(p: Path): String = new String(Files.readAllBytes(p), StandardCharsets.UTF_8)

  /** Propsの定義(なければファイルの先頭)の位置 */
  private def propsLocation(target: Path, param: Option[String]): Location = {
    val text = read(target)
    val index = new LineIndex(text)
    val propsAt = """\bcase\s+class\s+Props\s*\(""".r.findFirstMatchIn(text)
    val paramAt = param.flatMap { p =>
      propsAt.flatMap { m =>
        s"""\\b${java.util.regex.Pattern.quote(p)}\\s*:""".r.findFirstMatchIn(text.substring(m.end)).map(x => (m.end + x.start, p.length))
      }
    }
    val (offset, length) = paramAt.getOrElse((propsAt.map(_.start).getOrElse(0), 0))
    new Location(target.toUri.toString, index.range(offset, offset + length))
  }

  def definition(text: String, offset: Int, file: Path): Option[Location] = {
    try {
      val template = SfcParser.parse(new SourceText(text)).template
      template.filter(t => offset >= t.contentStart && offset <= t.contentEnd).flatMap { t =>
        wordAt(text, offset).flatMap { case (s, e) =>
          val word = text.substring(s, e)
          val isTag = (s >= 1 && text.charAt(s - 1) == '<') || (s >= 2 && text.startsWith("</", s - 2))
          if (isTag) {
            if (word.headOption.exists(_.isUpper)) componentFile(word, file).map(propsLocation(_, None)) else None
          } else {
            TemplateCompletion.context(text, t.contentStart, s) match {
              case TemplateCompletion.AttrName(tag, _, _) if tag.headOption.exists(_.isUpper) =>
                for {
                  target <- componentFile(tag, file)
                  p <- propName(word)
                } yield propsLocation(target, Some(p))
              case _ => None
            }
          }
        }
      }
    } catch {
      case NonFatal(_) => None
    }
  }

  // ---- ホバー ----

  private def markdown(range: org.eclipse.lsp4j.Range, md: String): Hover = {
    new Hover(new MarkupContent(MarkupKind.MARKDOWN, md), range)
  }

  private def propLine(p: TemplateCompletion.Prop): String = {
    val default = p.hasDefault match {
      case Some(true) => " = …"
      case _ => ""
    }
    s"${p.name}: ${p.tpe}$default"
  }

  private def required(p: TemplateCompletion.Prop): String = if (p.hasDefault.contains(false)) "(必須)" else ""

  /** コンポーネントのPropsの一覧 */
  private def componentHover(name: String, file: Path): Option[String] = {
    val where = TemplateCompletion.siblingComponents(file).get(name).map(p => s"`${p.getFileName}`")
      .orElse(TemplateCompletion.builtins.get(name).map(_ => "組み込みのコンポーネント"))
    where.map { w =>
      val props = TemplateCompletion.propsOfComponent(name, file)
      val body =
        if (props.isEmpty) "Propsはありません"
        else props.map(propLine).mkString("```scala\ncase class Props(\n  ", ",\n  ", "\n)\n```")
      s"**<$name>** — $w\n\n$body"
    }
  }

  def hover(text: String, offset: Int, file: Path): Option[Hover] = {
    try {
      val template = SfcParser.parse(new SourceText(text)).template
      template.filter(t => offset >= t.contentStart && offset <= t.contentEnd).flatMap { t =>
        wordAt(text, offset).flatMap { case (s, e) =>
          val word = text.substring(s, e)
          val index = new LineIndex(text)
          val range = index.range(s, e)
          val isTag = (s >= 1 && text.charAt(s - 1) == '<') || (s >= 2 && text.startsWith("</", s - 2))
          val md =
            if (isTag) {
              if (word.headOption.exists(_.isUpper)) componentHover(word, file)
              else HtmlData.tagDescriptions.get(word.toLowerCase).map(d => s"**<$word>** — $d")
            } else {
              TemplateCompletion.context(text, t.contentStart, s) match {
                case TemplateCompletion.AttrName(tag, _, _) if tag.headOption.exists(_.isUpper) =>
                  for {
                    p <- propName(word)
                    prop <- TemplateCompletion.propsOfComponent(tag, file).find(_.name == p)
                  } yield s"```scala\n${propLine(prop)}\n```\n<$tag> のprop${required(prop)}"
                case TemplateCompletion.AttrName(tag, _, _) =>
                  if (word.startsWith("@") || word.startsWith("v-on:")) {
                    val ev = (if (word.startsWith("@")) word.drop(1) else word.drop("v-on:".length)).takeWhile(_ != '.')
                    HtmlData.events.toMap.get(ev).map(d => s"**$ev** イベント — $d")
                  } else {
                    val a = word.stripPrefix(":").stripPrefix("v-bind:")
                    HtmlData.describeAttribute(tag, a).map { d =>
                      val values = HtmlData.valuesOf(tag, a)
                      val vs = if (values.isEmpty) "" else values.map(v => s"`$v`").mkString("\n\n値: ", " ", "")
                      s"**$a** — $d$vs"
                    }
                  }
                case _ => None
              }
            }
          md.map(markdown(range, _))
        }
      }
    } catch {
      case NonFatal(_) => None
    }
  }

}
