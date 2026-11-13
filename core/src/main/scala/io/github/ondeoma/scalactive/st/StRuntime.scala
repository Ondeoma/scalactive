package io.github.ondeoma.scalactive.st

import io.github.ondeoma.scalactive.components.{ComponentManager, ScopedSlot, Slot}
import io.github.ondeoma.scalactive.components.htmls.HtmlComponent
import io.github.ondeoma.scalactive.components.texts.TextComponent
import io.github.ondeoma.scalactive.controllers.attrs.{AttrBooleanController, AttrController, AttrStringController, ClassStringsController, FormProps}
import io.github.ondeoma.scalactive.controllers.events.EventController
import io.github.ondeoma.scalactive.enums.EventType
import io.github.ondeoma.scalactive.reactive.*
import io.github.ondeoma.scalactive.utils.DomUtil.addEventToElement
import io.github.ondeoma.scalactive.utils.TypeAlias.*
import org.scalajs.dom.*

import scala.annotation.implicitNotFound
import scala.collection.mutable
import scala.scalajs.js

/**
 * .st(単一ファイルコンポーネント)からsbt-scalactiveが生成するコードが利用するヘルパー.
 * 値がリアクティブかどうかをオーバーロードで判定し、既存のコンポーネント/コントローラーへ振り分けます.
 */
object StRuntime {

  private val injectedStyles = mutable.Set[String]()

  /**
   * <style>ブロックの内容をheadに1度だけ挿入します.
   */
  def injectStyle(id: String,
                  css: String): Unit = {
    if (!injectedStyles.contains(id)) {
      injectedStyles.addOne(id)
      val style = document.createElement("style")
      style.setAttribute("data-st", id)
      style.textContent = css
      document.head.appendChild(style)
    }
  }

  // テンプレートの式の追跡
  // Reactive型でない式(`s.count.v + 1` など)は、評価中に読んだリアクティブ値(`.v`)を監視し、変わったら計算し直します(Vueのテンプレートと同じ).
  // 何も読んでいない式は、これまでどおり1度だけ評価する静的な値として扱います. 追跡したくない部分は `untracked(...)` で囲みます.

  /** 式を評価し、リアクティブ値を読んでいればcomputed(Right)、読んでいなければ値(Left)を返します. */
  private def tracked[A](v: => A)
                        (using cm: ComponentManager): Either[A, Reactive[A]] = {
    Computed.track(() => v).map { c =>
      cm.addTmpReactive(c)
      c
    }
  }

  /** 式を評価し、リアクティブ値として返します(何も読んでいなければ変化しない値). */
  private def trackedR[A](v: => A)
                         (using cm: ComponentManager): Reactive[A] = {
    tracked(v).fold(new RV(_), identity)
  }

  def escape(s: String): String = {
    s.replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&#39;")
  }

  // {{ expr }}

  def text[A](rv: Reactive[A])
             (using cm: ComponentManager): HTML = {
    ComponentManager.%(TextComponent(rv)(String.valueOf(_)))
  }

  def text(v: => Any)
          (using cm: ComponentManager): HTML = {
    tracked(v) match {
      case Left(x) => escape(String.valueOf(x))
      case Right(r) => text(r)
    }
  }

  // v-html="expr"
  // 値をHTMLとしてそのまま展開します(エスケープしません). 信頼できない値を渡さないでください.

  def html(rv: Reactive[String])
          (using cm: ComponentManager): HTML = {
    ComponentManager.%(HtmlComponent(rv)(identity))
  }

  def html[A](rv: Reactive[A])
             (using d: DummyImplicit,
              cm: ComponentManager): HTML = {
    ComponentManager.%(HtmlComponent(rv)(a => if (a == null) "" else String.valueOf(a)))
  }

  def html(v: => Any)
          (using cm: ComponentManager): HTML = {
    tracked(v) match {
      case Left(x) => if (x == null) "" else String.valueOf(x)
      case Right(r) => html(r)
    }
  }

