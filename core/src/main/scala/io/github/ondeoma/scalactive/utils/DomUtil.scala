package io.github.ondeoma.scalactive.utils

import cats.syntax.all.*
import io.github.ondeoma.scalactive.enums.EventType
import io.github.ondeoma.scalactive.models.{AddMethod, Selector}
import io.github.ondeoma.scalactive.reactive.Reactive
import io.github.ondeoma.scalactive.utils.TypeAlias.*
import org.scalajs.dom.*
import org.scalajs.dom.document.body

import scala.language.implicitConversions
import scala.scalajs.js

object DomUtil {

  export Ext.*

  private val checkOrRadioSelector = """[type="checkbox"], [type="radio"]"""

  def addText(root: HTMLElement = body)
             (m: AddMethod,
              text: String): Option[Text] = {
    val t = Text()
    t.data = text
    addNodes(root)(m, t).flatMap(_.headOption)
  }

  def addHtml(root: HTMLElement = body)
             (m: AddMethod,
              html: HTML): List[Node] = {
    val nodes = html.toNodes
    addNodes(root)(m, nodes *)
    nodes
  }

  def addNodes[N <: Node](root: HTMLElement = body)
                         (m: AddMethod,
                          nodes: N*): Option[List[N]] = {
    import io.github.ondeoma.scalactive.models.AddMethod.*

    m match {
      case byNode: AddMethodByNode =>
        byNode match {
          case append(parent) => parent.append(nodes *)
          case prepend(parent) => parent.prepend(nodes *)
          case before(parent) => parent.before(nodes *)
          case after(parent) => parent.after(nodes *)
          case insertBefore(parent, tgt) => nodes.foreach(n => parent.insertBefore(n, tgt))
          case replaceChild(parent, tgt) =>
            nodes.toList match {
              case Nil => ()
              case h :: tail =>
                parent.replaceChild(h, tgt)
                tail.reverse.foreach(n => parent.insertBefore(n, h))
            }
          case replaceWith(element) => element.replaceWith(nodes *)
        }
        nodes.toList.some
      case byS: AddMethodBySelector =>
        (byS match {
          case appendS(s) => withElement(root)(s, _.append(nodes *))
          case prependS(s) => withElement(root)(s, _.prepend(nodes *))
          case beforeS(s) => withElement(root)(s, _.before(nodes *))
          case afterS(s) => withElement(root)(s, _.after(nodes *))
          case insertBeforeS(s, tgt) => withElement(root)(s, p => withElement(p)(tgt, tgt => nodes.foreach(n => p.insertBefore(n, tgt))))
          case replaceChildS(s, tgt) => withElement(root)(s, p => withElement(p)(tgt, tgt => {
            nodes.toList match {
              case Nil => ()
              case h :: tail =>
                p.replaceChild(h, tgt)
                tail.reverse.foreach(n => p.insertBefore(n, h))
            }
          }))
          case replaceWithS(s) => withElement(root)(s, _.replaceWith(nodes *))
        }).map(_ => nodes.toList)
      case byC: AddMethodByComment =>
        (byC match {
          case beforeC(commentId) => withComment(root)(commentId, c => nodes.foreach(n => c.parentNode.insertBefore(n, c)))
          case afterC(commentId) => withComment(root)(commentId, c => nodes.reverse.foreach(n => c.parentNode.insertBefore(n, c.nextSibling)))
        }).map(_ => nodes.toList)
    }
  }

  def withElement[A](root: HTMLElement = body)
                    (s: Selector,
                     f: HTMLElement => A): Option[A] = {
    Option(root.querySelector(s.selector)) match {
      case Some(ele: HTMLElement) => f(ele).some
      case _ =>
        ConsoleUtil.debug(s"Not Found Element: `$s`")
        None
    }
  }

  def withElements[A](root: HTMLElement = body)
                     (s: Selector,
                      f: (HTMLElement, Int) => A): List[A] = {
    val es = root.querySelectorAll(s.selector).toList.collect {
      case e: HTMLElement => e
    }
    if (es.isEmpty) {
      ConsoleUtil.debug(s"Not Found Elements: `$s`")
    }
    es.zipWithIndex.map((e, i) => f(e, i))
  }

  implicit def sToET(s: String): EventType = {
    EventType.CustomEventType(s)
  }

