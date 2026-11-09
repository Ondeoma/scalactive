package io.github.ondeoma.scalactive.ls

import io.github.ondeoma.scalactive.compiler.{Element, HtmlNode, HtmlParser, SfcParser, SourceText, StCompiler}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.collection.JavaConverters._
import scala.util.control.NonFatal

/**
 * 子コンポーネントのpropの参照の検索と名前の変更.
 * propは、定義(case class Props)・同じパッケージの.stのテンプレートの属性(title / :title / @close / v-model:count)・
 * 名前付きスロット(<template #footer>、<slot name="footer">)から参照されます.
 * (定義した.stの中のScalaの参照は、StLanguageServiceがpresentation compilerに問い合わせます)
 */
object PropRefs {

  /** 参照の書き方(名前を変えるときの書き換え方が変わる) */
  sealed trait Kind
  /** Scalaの名前(定義) */
  case object Plain extends Kind
  /** 子コンポーネントのタグの属性名 */
  final case class AttrName(written: String) extends Kind
  /** <template #name> の属性名 */
  final case class SlotAttr(written: String) extends Kind
  /** <slot name="..."> の値 */
  final case class SlotValue(written: String) extends Kind

  /** 参照. start/endは書き換える範囲 */
  final case class Occurrence(file: Path, start: Int, end: Int, kind: Kind)

  /** 名前を変える対象のprop */
  final case class Target(component: String, componentFile: Path, prop: String, declOffset: Int)

  private def read(p: Path): String = new String(Files.readAllBytes(p), StandardCharsets.UTF_8)

  private def kebabToCamel(s: String): String = {
    val parts = s.split('-')
    (parts.head :: parts.tail.toList.filter(_.nonEmpty).map(p => p.head.toUpper.toString + p.tail)).mkString
  }

  private def slotProp(attr: String): Option[String] = {
    if (attr.startsWith("#") || attr == "v-slot" || attr.startsWith("v-slot:")) {
      val raw = if (attr.startsWith("#")) attr.drop(1) else attr.drop("v-slot".length).stripPrefix(":")
      Some(if (raw.isEmpty || raw == "default") "slot" else kebabToCamel(raw))
    } else None
  }

  private def declOf(componentFile: Path, prop: String, textOf: Path => String): Option[Int] = {
    StCompiler.propsInfo(textOf(componentFile)).find(_.name == prop).map(_.offset)
  }

  /** offsetがpropの定義・参照の上なら、そのprop */
  def target(text: String, offset: Int, file: Path, textOf: Path => String): Option[Target] = {
    try {
      val src = new SourceText(text)
      val sfc = SfcParser.parse(src)
      val own = file.getFileName.toString.stripSuffix(".st")
      // 定義(case class Props のパラメータ名)
      val fromDecl = StCompiler.propsInfo(text).find(p => offset >= p.offset && offset <= p.offset + p.name.length)
        .map(p => Target(own, file, p.name, p.offset))
      fromDecl.orElse {
        sfc.template.filter(t => offset >= t.contentStart && offset <= t.contentEnd).flatMap { t =>
          TemplateNavigation.wordAt(text, offset).flatMap { case (s, e) =>
            val word = text.substring(s, e)
            TemplateCompletion.context(text, t.contentStart, s) match {
              case TemplateCompletion.AttrName(tag, _, _) if tag.headOption.exists(_.isUpper) =>
                for {
                  cf <- TemplateNavigation.componentFile(tag, file)
                  p <- TemplateNavigation.propName(word)
                  d <- declOf(cf, p, textOf)
                } yield Target(tag, cf, p, d)
              case TemplateCompletion.AttrName("template", _, stack) if stack.headOption.exists(_.headOption.exists(_.isUpper)) =>
                for {
                  cf <- TemplateNavigation.componentFile(stack.head, file)
                  p <- slotProp(word)
                  d <- declOf(cf, p, textOf)
                } yield Target(stack.head, cf, p, d)
              case TemplateCompletion.AttrValue("slot", "name", _) =>
                val p = kebabToCamel(word)
                declOf(file, p, textOf).map(d => Target(own, file, p, d))
              case _ => None
            }
          }
        }
      }
    } catch {
      case NonFatal(_) => None
    }
  }

  private def stFiles(dir: Path): List[Path] = {
    try {
      val s = Files.list(dir)
      try s.iterator().asScala.filter(_.toString.endsWith(".st")).toList.sortBy(_.toString)
      finally s.close()
    } catch {
      case NonFatal(_) => Nil
    }
  }