  // v-if / v-else-if / v-else / v-show

  def cond(rv: Reactive[Boolean]): Reactive[Boolean] = rv

  def cond(b: => Boolean)
          (using cm: ComponentManager): Reactive[Boolean] = trackedR(b)

  def not(rv: Reactive[Boolean])
         (using cm: ComponentManager): Reactive[Boolean] = {
    CRV(rv, (b: Boolean) => !b)
  }

  /**
   * 複数のリアクティブ値から計算される真偽値. 値が変わった時のみ通知します.
   * 一時リアクティブ値として登録され、abort時に元の値の監視を解除します.
   */
  private final class Branch(srcs: Seq[Reactive[Boolean]],
                             calc: () => Boolean) extends RV[Boolean](calc()) {

    private val infos = srcs.map(_.addWatcher { _ =>
      val n = calc()
      if (n != v) this := n
    })

    override def abort(): Unit = {
      infos.foreach(_.abort())
      super.abort()
    }

  }

  private def branch(srcs: Seq[Reactive[Boolean]],
                     calc: () => Boolean)
                    (using cm: ComponentManager): Reactive[Boolean] = {
    val b = new Branch(srcs, calc)
    cm.addTmpReactive(b)
    b
  }

  /** v-else-if: 先行する条件(prev)がすべて偽で、cが真の場合に真. */
  def elseIf(c: Reactive[Boolean],
             prev: Reactive[Boolean]*)
            (using cm: ComponentManager): Reactive[Boolean] = {
    branch(prev :+ c, () => !prev.exists(_.v) && c.v)
  }

  /** v-else: 先行する条件(prev)がすべて偽の場合に真. */
  def otherwise(prev: Reactive[Boolean]*)
               (using cm: ComponentManager): Reactive[Boolean] = {
    branch(prev, () => !prev.exists(_.v))
  }

  // :name="expr"

  def attr(name: String,
           rv: Reactive[String])
          (using cm: ComponentManager): ATTR = {
    ComponentManager.attrS(name, rv)
  }

  def attr(name: String,
           rv: Reactive[Boolean])
          (using d: DummyImplicit,
           cm: ComponentManager): ATTR = {
    ComponentManager.attrIf(rv, name)
  }

  def attr[A](name: String,
              rv: Reactive[A])
             (using d1: DummyImplicit,
              d2: DummyImplicit,
              cm: ComponentManager): ATTR = {
    ComponentManager.attrS(name, CRV(rv, (a: A) => String.valueOf(a)))
  }

  def attr(name: String,
           v: => Any)
          (using cm: ComponentManager): ATTR = {
    tracked(v) match {
      case Left(true) => s" $name"
      case Left(false | null | None) => ""
      case Left(Some(x)) => s""" $name="${escape(String.valueOf(x))}""""
      case Left(x) => s""" $name="${escape(String.valueOf(x))}""""
      case Right(r) => ComponentManager.el(anyAttrEffect(name, r))
    }
  }

  /** 属性に値を設定します. trueは値なし、false・null・Noneは属性を外し、Some(x)・それ以外は文字列にします. */
  private def setAnyAttr(ele: HTMLElement, name: String, v: Any): Unit = v match {
    case true => FormProps.set(ele, name, "")
    case false | null | None => FormProps.remove(ele, name)
    case Some(x) => FormProps.set(ele, name, String.valueOf(x))
    case x => FormProps.set(ele, name, String.valueOf(x))
  }

  private def anyAttrEffect(name: String,
                            r: Reactive[Any]): ElementEffectF = ele => AttrController { ac =>
    ac.element = Some(ele)
    setAnyAttr(ele, name, r.v)
    ac.watchInfos = List(r.addWatcher { case (_, n) => setAnyAttr(ele, name, n) })
    Right(ac)
  }

  // :class="expr"
  // 静的なclass属性と併用できるよう、常にクラスコントローラー経由で付与します.

