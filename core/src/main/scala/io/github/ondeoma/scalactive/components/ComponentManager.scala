package io.github.ondeoma.scalactive.components

import cats.syntax.all.*
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
  private var hooks: Option[LifecycleHooks] = None

  /** ライフサイクルフック(最初に使われたときに一時リアクティブ値として登録します) */
  private[scalactive] def lifecycle: LifecycleHooks = {
    hooks.getOrElse {
      val h = new LifecycleHooks
      hooks = Some(h)
      addTmpReactive(h)
      h
    }
  }

  def gen(): GenResult = genWithOwnLast().map(_._1)

  /**
   * genに加えて、HTML自身の最後のノード(子コンポーネントが足したノードを除く)を返します.
   * 子コンポーネントはプレースホルダーの後ろにノードを足すだけで、HTML自身のノードは取り除かないため、
   * HTML自身の最初と最後のノードはコンポーネントが片付けられるまで変わりません(forCの行の範囲に使います).
   */
  private[scalactive] def genWithOwnLast(): Either[String, ((List[Node], ChildrenComponents, TemporaryReactiveValues, EventHandlerIds), Node)] = {
    val tmpDiv = mkDiv()
    // 要素が空の場合は後に親ノードを特定できるようにダミーノードを含める。
    val nodes = html.toNodes.orDummyNode
    for {
      _ <- addNodes(tmpDiv)(AddMethod.append(tmpDiv), nodes *).toRight("ComponentManagerError")
      children = genChildren(tmpDiv)
      cs <- children.traverse(_.init())
      ns = tmpDiv.childNodes.toList
      _ = tmpDiv.remove()
      // 子コンポーネントの描画が終わった後に登録するので、onMountedは子から順に実行される
      _ = hooks.filter(_.mounted.nonEmpty).foreach(MountQueue.enqueue)
    } yield ((ns, cs, tmpReactives.toList, eventHandlerIds.toList), nodes.last)
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

  export io.github.ondeoma.scalactive.components.Slot
  export io.github.ondeoma.scalactive.components.ElementRef
  export io.github.ondeoma.scalactive.components.Teleport
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

  export io.github.ondeoma.scalactive.routes.Router

  export io.github.ondeoma.scalactive.utils.ConsoleUtil.*
  export io.github.ondeoma.scalactive.utils.DomUtil.*
  export io.github.ondeoma.scalactive.utils.TypeAlias.*

  def apply(f: ComponentManager => HTML): GenResult = {
    new ComponentManager(f).gen()
  }

  private[scalactive] def genWithOwnLast(f: ComponentManager => HTML) = {
    new ComponentManager(f).genWithOwnLast()
  }

  /**
   * 要素をElementRefに結び付けます(テンプレート参照). 例: `<input ${ref(input)}>`
   * 要素が作られたときに設定され、コンポーネントが片付けられると空に戻ります.
   */
  def ref[E <: Element](r: ElementRef[E])
                       (using cc: ComponentManager): ATTR = {
    el(ele => AttrController { ac =>
      ac.element = Some(ele)
      r.set(Some(ele.asInstanceOf[E]))
      ac.onClear = () => if (r.get.exists(_ eq ele)) r.set(None)
      Right(ac)
    })
  }

  /**
   * コンポーネントがDOMに挿入された後に実行する処理を登録します(Vueのmounted).
   * 現在の処理(描画・描き直し)が終わった直後に、子コンポーネントのものから順に実行されます.
   * 実行前にコンポーネントが片付けられた場合は実行しません.
   */
  def onMounted(f: => Unit)
               (using cm: ComponentManager): Unit = {
    cm.lifecycle.mounted += (() => f)
  }

  /**
   * コンポーネントが片付けられた(DOMから取り除かれた)後に実行する処理を登録します(Vueのunmounted).
   * 描き直し・ifCの切り替え・forCの行の削除などでも実行されます. タイマーや外部のイベントの解除などに使います.
   */
  def onUnmounted(f: => Unit)
                 (using cm: ComponentManager): Unit = {
    cm.lifecycle.unmounted += (() => f)
  }

  def %(f: GenComponentF)
       (using cc: ComponentManager): HTML = {
    componentWithComment(f)
  }

  def c(f: GenComponentF)
       (using cc: ComponentManager): HTML = {
    componentWithComment(f)
  }

  /**
   * 子コンポーネントを展開し、そのルート要素(最初の要素)にattrsを付け加えます(属性の引き継ぎ).
   * 子が描き直されたときも、新しいルート要素に付け直します.
   * attrsにはStRuntimeのrootCls/rootStyle/rootAttrや、要素に効果を付けるコントローラーを渡します.
   */
  def c(f: GenComponentF,
        attrs: ElementEffectF*)
       (using cc: ComponentManager): HTML = {
    if (attrs.isEmpty) componentWithComment(f)
    else componentWithComment((parent, am) => {
      val ctrl = f(parent, am)
      ctrl match {
        case n: NodesComponentController =>
          n.afterInit = n.afterInit :+ { (nc: NodesComponentController) =>
            nc.elements.headOption match {
              case Some(root) =>
                attrs.map(_(root)).toList.traverse(_.init()) match {
                  case Right(cs) => nc.children = nc.children ++ cs
                  case Left(e) => io.github.ondeoma.scalactive.utils.ConsoleUtil.error(e)
                }
              case None => io.github.ondeoma.scalactive.utils.ConsoleUtil.warn("attributes on a component without a root element are ignored")
            }
          }
        case _ => io.github.ondeoma.scalactive.utils.ConsoleUtil.warn("attributes on this component are ignored")
      }
      ctrl
    })
  }

  def componentWithComment(f: GenComponentF)
                          (using cc: ComponentManager): HTML = {
    val id = "cm-" + randomUUID()
    cc.bufGC.addOne(id -> f)
    // language=html
    s"<!--$id-->"
  }

  def el(fs: ElementEffectF*)
        (using cc: ComponentManager): ATTR = {
    elementByDataAttr(fs *)
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
    el(ClassBooleanController(rv, vs.toList))
  }

  def clsS(rv: Reactive[String])
          (using cc: ComponentManager): ATTR = {
    el(ClassStringController(rv))
  }

  def clsSs(rv: Reactive[List[String]])
           (using cc: ComponentManager): ATTR = {
    el(ClassStringsController(rv))
  }

  def clsTgl(rv: Reactive[Boolean],
             ifTrue: String,
             ifFalse: String)
            (using cc: ComponentManager): ATTR = {
    clsTgl(rv, List(ifTrue), List(ifFalse))
  }

  def clsTgl(rv: Reactive[Boolean],
             ifTrue: List[String],
             ifFalse: List[String])
            (using cc: ComponentManager): ATTR = {
    el(ClassToggleController(rv, ifTrue, ifFalse))
  }

  def styleIf(rv: Reactive[Boolean],
              name: String,
              value: String)
             (using cc: ComponentManager): ATTR = {
    el(StyleBooleanController(rv, name, value))
  }

  def styleS(name: String,
             rv: Reactive[String])
            (using cc: ComponentManager): ATTR = {
    el(StyleStringController(name, rv))
  }

  def styleTgl(rv: Reactive[Boolean],
               name: String,
               ifTrue: String,
               ifFalse: String)
              (using cc: ComponentManager): ATTR = {
    el(StyleToggleController(rv, name, ifTrue, ifFalse))
  }

  def attrIf(rv: Reactive[Boolean],
             name: String)
            (using cc: ComponentManager): ATTR = {
    el(AttrBooleanController(name, rv, ""))
  }

  def attrS(name: String,
            rv: Reactive[String])
           (using cc: ComponentManager): ATTR = {
    el(AttrStringController(name, rv))
  }
  
  def showIf(rv: Reactive[Boolean],
             showStyle: String = "block")
            (using cc: ComponentManager): ATTR = {
    el(StyleToggleController(rv, "display", showStyle, "none"))
  }

  def showFlexIf(rv: Reactive[Boolean])
                (using cc: ComponentManager): ATTR = {
    showIf(rv, "flex")
  }

  def showGridIf(rv: Reactive[Boolean])
                (using cc: ComponentManager): ATTR = {
    showIf(rv, "grid")
  }

  def showIfBriefly(rv: Reactive[Boolean],
                    duration: FiniteDuration,
                    showStyle: String = "block")
                   (using cc: ComponentManager): ATTR = {
    el(StyleBrieflyController(rv, duration, "display", showStyle, "none"))
  }

  /**
   * 中身を別の要素(toのセレクター)の末尾に描画します(VueのTeleport、ReactのPortal).
   * 例: `teleportC("body") { implicit cm => "<div class='modal'>...</div>" }`
   */
  def teleportC(to: String,
                disabled: Boolean = false)
               (genHtml: ComponentManager => HTML)
               (using ComponentManager): HTML = {
    %(Teleport(to, disabled, Slot(genHtml))(_, _))
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

  // forEachC: 添字を渡さないforC. 行の位置が変わっても作り直さず使い回すため、削除や並べ替えが速くなります.
  // (forCは、添字を使う行を正しく描くために、位置が変わった行を作り直します)

  def forEachC[A](forV: List[A])
                 (genHtml: (ComponentManager, A) => HTML)
                 (using ComponentManager): HTML = {
    %(ForStaticComponent(forV, usesIndex = false)((cm, a, _) => genHtml(cm, a)))
  }

  def forEachC[A](forV: RVList[A])
                 (genHtml: (ComponentManager, RV[A]) => HTML)
                 (using ComponentManager): HTML = {
    %(ForRVComponent(forV, usesIndex = false)((cm, a, _) => genHtml(cm, a)))
  }

  def forEachC[A, RM <: ReactiveModel[A, RM]](forV: RMList[A, RM])
                                             (genHtml: (ComponentManager, RM) => HTML)
                                             (using ComponentManager): HTML = {
    %(ForRMComponent(forV, usesIndex = false)((cm, a, _) => genHtml(cm, a)))
  }

  def forEachC[A](forV: CRV[List[A]])
                 (genHtml: (ComponentManager, A) => HTML)
                 (using ComponentManager): HTML = {
    %(ForLRComponent(forV, usesIndex = false)((cm, a, _) => genHtml(cm, a)))
  }

  def forEachC[A](forV: RV[List[A]])
                 (genHtml: (ComponentManager, A) => HTML)
                 (using ComponentManager): HTML = {
    %(ForLRComponent(forV, usesIndex = false)((cm, a, _) => genHtml(cm, a)))
  }

  def forEachC[A](forV: ReactiveList[A])
                 (genHtml: (ComponentManager, A) => HTML)
                 (using ComponentManager): HTML = {
    %(ForLRComponent(forV, usesIndex = false)((cm, a, _) => genHtml(cm, a)))
  }

  // key付きのforC/forEachC: keyで行を対応付けます(Vueのv-forの:key).
  // キーが同じでも値が変わった行は作り直します.

  def forC[A](forV: List[A], key: A => Any)
             (genHtml: (ComponentManager, A, IDX) => HTML)
             (using ComponentManager): HTML = {
    %(ForStaticComponent(forV, true, key)(genHtml))
  }

  def forC[A](forV: RVList[A], key: RV[A] => Any)
             (genHtml: (ComponentManager, RV[A], IDX) => HTML)
             (using ComponentManager): HTML = {
    %(ForRVComponent(forV, true, key)(genHtml))
  }

  def forC[A, RM <: ReactiveModel[A, RM]](forV: RMList[A, RM], key: RM => Any)
                                         (genHtml: (ComponentManager, RM, IDX) => HTML)
                                         (using ComponentManager): HTML = {
    %(ForRMComponent(forV, true, key)(genHtml))
  }

  def forC[A](forV: CRV[List[A]], key: A => Any)
             (genHtml: (ComponentManager, A, IDX) => HTML)
             (using ComponentManager): HTML = {
    %(ForLRComponent(forV, true, key)(genHtml))
  }

  def forC[A](forV: RV[List[A]], key: A => Any)
             (genHtml: (ComponentManager, A, IDX) => HTML)
             (using ComponentManager): HTML = {
    %(ForLRComponent(forV, true, key)(genHtml))
  }

  def forC[A](forV: ReactiveList[A], key: A => Any)
             (genHtml: (ComponentManager, A, IDX) => HTML)
             (using ComponentManager): HTML = {
    %(ForLRComponent(forV, true, key)(genHtml))
  }

  def forEachC[A](forV: List[A], key: A => Any)
                 (genHtml: (ComponentManager, A) => HTML)
                 (using ComponentManager): HTML = {
    %(ForStaticComponent(forV, false, key)((cm, a, _) => genHtml(cm, a)))
  }

  def forEachC[A](forV: RVList[A], key: RV[A] => Any)
                 (genHtml: (ComponentManager, RV[A]) => HTML)
                 (using ComponentManager): HTML = {
    %(ForRVComponent(forV, false, key)((cm, a, _) => genHtml(cm, a)))
  }

  def forEachC[A, RM <: ReactiveModel[A, RM]](forV: RMList[A, RM], key: RM => Any)
                                             (genHtml: (ComponentManager, RM) => HTML)
                                             (using ComponentManager): HTML = {
    %(ForRMComponent(forV, false, key)((cm, a, _) => genHtml(cm, a)))
  }

  def forEachC[A](forV: CRV[List[A]], key: A => Any)
                 (genHtml: (ComponentManager, A) => HTML)
                 (using ComponentManager): HTML = {
    %(ForLRComponent(forV, false, key)((cm, a, _) => genHtml(cm, a)))
  }

  def forEachC[A](forV: RV[List[A]], key: A => Any)
                 (genHtml: (ComponentManager, A) => HTML)
                 (using ComponentManager): HTML = {
    %(ForLRComponent(forV, false, key)((cm, a, _) => genHtml(cm, a)))
  }

  def forEachC[A](forV: ReactiveList[A], key: A => Any)
                 (genHtml: (ComponentManager, A) => HTML)
                 (using ComponentManager): HTML = {
    %(ForLRComponent(forV, false, key)((cm, a, _) => genHtml(cm, a)))
  }

  def ev(et: EventType | List[EventType],
         handler: Event => Unit)
        (using cc: ComponentManager): ATTR = {
    et match {
      case et: EventType =>
        el(EventController(et, handler))
      case ets: List[EventType] =>
        el(ets.map(EventController(_, handler)) *)
    }
  }

  def ev0(et: EventType | List[EventType],
          handler: => Unit)
         (using cc: ComponentManager): ATTR = {
    val fixedHandler = (e: Event) => handler
    et match {
      case et: EventType =>
        el(EventController(et, fixedHandler))
      case ets: List[EventType] =>
        el(ets.map(EventController(_, fixedHandler)) *)
    }
  }

  def evClick(handler: => Unit)
             (using cc: ComponentManager): ATTR = {
    ev0(EventType.click, handler)
  }

}