  def withComment[A](root: HTMLElement = body)
                    (commentId: ID,
                     f: Comment => A): Option[A] = {
    val nf: NodeFilter = new js.Object {
      def acceptNode(n: Node): Int = {
        if (n.nodeValue.trim == commentId) NodeFilter.FILTER_ACCEPT
        else NodeFilter.FILTER_REJECT
      }
    }.asInstanceOf[NodeFilter]

    val it: NodeIterator = document.createNodeIterator(root, NodeFilter.SHOW_COMMENT, nf, true)
    Option(it.nextNode()) match {
      case Some(c: Comment) => f(c).some
      case _ =>
        ConsoleUtil.error(s"Not Found Comment: `$commentId`")
        None
    }
  }

  def addEventToElement(ele: HTMLElement,
                        event: EventType,
                        handler: Event => Unit,
                        capture: Boolean = false,
                        once: Boolean = false,
                        passive: Boolean = false): AbortController = {
    val ac = AbortController()
    val elo: EventListenerOptions = new EventListenerOptions {}
    elo.capture = capture
    elo.once = once
    elo.passive = passive
    elo.signal = ac.signal
    ele.addEventListener(event.toString, handler, elo)
    ac
  }

  def getValue(e: HTMLElement): Option[String] = {
    lazy val byInput = e.toInput.flatMap { e =>
      if (e.matches(checkOrRadioSelector)) {
        if (e.checked) e.value.some
        else None
      } else e.value.some
    }
    lazy val bySelect = e.toSelect.map(_.value)
    lazy val byTextArea = e.toTextArea.map(_.value)

    byInput.orElse(bySelect).orElse(byTextArea)
  }

  def setValue(e: HTMLElement,
               v: String): Unit = {
    lazy val toInput = e.toInput.map { e =>
      if (e.matches(checkOrRadioSelector)) {
        e.checked = (e.value == v)
      } else {
        e.value = v
      }
    }
    lazy val toSelect = e.toSelect.map(_.value = v)
    lazy val toTextArea = e.toTextArea.map(_.value = v)

    toInput.orElse(toSelect).orElse(toTextArea)
  }

  def mkTemplate(): HTMLTemplateElement = {
    document.createElement("template").asInstanceOf[HTMLTemplateElement]
  }

  def mkDiv(): HTMLDivElement = {
    document.createElement("div").asInstanceOf[HTMLDivElement]
  }

  def expandAttrs(attrs: Map[AttrName, String | Boolean]): String = {
    attrs.flatMap { (a, v) =>
      (v: @unchecked) match {
        case s: String => s"""$a="$s"""".some
        case b: Boolean if b => a.some
        case b: Boolean if !b => None
      }
    }.mkString(" ")
  }

  def setAttrs(e: Element,
               attrs: Map[AttrName, String | Boolean],
               attrRVs: Map[AttrName, Reactive[String] | Reactive[Boolean]]): Unit = {
    // リアクティブではない属性値の指定
    attrs.filter(t => !attrRVs.exists(_._1 == t._1)).foreach { (n, v) =>
      val fixedV = v match {
        case b: Boolean => ""
        case s: String => s
      }
      e.setAttribute(n, fixedV)
    }
    // リアクティブな属性値の指定
    attrRVs.foreach { (n, rv) =>
      val reactiveV = rv.v
      val staticV = attrs.get(n)
      (reactiveV, staticV) match {
        case (false, _) => e.removeAttribute(n)
        case (true, Some(sv: String)) => e.setAttribute(n, sv)
        case (true, _) => e.setAttribute(n, "")
        case (rv: String, Some(sv: String)) => e.setAttribute(n, s"$rv $sv")
        case (rv: String, _) => e.setAttribute(n, rv)
      }
    }
  }

  object Ext {

    extension (e: Node) {
      def toHtml: Option[HTMLElement] = {
        e match {
          case e: HTMLElement => Some(e)
          case _ =>
            ConsoleUtil.debug(s"NOT HTMLElement($e).")
            None
        }
      }

      def toInput: Option[HTMLInputElement] = {
        e match {
          case e: HTMLInputElement => Some(e)
          case _ =>
            ConsoleUtil.debug(s"NOT HTMLInputElement($e).")
            None
        }
      }

      def toSelect: Option[HTMLSelectElement] = {
        e match {
          case e: HTMLSelectElement => Some(e)
          case _ =>
            ConsoleUtil.debug(s"NOT HTMLTextAreaElement($e).")
            None
        }
      }

