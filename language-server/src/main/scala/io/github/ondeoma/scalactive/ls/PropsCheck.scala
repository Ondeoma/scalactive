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

  /** 検査の結果. start/endは.stの中の位置、namesは問題のprop・スロットの名前、fixesはクイックフィックス */
  final case class Problem(start: Int, end: Int, message: String, names: List[String], fixes: List[Fix] = Nil)

  /** クイックフィックス: start〜endをnewTextに置き換える(start == end なら挿入) */
  final case class Fix(title: String, start: Int, end: Int, newText: String)

  /** 編集距離(大文字・小文字は区別しない) */
  private[ls] def distance(a: String, b: String): Int = {
    val x = a.toLowerCase
    val y = b.toLowerCase
    val d = Array.tabulate(x.length + 1, y.length + 1)((i, j) => if (i == 0) j else if (j == 0) i else 0)
    for (i <- 1 to x.length; j <- 1 to y.length) {
      val cost = if (x(i - 1) == y(j - 1)) 0 else 1
      d(i)(j) = math.min(math.min(d(i - 1)(j) + 1, d(i)(j - 1) + 1), d(i - 1)(j - 1) + cost)
    }
    d(x.length)(y.length)
  }

  /** nameに近い候補(編集距離が名前の長さに応じて1〜3以内、近い順に3つまで) */
  private def similar(name: String, candidates: List[String]): List[String] = {
    val limit = if (name.length <= 4) 1 else if (name.length <= 8) 2 else 3
    candidates.map(c => (c, distance(name, c))).filter(_._2 <= limit).sortBy(_._2).map(_._1).take(3)
  }

  /** 足りないpropを書き足すときの属性(イベントは @name、Stringは name、ほかは :name) */
  private def attributeFor(p: TemplateCompletion.Prop): String = {
    if (p.isEvent) "@" + TemplateCompletion.kebab(p.name.drop(2).head.toLower + p.name.drop(3)) + "=\"\""
    else if (p.tpe == "String") p.name + "=\"\""
    else ":" + p.name + "=\"\""
  }

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
        val isSlot = written.startsWith("#") || written.startsWith("v-slot")
        val what = if (isSlot) "slot" else "prop"
        val candidates = props.filter(c => if (isSlot) c.isSlot || c.isScopedSlot else !(c.isSlot || c.isScopedSlot)).map(_.name)
        val kind = if (isSlot) PropRefs.SlotAttr(written) else PropRefs.AttrName(written)
        val fixes = similar(p, candidates).flatMap { c =>
          PropRefs.renamed(kind, c).toOption.filter(_ != written).map(t => Fix(s"Change to '$t'", offset, offset + written.length, t))
        }.distinct
        Problem(offset, offset + written.length, s"<${e.name}> has no $what '$p'", List(p), fixes)
    }
    val defaultSlot =
      if (hasDefaultSlot && !known.contains("slot")) List(Problem(e.offset + 1, e.offset + 1 + e.name.length, s"<${e.name}> has no default slot (children are ignored)", List("slot")))
      else Nil
    val given = (attrs.map(_._1) ++ slots.map(_._1) ++ (if (hasDefaultSlot) List("slot") else Nil)).toSet
    val missingProps = props.filter(p => p.hasDefault.contains(false) && !given.contains(p.name))
    val missing = missingProps.map(_.name)
    val missingProblem =
      if (missing.isEmpty) Nil
      else {
        // タグ名の直後に書き足す(スロットは書き足さない)
        val insertable = missingProps.filterNot(p => p.isSlot || p.isScopedSlot)
        val at = e.offset + 1 + e.name.length
        val fix = if (insertable.isEmpty) Nil
          else List(Fix(s"Add missing prop${if (insertable.size > 1) "s" else ""}: ${insertable.map(_.name).mkString(", ")}", at, at,
            insertable.map(p => " " + attributeFor(p)).mkString))
        List(Problem(e.offset + 1, e.offset + 1 + e.name.length, s"<${e.name}> is missing required prop${if (missing.size > 1) "s" else ""}: ${missing.mkString(", ")}", missing, fix))
      }
    unknown ++ defaultSlot ++ missingProblem
  }

}