  private def splitClasses(s: String): List[String] = {
    s.split("\\s+").toList.filter(_.nonEmpty)
  }

  def cls(rv: Reactive[String])
         (using cm: ComponentManager): ATTR = {
    ComponentManager.clsSs(CRV(rv, splitClasses))
  }

  def cls(rv: Reactive[List[String]])
         (using d: DummyImplicit,
          cm: ComponentManager): ATTR = {
    ComponentManager.clsSs(rv)
  }

  /** 式の値が真か(Reactive[Boolean]・Boolean・Option). */
  private def truthy(v: Any): Boolean = v match {
    case r: Reactive[?] => truthy(r.v)
    case b: Boolean => b
    case None | null => false
    case _ => true
  }

  /**
   * クラス名と条件の組・Map(オブジェクト記法)、またはそれと文字列を混ぜたリスト.
   * 条件はBooleanまたはReactive[Boolean]で、Reactiveの場合は変化に追従します.
   * 例: `"active" -> s.on`、`Map("active" -> s.on, "big" -> true)`、`List("btn", "active" -> s.on)`
   */
  def cls(rv: Reactive[Map[String, Boolean]])
         (using d1: DummyImplicit,
          d2: DummyImplicit,
          cm: ComponentManager): ATTR = {
    ComponentManager.clsSs(CRV(rv, (m: Map[String, Boolean]) => m.toList.collect { case (k, true) => splitClasses(k) }.flatten))
  }

  def cls(v: => Any)
         (using cm: ComponentManager): ATTR = {
    ComponentManager.clsSs(trackedR(classesOf(v)))
  }

  /**
   * :classの値(文字列・リスト・組・Map)から、付与するクラスのリストを求めます.
   * 条件のReactiveは `.v` で読むので、trackedの中で呼べば変化に追従します.
   */
  private def classesOf(v: Any): List[String] = {
    // 文字列の要素はそのまま、組の要素は条件付き
    val entries: List[Either[String, (String, Any)]] = v match {
      case s: String => List(Left(s))
      case (k: String, c) => List(Right((k, c)))
      case xs: Iterable[?] => xs.toList.map {
        case (k: String, c) => Right((k, c))
        case x => Left(String.valueOf(x))
      }
      case x => List(Left(String.valueOf(x)))
    }
    entries.collect { case Left(s) => splitClasses(s) }.flatten ++
      entries.collect { case Right((k, c)) if truthy(c) => splitClasses(k) }.flatten
  }

  // :style="expr"

  /** fontSize → font-size(カスタムプロパティ --x はそのまま) */
  private def cssName(name: String): String = {
    if (name.startsWith("--")) name
    else name.flatMap(c => if (c.isUpper) s"-${c.toLower}" else c.toString)
  }

  /** スタイルの値. None・null・false・空文字はプロパティを外す. */
  private def styleValue(v: Any): Option[String] = v match {
    case r: Reactive[?] => styleValue(r.v)
    case None | null | false => None
    case Some(x) => styleValue(x)
    case x =>
      val s = String.valueOf(x)
      if (s.isEmpty) None else Some(s)
  }

  private def setStyleProp(ele: HTMLElement, name: String, v: Any): Unit = {
    styleValue(v) match {
      case Some(s) => ele.style.setProperty(cssName(name), s)
      case None => ele.style.removeProperty(cssName(name))
    }
  }

  /** style属性全体を文字列で指定します(従来どおり). */
  def style(rv: Reactive[String])
           (using cm: ComponentManager): ATTR = {
    ComponentManager.attrS("style", rv)
  }

  /** プロパティ名と値のMap全体がReactiveの場合. 値がなくなったプロパティは外します. */
  def style(rv: Reactive[Map[String, String]])
           (using d: DummyImplicit,
            cm: ComponentManager): ATTR = {
    ComponentManager.el(styleMapEffect(rv))
  }