  private def elements(text: String): List[Element] = {
    try {
      val src = new SourceText(text)
      SfcParser.parse(src).template.toList.flatMap { t =>
        def walk(ns: List[HtmlNode]): List[Element] = ns.flatMap {
          case e: Element => e :: walk(e.children)
          case _ => Nil
        }
        walk(HtmlParser.parse(src, t.contentStart, t.contentEnd))
      }
    } catch {
      case NonFatal(_) => Nil
    }
  }

  /** propの参照(定義と、テンプレートからの参照). 定義した.stの中のScalaの参照は含みません */
  def occurrences(t: Target, textOf: Path => String): List[Occurrence] = {
    val decl = Occurrence(t.componentFile, t.declOffset, t.declOffset + t.prop.length, Plain)
    val fromTemplates = Option(t.componentFile.getParent).toList.flatMap(stFiles).flatMap { f =>
      val text = textOf(f)
      elements(text).flatMap { e =>
        if (e.name == t.component) {
          val attrs = e.attrs.collect {
            case a if TemplateNavigation.propName(a.name).contains(t.prop) && slotProp(a.name).isEmpty =>
              Occurrence(f, a.offset, a.offset + a.name.length, AttrName(a.name))
          }
          val slots = e.children.collect { case c: Element if c.name == "template" => c }.flatMap(_.attrs).collect {
            case a if slotProp(a.name).contains(t.prop) => Occurrence(f, a.offset, a.offset + a.name.length, SlotAttr(a.name))
          }
          attrs ++ slots
        } else if (e.name == "slot" && f == t.componentFile) {
          e.attrs.collect {
            case a if a.name == "name" && a.value.exists(v => kebabToCamel(v) == t.prop) && a.valueOffset >= 0 =>
              Occurrence(f, a.valueOffset, a.valueOffset + a.value.get.length, SlotValue(a.value.get))
          }
        } else Nil
      }
    }
    decl :: fromTemplates
  }

  /** コンポーネントのタグ(<Card / </Card)の参照 */
  def componentOccurrences(component: String, dir: Path, textOf: Path => String): List[Occurrence] = {
    val tag = ("</?" + java.util.regex.Pattern.quote(component) + "(?![\\w.-])").r
    stFiles(dir).flatMap { f =>
      val text = textOf(f)
      val template = try SfcParser.parse(new SourceText(text)).template catch {
        case NonFatal(_) => None
      }
      template.toList.flatMap { t =>
        tag.findAllMatchIn(text.substring(t.contentStart, t.contentEnd)).map { m =>
          val s = t.contentStart + m.end - component.length
          Occurrence(f, s, s + component.length, Plain)
        }
      }
    }
  }

  private def kebab(s: String): String = s.flatMap(c => if (c.isUpper) "-" + c.toLower else c.toString)

  /** 元の書き方に合わせる(ケバブケースならケバブケースで) */
  private def styled(name: String, written: String): String = if (written.contains("-")) kebab(name) else name

  /** 名前を変えたときの、参照の新しい書き方 */
  def renamed(kind: Kind, newName: String): Either[String, String] = kind match {
    case Plain => Right(newName)
    case SlotValue(w) => Right(styled(newName, w))
    case SlotAttr(w) =>
      val prefix = if (w.startsWith("#")) "#" else "v-slot:"
      Right(if (newName == "slot") prefix + "default" else prefix + styled(newName, w.stripPrefix(prefix)))
    case AttrName(w) =>
      if (w.startsWith("@") || w.startsWith("v-on:")) {
        val prefix = if (w.startsWith("@")) "@" else "v-on:"
        if (newName.length > 2 && newName.startsWith("on") && newName.charAt(2).isUpper) {
          val ev = newName.charAt(2).toLower.toString + newName.drop(3)
          Right(prefix + styled(ev, w.stripPrefix(prefix)))
        } else Left(s"'$w' is an event binding; the new name must look like onXxx")
      } else if (w == "v-model" || w.startsWith("v-model:")) {
        Right(if (newName == "model") "v-model" else "v-model:" + styled(newName, w.stripPrefix("v-model:")))
      } else if (w.startsWith(":")) Right(":" + styled(newName, w.drop(1)))
      else if (w.startsWith("v-bind:")) Right("v-bind:" + styled(newName, w.drop("v-bind:".length)))
      else Right(styled(newName, w))
  }

  def defaultTextOf: Path => String = read

}
