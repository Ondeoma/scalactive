package io.github.ondeoma.scalactive.components

import io.github.ondeoma.scalactive.controllers.*
import io.github.ondeoma.scalactive.enums.EventType
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.*
import io.github.ondeoma.scalactive.utils
import io.github.ondeoma.scalactive.utils.ConsoleUtil
import org.scalajs.dom.*

trait BaseComponent {

  val ComponentManager = io.github.ondeoma.scalactive.components.ComponentManager(_)

  export io.github.ondeoma.scalactive.components.ComponentManager.*
  
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
                                 watch: NodesComponentController => WatchInfos = _ => Nil)
                                (implicit root: HTMLElement,
                                 am: AddMethod): NodesComponentController = {
    NodesComponentController { c =>
      for {
        // scala 3.4.0~
        // (ns, children, tmpRs, eIds) <- gen
        t4 <- gen
        (ns, children, tmpRs, eIds) = t4
        fixedNs = ns.orDummyNode
        _ <- addNodes(root)(am, fixedNs *).toRight(addNodesErrorMessage)
      } yield {
        c.nodes = fixedNs
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
                                          watch: NodesComponentController => WatchInfos = _ => Nil)
                                         (implicit root: HTMLElement,
                                          am: AddMethod): NodesComponentController = {
    NodesComponentController { c =>
      for {
        // Scala 3.4.0~
        // (ns, children, tmpRs, eIds) <- gen
        t4 <- gen
        (ns, children, tmpRs, eIds) = t4
        eles = ns.toHtmlElements
        ele <- eles.headOption.toRight(notFoundHeadElementErrorMessage)
        _ <- addNodes(root)(am, ns *).toRight(addNodesErrorMessage)
      } yield {
        c.nodes = ns
        c.watchInfos = watch(c) ::: attrRs.map(_._2.addWatcher(_ => setAttrs(ele, attrs, attrRs))).toList
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
                                         watch: NodesComponentController => WatchInfos = _ => Nil)
                                        (implicit root: HTMLElement,
                                         am: AddMethod): NodesComponentController = {
    NodesComponentController { c =>
      for {
        // Scala 3.4.0~
        // (ns, children, tmpRs, eIds) <- gen
        t4 <- gen
        (ns, children, tmpRs, eIds) = t4
        eles = ns.toHtmlElements
        ele <- eles.headOption.toRight(notFoundHeadElementErrorMessage)
        _ <- addNodes(root)(am, eles *).toRight(addNodesErrorMessage)
      } yield {
        c.nodes = List(ele)
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