  private def styleMapEffect(rv: Reactive[Map[String, String]]): ElementEffectF = ele => AttrController { ac =>
    var current = Map.empty[String, String]
    val set = (m: Map[String, String]) => {
      (current.keySet -- m.keySet).foreach(k => ele.style.removeProperty(cssName(k)))
      m.foreach((k, v) => setStyleProp(ele, k, v))
      current = m
    }
    ac.element = Some(ele)
    set(rv.v)
    ac.watchInfos = List(rv.addWatcher { case (_, n) => set(n) })
    Right(ac)
  }

  /** 変化しないプロパティを設定します(変化するものはstyleMapEffectで扱います). */
  private def stylePropsEffect(ps: List[(String, Any)]): ElementEffectF = ele => AttrController { ac =>
    ac.element = Some(ele)
    ps.foreach((k, x) => setStyleProp(ele, k, x))
    Right(ac)
  }

  /**
   * :styleの値(文字列・組・Map)を、プロパティ名と値のMapにします. 値のないプロパティは含めません.
   * 値のReactiveは `.v` で読むので、trackedの中で呼べば変化に追従します.
   */
  private def styleMapOf(v: Any): Map[String, String] = {
    val ps = stylePairs(v).getOrElse(parseStyle(if (v == null) "" else String.valueOf(v)))
    ps.flatMap((k, x) => styleValue(x).map(k -> _)).toMap
  }

  private def stylePairs(v: Any): Option[List[(String, Any)]] = v match {
    case (k: String, x) => Some(List((k, x)))
    case xs: Iterable[?] if xs.nonEmpty && xs.forall { case (_: String, _) => true; case _ => false } =>
      Some(xs.toList.map { case (k: String, x) => (k, x); case _ => throw new IllegalStateException })
    case _ => None
  }

  /** "color: red; font-size: 12px" をプロパティ名と値の組にします. */
  private def parseStyle(s: String): List[(String, String)] = {
    s.split(";").toList.flatMap { d =>
      d.indexOf(':') match {
        case -1 => None
        case i => Some((d.take(i).trim, d.drop(i + 1).trim)).filter(_._1.nonEmpty)
      }
    }
  }

  def style[A](rv: Reactive[A])
              (using d1: DummyImplicit,
               d2: DummyImplicit,
               cm: ComponentManager): ATTR = {
    ComponentManager.attrS("style", CRV(rv, (a: A) => String.valueOf(a)))
  }

  /**
   * 文字列はstyle属性として埋め込みます(従来どおり).
   * プロパティ名と値の組・Map(オブジェクト記法)は、プロパティごとに設定し、静的なstyle属性と併用できます.
   * 値がReactiveの場合は変化に追従します. 例: `Map("color" -> s.color, "fontSize" -> "12px")`
   */
  def style(v: => Any)
           (using cm: ComponentManager): ATTR = {
    var raw: Any = null
    tracked {
      raw = v
      styleMapOf(raw)
    } match {
      // 何も読んでいなければ、これまでどおり(文字列はstyle属性に、組・Mapはプロパティごとに)
      case Left(_) => stylePairs(raw) match {
        case None => attr("style", raw)
        case Some(ps) => ComponentManager.el(stylePropsEffect(ps))
      }
      case Right(r) => ComponentManager.el(styleMapEffect(r))
    }
  }

  // <Child class="..." :style="..." id="..." data-x="..." />
  // 子コンポーネントのルート要素に付け加える属性(属性の引き継ぎ). classとstyleは子のものと合わせます.
  // Scalaからは c(Child(), rootCls("primary"), rootAttr("id", "x")) のように使えます.

  def rootCls(rv: Reactive[String])
             (using cm: ComponentManager): ElementEffectF = {
    ClassStringsController(CRV(rv, splitClasses))
  }

  def rootCls(rv: Reactive[List[String]])
             (using d: DummyImplicit,
              cm: ComponentManager): ElementEffectF = {
    ClassStringsController(rv)
  }