      def toTextArea: Option[HTMLTextAreaElement] = {
        e match {
          case e: HTMLTextAreaElement => Some(e)
          case _ =>
            ConsoleUtil.debug(s"NOT HTMLTextAreaElement($e).")
            None
        }
      }

      def toTemplate: Option[HTMLTemplateElement] = {
        e match {
          case e: HTMLTemplateElement => Some(e)
          case _ =>
            ConsoleUtil.debug(s"NOT HTMLTemplateElement($e).")
            None
        }
      }

      def toButton: Option[HTMLButtonElement] = {
        e match {
          case e: HTMLButtonElement => Some(e)
          case _ =>
            ConsoleUtil.debug(s"NOT HTMLButtonElement($e).")
            None
        }
      }

      def childrenHtmlElements: List[HTMLElement] = {
        e.childNodes.toList.toHtmlElements
      }
    }

    extension (e: Element) {
      def addClass(c: String): Unit = {
        e.classList.add(c)
      }

      def getStyle(name: String): String = {
        e match {
          case e: HTMLElement => e.style.getPropertyValue(name)
          case _ => ""
        }
      }

      def setStyle(name: String,
                   value: String,
                   priority: String = ""): Unit = {
        e match {
          case e: HTMLElement => e.style.setProperty(name, value, priority)
          case _ => ()
        }
      }

      def rmStyle(name: String): Unit = {
        e match {
          case e: HTMLElement => e.style.removeProperty(name)
          case _ => ()
        }
      }

      def rmClass(c: String): Unit = {
        e.classList.remove(c)
      }

      def addClasses(clss: List[String]): Unit = {
        clss.foreach(e.classList.add)
      }

      def rmClasses(clss: List[String]): Unit = {
        clss.foreach(e.classList.remove)
      }
    }

    extension (et: EventTarget) {
      def toHtml: Option[HTMLElement] = {
        et match {
          case e: HTMLElement => Some(e)
          case _ =>
            ConsoleUtil.debug(s"NOT HTMLElement($et).")
            None
        }
      }

      def toInput: Option[HTMLInputElement] = {
        et match {
          case e: HTMLInputElement => Some(e)
          case _ =>
            ConsoleUtil.debug(s"NOT HTMLInputElement($et).")
            None
        }
      }

      def toSelect: Option[HTMLSelectElement] = {
        et match {
          case e: HTMLSelectElement => Some(e)
          case _ =>
            ConsoleUtil.debug(s"NOT HTMLSelectElement($et).")
            None
        }
      }

      def toTextArea: Option[HTMLTextAreaElement] = {
        et match {
          case e: HTMLTextAreaElement => Some(e)
          case _ =>
            ConsoleUtil.debug(s"NOT HTMLTextAreaElement($et).")
            None
        }
      }
    }

    extension (ev: Event) {
      def ifInput(f: HTMLInputElement => Unit): Option[Unit] = {
        ev.target.toInput.map(f)
      }

      def ifSelect(f: HTMLSelectElement => Unit): Option[Unit] = {
        ev.target.toSelect.map(f)
      }

      def ifTextArea(f: HTMLTextAreaElement => Unit): Option[Unit] = {
        ev.target.toTextArea.map(f)
      }
    }

    extension (html: String) {

      def toNodes: List[Node] = {
        val t = mkTemplate()
        t.innerHTML = html
        t.content.childNodes.toList
      }

      def toElement: Option[Element] = {
        toNodes.collectFirst {
          case e: Element => e
        }
      }

      def toHtmlElements: List[HTMLElement] = {
        toNodes.collect {
          case e: HTMLElement => e
        }
      }

      def toHtmlElement: Option[HTMLElement] = {
        toNodes.collectFirst {
          case e: HTMLElement => e
        }
      }

    }

    extension (nodes: List[Node]) {
      def toHtmlElements: List[HTMLElement] = {
        nodes.collect {
          case e: HTMLElement => e
        }
      }
      
      def orDummyNode: List[Node] = {
        if (nodes.isEmpty) List(new Comment)
        else nodes
      }
    }

    extension (eles: List[HTMLElement]) {
      def root: Option[HTMLElement] = {
        if (eles.length >= 2) eles.head.parentElement.some
        else eles.headOption
      }
    }

  }
}
