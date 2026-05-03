package io.github.ondeoma.scalactive.components

import cats.syntax.all.*
import io.github.ondeoma.scalactive.components.fors.{ForLRComponent, ForRMComponent, ForRVComponent, ForStaticComponent}
import org.scalajs.dom.*
import io.github.ondeoma.scalactive.reactive.*
import io.github.ondeoma.scalactive.syntax.All.*
import io.github.ondeoma.scalactive.utils.TypeAlias.*
import io.github.ondeoma.scalactive.components.ifs.IfComponent
import io.github.ondeoma.scalactive.controllers.ComponentController
import io.github.ondeoma.scalactive.controllers.attrs.{ClassBooleanController, ClassStringController, ClassStringsController, StyleBrieflyController, StyleToggleController}
import io.github.ondeoma.scalactive.controllers.events.EventController
import io.github.ondeoma.scalactive.enums.{EventType, SelectorAttributeOperator}
import io.github.ondeoma.scalactive.models.AddMethod
import io.github.ondeoma.scalactive.reactive.{CRV, ListRM, ListRV, RV, Reactive, ReactiveModel}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.FiniteDuration


class ComponentManager private(genHtml: ComponentManager => HTML) {

  private lazy val bufGC = ListBuffer[(ID, GenComponentF)]()
  private lazy val bufEE = ListBuffer[(ID, ElementEffectF)]()
  private lazy val html: String = genHtml(this)
  private lazy val tmpReactives = ListBuffer[Reactive[?]]()
  private lazy val eventHandlerIds = ListBuffer[ID]()

  def gen(): GenResult = {
    val tmpDiv = mkDiv()
    val nodes = html.toNodes
    for {
      _ <- addNodes(tmpDiv)(AddMethod.append(tmpDiv), nodes *).toRight("ComponentManagerError")
      children = genChildren(tmpDiv)
      cs <- children.traverse(_.init())
      ns = tmpDiv.childNodes.toList
      _ = tmpDiv.remove()
    } yield (ns, cs, tmpReactives.toList, eventHandlerIds.toList)
  }

  private def genChildren(ele: HTMLElement): List[ComponentController[?]] = {
    bufGC.toList.map { (commentId, gen) =>
      gen(ele, AddMethod.afterC(commentId))
    }
  } ::: bufEE.toList.flatMap { (dataId, gen) =>
    val attr = s"data-$dataId"
    if (ele.hasAttribute(attr)) {
      Some(gen(ele))
    } else withElement(ele)(AttrSelector(attr, SelectorAttributeOperator.Has), el => {
      gen(el)
    })
  }

  def addTmpReactive(rv: Reactive[?]): Unit = {
    tmpReactives.addOne(rv)
  }

  def addEventHandlers(id: ID): Unit = {
    eventHandlerIds.addOne(id)
  }

}

object ComponentManager {

  def apply(f: ComponentManager => HTML): GenResult = {
    new ComponentManager(f).gen()
  }

  def %(f: GenComponentF)
       (using cc: ComponentManager): HTML = {
    componentWithComment(f)
  }

  def c(f: GenComponentF)
       (using cc: ComponentManager): HTML = {
    componentWithComment(f)
  }

  def componentWithComment(f: GenComponentF)
                          (using cc: ComponentManager): HTML = {
    val id = "cm-" + randomUUID()
    cc.bufGC.addOne(id -> f)
    // language=html
    s"<!--$id-->"
  }

  def el(f: ElementEffectF)
        (using cc: ComponentManager): ATTR = {
    elementByDataAttr(f)
  }

  def elementByDataAttr(fs: ElementEffectF*)
                       (using cc: ComponentManager): ATTR = {
    val id = "cm-" + randomUUID()
    fs.foreach(f => cc.bufEE.addOne(id -> f))
    // language=html
    s""" data-$id"""
  }

  def clsIf(rv: Reactive[Boolean],
            vs: String*)
           (using cc: ComponentManager): ATTR = {
    classReactiveBoolean(rv, vs *)
  }

  def classReactiveBoolean(rv: Reactive[Boolean],
                           vs: String*)
                          (using cc: ComponentManager): ATTR = {
    elementByDataAttr(ClassBooleanController(rv, vs.toList))
  }

  def showIf(rv: Reactive[Boolean],
             showStyle: String = "block")
            (using cc: ComponentManager): ATTR = {
    elementByDataAttr(StyleToggleController(rv, "display", showStyle, "none"))
  }

  def showIfBriefly(rv: Reactive[Boolean],
                    duration: FiniteDuration,
                    showStyle: String = "block")
                   (using cc: ComponentManager): ATTR = {
    elementByDataAttr(StyleBrieflyController(rv, duration, "display", showStyle, "none"))
  }

  def clsS(rv: Reactive[String])
          (using cc: ComponentManager): ATTR = {
    classReactiveString(rv)
  }

  def classReactiveString(rv: Reactive[String])
                         (using cc: ComponentManager): ATTR = {
    elementByDataAttr(ClassStringController(rv))
  }

  def clsSs(rv: Reactive[List[String]])
           (using cc: ComponentManager): ATTR = {
    classReactiveStrings(rv)
  }

  def classReactiveStrings(rv: Reactive[List[String]])
                          (using cc: ComponentManager): ATTR = {
    elementByDataAttr(ClassStringsController(rv))
  }

  def ifC(ifV: Reactive[Boolean])
         (genHtml: ComponentManager => HTML)
         (using ComponentManager): HTML = {
    %(IfComponent(ifV)(genHtml))
  }

  def forC[A](forV: List[A])
             (genHtml: (ComponentManager, A, IDX) => HTML)
             (using ComponentManager): HTML = {
    %(ForStaticComponent(forV)(genHtml))
  }

  def forC[A](forV: ListRV[A])
             (genHtml: (ComponentManager, RV[A], IDX) => HTML)
             (using ComponentManager): HTML = {
    %(ForRVComponent(forV)(genHtml))
  }

  def forC[A, RM <: ReactiveModel[A, RM]](forV: ListRM[A, RM])
                                         (genHtml: (ComponentManager, RM, IDX) => HTML)
                                         (using ComponentManager): HTML = {
    %(ForRMComponent(forV)(genHtml))
  }

  def forC[A](forV: CRV[List[A]])
             (genHtml: (ComponentManager, A, IDX) => HTML)
             (using ComponentManager): HTML = {
    %(ForLRComponent(forV)(genHtml))
  }

  def forC[A](forV: RV[List[A]])
             (genHtml: (ComponentManager, A, IDX) => HTML)
             (using ComponentManager): HTML = {
    %(ForLRComponent(forV)(genHtml))
  }

  def ev(et: EventType | List[EventType],
         handler: Event => Unit)
        (using cc: ComponentManager): ATTR = {
    et match {
      case et: EventType =>
        elementByDataAttr(EventController(et, handler))
      case ets: List[EventType] =>
        elementByDataAttr(ets.map(EventController(_, handler)) *)
    }
  }

  def ev0(et: EventType | List[EventType],
          handler: => Unit)
         (using cc: ComponentManager): ATTR = {
    val fixedHandler = (e: Event) => handler
    et match {
      case et: EventType =>
        elementByDataAttr(EventController(et, fixedHandler))
      case ets: List[EventType] =>
        elementByDataAttr(ets.map(EventController(_, fixedHandler)) *)
    }
  }

  def evClick(handler: => Unit)
             (using cc: ComponentManager): ATTR = {
    ev0(EventType.click, handler)
  }

}