  def rootCls(rv: Reactive[Map[String, Boolean]])
             (using d1: DummyImplicit,
              d2: DummyImplicit,
              cm: ComponentManager): ElementEffectF = {
    ClassStringsController(CRV(rv, (m: Map[String, Boolean]) => m.toList.collect { case (k, true) => splitClasses(k) }.flatten))
  }

  def rootCls(v: => Any)
             (using cm: ComponentManager): ElementEffectF = {
    ClassStringsController(trackedR(classesOf(v)))
  }

  /** 文字列のstyleも、プロパティごとに子のstyleへ合わせます. */
  def rootStyle(rv: Reactive[String])
               (using cm: ComponentManager): ElementEffectF = {
    styleMapEffect(CRV(rv, (s: String) => parseStyle(s).toMap))
  }

  def rootStyle(rv: Reactive[Map[String, String]])
               (using d: DummyImplicit,
                cm: ComponentManager): ElementEffectF = {
    styleMapEffect(rv)
  }

  def rootStyle(v: => Any)
               (using cm: ComponentManager): ElementEffectF = {
    tracked(styleMapOf(v)) match {
      case Left(m) => stylePropsEffect(m.toList)
      case Right(r) => styleMapEffect(r)
    }
  }

  def rootAttr(name: String,
               rv: Reactive[String])
              (using cm: ComponentManager): ElementEffectF = {
    AttrStringController(name, rv)
  }

  def rootAttr(name: String,
               rv: Reactive[Boolean])
              (using d: DummyImplicit,
               cm: ComponentManager): ElementEffectF = {
    AttrBooleanController(name, rv, "")
  }

  def rootAttr[A](name: String,
                  rv: Reactive[A])
                 (using d1: DummyImplicit,
                  d2: DummyImplicit,
                  cm: ComponentManager): ElementEffectF = {
    AttrStringController(name, CRV(rv, (a: A) => String.valueOf(a)))
  }

  def rootAttr(name: String,
               v: => Any)
              (using cm: ComponentManager): ElementEffectF = {
    anyAttrEffect(name, trackedR(v))
  }

  // v-for="x in expr"
  // ComponentManager.forC / forEachC へ渡すリスト. リアクティブなリストはそのまま、
  // Listの式は、読んだリアクティブ値が変わったときに計算し直すCRVにします.

  def each[R <: Reactive[?]](r: R): R = r

  def each[A](xs: => List[A])
             (using cm: ComponentManager): CRV[List[A]] = {
    Computed.track(() => xs) match {
      case Left(l) => new RV(l).mapC(identity)
      case Right(c) =>
        cm.addTmpReactive(c)
        c
    }
  }

  // @event="stmt"

  def on(name: String,
         handler: Event => Unit)
        (using cm: ComponentManager): ATTR = {
    ComponentManager.ev(EventType.CustomEventType(name), handler)
  }

  /**
   * .once / .capture / .passive 付きのイベント.
   * guard(キー修飾子などの条件)を満たした場合だけhandlerを実行します. onceの場合は、条件を満たした最初の1回だけ実行して解除します.
   */
  def on(name: String,
         handler: Event => Unit,
         capture: Boolean,
         once: Boolean,
         passive: Boolean,
         guard: Event => Boolean)
        (using cm: ComponentManager): ATTR = {
    ComponentManager.el(ele => new EventController(ec => {
      var done = false
      val h: Event => Unit = e => {
        try {
          if (!done && guard(e)) {
            if (once) {
              done = true
              ec.abortController.foreach(_.abort())
            }
            handler(e)
          }
        } catch {
          // ハンドラーの中の例外は、エラーの境界(なければScalactiveConfig.errorHandler)へ
          case scala.util.control.NonFatal(ex) => io.github.ondeoma.scalactive.components.ErrorBoundaryContext.report(ec.boundary, ex)
        }
      }
      ec.element = Some(ele)
      ec.abortController = Some(addEventToElement(ele, EventType.CustomEventType(name), h, capture = capture, passive = passive))
      Right(ec)
    }))
  }

