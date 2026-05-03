package io.github.ondeoma.scalactive.components

import cats.syntax.all.*
import org.scalajs.dom.*
import io.github.ondeoma.scalactive.enums.EventType.input
import io.github.ondeoma.scalactive.syntax.All.*
import io.github.ondeoma.scalactive.utils
import io.github.ondeoma.scalactive.controllers.{ComponentController, HtmlElementsComponentController, HtmlElementsController}
import io.github.ondeoma.scalactive.enums.EventType
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.{RV, Reactive}
import io.github.ondeoma.scalactive.utils
import io.github.ondeoma.scalactive.utils.ConsoleUtil

trait BaseComponent {

  export ComponentManager.*
  export EventType.*

  type HtmlEsC[CC <: ComponentController[CC]] = HtmlElementsController[CC]
  type HtmlEsCC = HtmlElementsComponentController

  protected def addNodesErrorMessage: String = {
    val e = s"${this.getClass.getSimpleName} Add Nodes Error!"
    ConsoleUtil.error(e)
    e
  }

  protected def notFoundHeadElementErrorMessage: String = {
    val e = s"${this.getClass.getSimpleName} Not Found Head Element Error!"
    utils.ConsoleUtil.error(e)
    e
  }

  protected def genErrorMessage: String = {
    val e = s"${this.getClass.getSimpleName} Gen Error!"
    utils.ConsoleUtil.error(e)
    e
  }

  protected def mkSimpleHtmlEsCC(gen: => GenResult,
                                 watch: HtmlEsCC => WatchInfos = _ => Nil)
                                (implicit root: HTMLElement,
                                 am: AddMethod): HtmlEsCC = {
    HtmlElementsComponentController { c =>
      for {
        // scala 3.4.0~
        // (ns, children, tmpRs, eIds) <- gen
        t4 <- gen
        (ns, children, tmpRs, eIds) = t4
        _ <- addNodes(root)(am, ns *).toRight(addNodesErrorMessage)
      } yield {
        c.elements = ns.toHtmlElements
        c.children = children
        c.tmpReactives = tmpRs
        c.eventHandlers = eIds
        c.watchInfos = watch(c)
        c
      }
    }
  }

  protected def mkSimpleHtmlEsWithAttrsCC(gen: => GenResult,
                                          attrs: Map[AttrName, String | Boolean],
                                          attrRs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
                                          watch: HtmlEsCC => WatchInfos = _ => Nil)
                                         (implicit root: HTMLElement,
                                          am: AddMethod): HtmlEsCC = {
    HtmlElementsComponentController { c =>
      for {
        // Scala 3.4.0~
        // (ns, children, tmpRs, eIds) <- gen
        t4 <- gen
        (ns, children, tmpRs, eIds) = t4
        eles = ns.toHtmlElements
        ele <- eles.headOption.toRight(notFoundHeadElementErrorMessage)
        _ <- addNodes(root)(am, ns *).toRight(addNodesErrorMessage)
      } yield {
        c.elements = eles
        c.children = children
        c.tmpReactives = tmpRs
        c.eventHandlers = eIds
        setAttrs(ele, attrs, attrRs)
        c
      }
    }
  }

  protected def mkSimpleHtmlEsInputCC[V](value: RV[V],
                                         gen: => GenResult,
                                         setToElement: HTMLElement => Unit,
                                         attrs: Map[AttrName, String | Boolean],
                                         attrRs: Map[AttrName, Reactive[String] | Reactive[Boolean]],
                                         watch: HtmlEsCC => WatchInfos = _ => Nil)
                                        (implicit root: HTMLElement,
                                         am: AddMethod): HtmlEsCC = {
    HtmlElementsComponentController { c =>
      for {
        // Scala 3.4.0~
        // (ns, children, tmpRs, eIds) <- gen
        t4 <- gen
        (ns, children, tmpRs, eIds) = t4
        eles = ns.toHtmlElements
        ele <- eles.headOption.toRight(notFoundHeadElementErrorMessage)
        _ <- addNodes(root)(am, eles *).toRight(addNodesErrorMessage)
      } yield {
        c.elements = List(ele)
        c.watchInfos = value.addWatcher(_ => setToElement(ele)) ::
          watch(c) :::
          attrRs.map(_._2.addWatcher(_ => setAttrs(ele, attrs, attrRs))).toList
        c.tmpReactives = tmpRs
        c.eventHandlers = eIds
        setToElement(ele)
        setAttrs(ele, attrs, attrRs)
        c
      }
    }
  }

}
