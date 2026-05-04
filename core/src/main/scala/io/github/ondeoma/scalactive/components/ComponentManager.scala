package io.github.ondeoma.scalactive.components

import cats.syntax.all.*
import io.github.ondeoma.scalactive.components.*
import io.github.ondeoma.scalactive.components.fors.*
import io.github.ondeoma.scalactive.components.ifs.*
import io.github.ondeoma.scalactive.components.inputs.*
import io.github.ondeoma.scalactive.components.texts.*
import io.github.ondeoma.scalactive.components.utils.*
import io.github.ondeoma.scalactive.controllers.*
import io.github.ondeoma.scalactive.controllers.attrs.*
import io.github.ondeoma.scalactive.controllers.events.*
import io.github.ondeoma.scalactive.enums.*
import io.github.ondeoma.scalactive.facades.Crypto.*
import io.github.ondeoma.scalactive.models.*
import io.github.ondeoma.scalactive.models.Selector.*
import io.github.ondeoma.scalactive.reactive.*
import io.github.ondeoma.scalactive.utils.DomUtil.*
import io.github.ondeoma.scalactive.utils.TypeAlias.*
import org.scalajs.dom.*

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
    // 要素が空の場合は後に親ノードを特定できるようにダミーノードを含める。
    val nodes = html.toNodes.orDummyNode 
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

  export io.github.ondeoma.scalactive.components.fors.ForGeneralComponent
  export io.github.ondeoma.scalactive.components.fors.ForLRComponent
  export io.github.ondeoma.scalactive.components.fors.ForRMComponent
  export io.github.ondeoma.scalactive.components.fors.ForRVComponent
  export io.github.ondeoma.scalactive.components.fors.ForStaticComponent
  export io.github.ondeoma.scalactive.components.htmls.HtmlComponent
  export io.github.ondeoma.scalactive.components.htmls.Html2Component
  export io.github.ondeoma.scalactive.components.htmls.Html3Component
  export io.github.ondeoma.scalactive.components.htmls.Html4Component
  export io.github.ondeoma.scalactive.components.htmls.Html5Component
  export io.github.ondeoma.scalactive.components.htmls.Html6Component
  export io.github.ondeoma.scalactive.components.ifs.IfComponent
  export io.github.ondeoma.scalactive.components.inputs.buttons.ButtonComponent
  export io.github.ondeoma.scalactive.components.inputs.checkboxes.CheckboxBooleanComponent
  export io.github.ondeoma.scalactive.components.inputs.checkboxes.CheckboxGeneralComponent
  export io.github.ondeoma.scalactive.components.inputs.checkboxes.CheckboxGroupPartComponent
  export io.github.ondeoma.scalactive.components.inputs.files.FileInputComponent
  export io.github.ondeoma.scalactive.components.inputs.links.LinkComponent
  export io.github.ondeoma.scalactive.components.inputs.links.LinkStaticComponent
  export io.github.ondeoma.scalactive.components.inputs.radios.RadioGeneralComponent
  export io.github.ondeoma.scalactive.components.inputs.radios.RadioStringComponent
  export io.github.ondeoma.scalactive.components.inputs.selects.SelectGeneralComponent
  export io.github.ondeoma.scalactive.components.inputs.selects.SelectStringComponent
  export io.github.ondeoma.scalactive.components.inputs.textareas.TextAreaComponent
  export io.github.ondeoma.scalactive.components.inputs.texts.TextInputGeneralComponent
  export io.github.ondeoma.scalactive.components.inputs.texts.TextInputStringComponent
  export io.github.ondeoma.scalactive.components.texts.TextComponent
  export io.github.ondeoma.scalactive.components.texts.Text2Component
  export io.github.ondeoma.scalactive.components.texts.Text3Component
  export io.github.ondeoma.scalactive.components.texts.Text4Component
  export io.github.ondeoma.scalactive.components.texts.Text5Component
  export io.github.ondeoma.scalactive.components.texts.Text6Component
  
  export io.github.ondeoma.scalactive.controllers.NodesComponentController
  export io.github.ondeoma.scalactive.controllers.TextComponentController
  export io.github.ondeoma.scalactive.controllers.events.EventController

  export io.github.ondeoma.scalactive.enums.AdjacentPosition
  export io.github.ondeoma.scalactive.enums.EventType
  export io.github.ondeoma.scalactive.enums.HtmlInputType
  export io.github.ondeoma.scalactive.enums.HtmlInputType.StringInputType
  export io.github.ondeoma.scalactive.enums.SelectorAttributeOperator
  export io.github.ondeoma.scalactive.enums.SelectorCaseInsensitive
  export io.github.ondeoma.scalactive.enums.SelectorRelationCombinator
  export io.github.ondeoma.scalactive.models.AddMethod
  export io.github.ondeoma.scalactive.models.Selector
  export io.github.ondeoma.scalactive.models.Selector.*
  export io.github.ondeoma.scalactive.models.WatchInfo

  export io.github.ondeoma.scalactive.reactive.CRV
  export io.github.ondeoma.scalactive.reactive.ReactiveList
  export io.github.ondeoma.scalactive.reactive.RMList
  export io.github.ondeoma.scalactive.reactive.RVList
  export io.github.ondeoma.scalactive.reactive.Reactive
  export io.github.ondeoma.scalactive.reactive.ReactiveModel
  export io.github.ondeoma.scalactive.reactive.ReactiveModelMacro
  export io.github.ondeoma.scalactive.reactive.ReactiveModelMacro.*
  export io.github.ondeoma.scalactive.reactive.RMCompatible
  export io.github.ondeoma.scalactive.reactive.RMCompatible.*
  export io.github.ondeoma.scalactive.reactive.RV

  export io.github.ondeoma.scalactive.utils.ConsoleUtil.*
  export io.github.ondeoma.scalactive.utils.DomUtil.*
  export io.github.ondeoma.scalactive.utils.TypeAlias.*

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

  def forC[A](forV: RVList[A])
             (genHtml: (ComponentManager, RV[A], IDX) => HTML)
             (using ComponentManager): HTML = {
    %(ForRVComponent(forV)(genHtml))
  }

  def forC[A, RM <: ReactiveModel[A, RM]](forV: RMList[A, RM])
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
  
  def forC[A](forV: ReactiveList[A])
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