  // @event.enter / .ctrl / .self などの修飾子

  /** キーボードイベントのkeyがkeysのいずれかであるか. */
  def key(event: Event,
          keys: String*): Boolean = {
    event match {
      case k: KeyboardEvent => keys.contains(k.key)
      case _ => false
    }
  }

  /** 修飾キー(ctrl/shift/alt/meta)が押されているか. */
  def modifier(event: Event,
               name: String): Boolean = {
    event.asInstanceOf[js.Dynamic].selectDynamic(name + "Key").asInstanceOf[js.UndefOr[Boolean]].getOrElse(false)
  }

  /** イベントが要素自身で発生したか(子要素からのバブリングでないか). */
  def self(event: Event): Boolean = {
    event.target == event.currentTarget
  }

  // v-model="rv"

  /**
   * v-modelで要素とRVを同期する方法.
   * String/Int/Double/Boolean/List[String]のインスタンスを用意しています.
   */
  @implicitNotFound("v-model does not support RV[${A}] (supported: RV[String], RV[Int], RV[Double], RV[Boolean], RV[List[String]])")
  trait ModelBinding[A] {

    /** 値を要素へ反映します. */
    def write(ele: HTMLElement, a: A, trim: Boolean): Unit

    /** 要素から値を読み取ります. 反映すべき値がない場合はNone. */
    def read(ele: HTMLElement, current: A, trim: Boolean): Option[A]

  }

  private def dyn(ele: HTMLElement): js.Dynamic = ele.asInstanceOf[js.Dynamic]

  private def inputType(ele: HTMLElement): String = {
    if (ele.tagName.equalsIgnoreCase("input")) String.valueOf(dyn(ele).`type`).toLowerCase else ""
  }

  private def isCheckable(ele: HTMLElement): Boolean = {
    val t = inputType(ele)
    t == "checkbox" || t == "radio"
  }

  private def isSelect(ele: HTMLElement): Boolean = ele.tagName.equalsIgnoreCase("select")

  private def elementValue(ele: HTMLElement): String = String.valueOf(dyn(ele).value)

  private def checked(ele: HTMLElement): Boolean = dyn(ele).checked.asInstanceOf[js.UndefOr[Boolean]].getOrElse(false)

  private def stringLike[A](toS: A => String,
                            fromS: String => Option[A]): ModelBinding[A] = new ModelBinding[A] {

    def write(ele: HTMLElement, a: A, trim: Boolean): Unit = {
      if (isCheckable(ele)) {
        dyn(ele).checked = elementValue(ele) == toS(a)
      } else if (!read(ele, a, trim).contains(a)) {
        // 入力途中の値(末尾の空白や"1."など)を書き換えないよう、読み取り結果が異なる場合のみ反映する
        dyn(ele).value = toS(a)
      }
    }

    def read(ele: HTMLElement, current: A, trim: Boolean): Option[A] = {
      if (isCheckable(ele)) {
        if (checked(ele)) fromS(elementValue(ele)) else None
      } else {
        val s = elementValue(ele)
        fromS(if (trim) s.trim else s)
      }
    }

  }

  object ModelBinding {

    given ModelBinding[String] = stringLike(identity, Some(_))

    given ModelBinding[Int] = stringLike(_.toString, _.trim.toIntOption)

    given ModelBinding[Double] = stringLike(_.toString, _.trim.toDoubleOption)

    given ModelBinding[Boolean] = new ModelBinding[Boolean] {

      def write(ele: HTMLElement, a: Boolean, trim: Boolean): Unit = dyn(ele).checked = a

      def read(ele: HTMLElement, current: Boolean, trim: Boolean): Option[Boolean] = Some(checked(ele))

    }

