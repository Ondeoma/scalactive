package io.github.ondeoma.scalactive.ls

import io.github.ondeoma.scalactive.compiler.{Element, HtmlNode, HtmlParser, SfcParser, SourceText}

import java.nio.file.Path
import scala.util.control.NonFatal

/**
 * 子コンポーネントのタグのpropsの検査(未知のprop・スロット、必須のpropの不足).
 * 同じパッケージの.st(Propsのあるもの)だけを対象にします. コンパイルせずにテキストから求めるため、
 * 型検査の前や、ほかの場所の型エラーでコンパイルできないときにも指摘できます.
 */
object PropsCheck {

  /** 検査の結果. start/endは.stの中の位置、namesは問題のprop・スロットの名前 */
  final case class Problem(start: Int, end: Int, message: String, names: List[String])

  /** propにならない属性(条件・繰り返し・ref)と、子コンポーネントのルート要素に付け加える属性 */
  private def ignored(name: String): Boolean = {
    val raw = name.stripPrefix(":").stripPrefix("v-bind:")
    Set("v-if", "v-else-if", "v-else", "v-for", "key", "ref").contains(raw) ||
      raw == "class" || raw == "style" || raw == "id" || raw == "role" || raw.startsWith("data-") || raw.startsWith("aria-")
  }

  private def kebabToCamel(s: String): String = {
    val parts = s.split('-')
    (parts.head :: parts.tail.toList.filter(_.nonEmpty).map(p => p.head.toUpper.toString + p.tail)).mkString
  }

  /** 属性名 → propの名前(TemplateCodegenの対応付けと同じ) */
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
      else if (attr.startsWith("v-")) None
      else Some(attr)
    raw.map(kebabToCamel).filter(_.nonEmpty)
  }

  /** <template #name> のスロット名(propの名前) */
  private def slotName(c: HtmlNode): Option[(String, Int, String)] = c match {
    case t: Element if t.name == "template" =>
      t.attrs.find(a => a.name.startsWith("#") || a.name == "v-slot" || a.name.startsWith("v-slot:")).map { a =>
        val raw = if (a.name.startsWith("#")) a.name.drop(1) else a.name.drop("v-slot".length).stripPrefix(":")
        (if (raw.isEmpty || raw == "default") "slot" else kebabToCamel(raw), a.offset, a.name)
      }
    case _ => None
  }

  def check(text: String, file: Path): List[Problem] = {
    try {
      val src = new SourceText(text)
      SfcParser.parse(src).template.toList.flatMap { t =>
        val nodes = HtmlParser.parse(src, t.contentStart, t.contentEnd)
        val components = TemplateCompletion.siblingComponents(file)
        val cache = scala.collection.mutable.Map[String, List[TemplateCompletion.Prop]]()
        def propsOf(name: String) = cache.getOrElseUpdate(name, components.get(name).map(TemplateCompletion.readProps).getOrElse(Nil))

        def walk(ns: List[HtmlNode]): List[Problem] = ns.flatMap {
          case e: Element =>
            val own =
              if (e.name.headOption.exists(_.isUpper) && propsOf(e.name).nonEmpty) checkElement(e, propsOf(e.name))
              else Nil
            own ++ walk(e.children)
          case _ => Nil
        }

        walk(nodes)
      }
    } catch {
      // 構文エラーはコンパイラが報告する
      case NonFatal(_) => Nil
    }
  }

  private def checkElement(e: Element, props: List[TemplateCompletion.Prop]): List[Problem] = {
    val known = props.map(_.name).toSet
    val attrs = e.attrs.filterNot(a => ignored(a.name)).flatMap(a => propName(a.name).map(p => (p, a.offset, a.name)))
    val (named, rest) = e.children.partition(c => slotName(c).isDefined)
    val slots = named.flatMap(slotName)
    val hasDefaultSlot = rest.exists {
      case io.github.ondeoma.scalactive.compiler.Text(t, _) => t.trim.nonEmpty
      case _: io.github.ondeoma.scalactive.compiler.RawText => false
      case _ => true
    }
    val unknown = (attrs ++ slots).collect {
      case (p, offset, written) if !known.contains(p) =>
        val what = if (written.startsWith("#") || written.startsWith("v-slot")) "slot" else "prop"
        Problem(offset, offset + written.length, s"<${e.name}> has no $what '$p'", List(p))
    }
    val defaultSlot =
      if (hasDefaultSlot && !known.contains("slot")) List(Problem(e.offset + 1, e.offset + 1 + e.name.length, s"<${e.name}> has no default slot (children are ignored)", List("slot")))
      else Nil
    val given = (attrs.map(_._1) ++ slots.map(_._1) ++ (if (hasDefaultSlot) List("slot") else Nil)).toSet
    val missing = props.filter(p => p.hasDefault.contains(false) && !given.contains(p.name)).map(_.name)
    val missingProblem =
      if (missing.isEmpty) Nil
      else List(Problem(e.offset + 1, e.offset + 1 + e.name.length, s"<${e.name}> is missing required prop${if (missing.size > 1) "s" else ""}: ${missing.mkString(", ")}", missing))
    unknown ++ defaultSlot ++ missingProblem
  }

}