    /** チェックボックスのグループ、または複数選択のselect. */
    given ModelBinding[List[String]] = new ModelBinding[List[String]] {

      private def options(ele: HTMLElement): List[js.Dynamic] = {
        val os = dyn(ele).options
        (0 until os.length.asInstanceOf[Int]).map(i => os.item(i)).toList
      }

      def write(ele: HTMLElement, a: List[String], trim: Boolean): Unit = {
        if (isSelect(ele)) {
          options(ele).foreach(o => o.selected = a.contains(String.valueOf(o.value)))
        } else {
          dyn(ele).checked = a.contains(elementValue(ele))
        }
      }

      def read(ele: HTMLElement, current: List[String], trim: Boolean): Option[List[String]] = {
        if (isSelect(ele)) {
          Some(options(ele).filter(_.selected.asInstanceOf[Boolean]).map(o => String.valueOf(o.value)))
        } else {
          val v = elementValue(ele)
          if (checked(ele)) Some(if (current.contains(v)) current else current :+ v)
          else Some(current.filterNot(_ == v))
        }
      }

    }

  }

  /**
   * input/textarea/selectの値とRVを双方向に同期します.
   * チェックボックス・ラジオボタン・select・lazy指定の場合はchange、それ以外はinputイベントで反映します.
   * IME変換中の入力は確定(compositionend)まで反映しません.
   */
  def model[A](rv: RV[A],
               lazyUpdate: Boolean = false,
               trim: Boolean = false)
              (using b: ModelBinding[A],
               cm: ComponentManager): ATTR = {
    val sync: HTMLElement => AttrController = ele => AttrController { ac =>
      ac.element = Some(ele)
      b.write(ele, rv.v, trim)
      ac.watchInfos = List(rv.addWatcher { case (_, n) => b.write(ele, n, trim) })
      Right(ac)
    }
    def update(ele: HTMLElement)(e: Event): Unit = {
      val composing = e.asInstanceOf[js.Dynamic].isComposing.asInstanceOf[js.UndefOr[Boolean]].getOrElse(false)
      val compositionEnd = e.`type` == "compositionend"
      if (lazyUpdate && compositionEnd) ()
      else if (!composing || compositionEnd) {
        b.read(ele, rv.v, trim).foreach(n => if (n != rv.v) rv := n)
      }
    }
    val listen: HTMLElement => EventController = ele => {
      val usesChange = lazyUpdate || isCheckable(ele) || isSelect(ele)
      val et = if (usesChange) EventType.change else EventType.CustomEventType("input")
      EventController(ele, et, update(ele))
    }
    val listenComposition: HTMLElement => EventController = ele => {
      EventController(ele, EventType.CustomEventType("compositionend"), update(ele))
    }
    ComponentManager.el(sync, listen, listenComposition)
  }

  // <slot /> と、子コンポーネントへ渡す子要素

  def slotOf(f: ComponentManager => HTML): Slot = Slot(f)

  def slot(s: Slot)
          (fallback: ComponentManager => HTML)
          (using cm: ComponentManager): HTML = {
    s(fallback)
  }

  // <slot name="x" :value="v" /> と <template #x="v">(スコープ付きスロット)

  def scopedSlotOf[A](f: A => ComponentManager => HTML): ScopedSlot[A] = ScopedSlot(f)

  def scopedSlot[A](s: ScopedSlot[A],
                    value: A)
                   (fallback: ComponentManager => HTML)
                   (using cm: ComponentManager): HTML = {
    s(value)(fallback)
  }

  // <ChildComponent />

  def component(f: GenComponentF)
               (using cm: ComponentManager): HTML = {
    ComponentManager.c(f)
  }

  /** 子コンポーネントを展開し、ルート要素にattrs(rootCls/rootStyle/rootAttr)を付け加えます. */
  def component(f: GenComponentF,
                attrs: ElementEffectF*)
               (using cm: ComponentManager): HTML = {
    ComponentManager.c(f, attrs *)
  }

}
