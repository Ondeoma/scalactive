package io.github.ondeoma.scalactive.components

import io.github.ondeoma.scalactive.ScalactiveConfig
import io.github.ondeoma.scalactive.components.ComponentManager.{AddMethod, HTML}
import io.github.ondeoma.scalactive.controllers.NodesComponentController
import io.github.ondeoma.scalactive.reactive.*
import org.scalajs.dom.*

import scala.concurrent.Future
import scala.concurrent.duration.*
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

/**
 * onMounted / onUnmounted のテスト.
 */
class LifecycleSuite extends munit.FunSuite {

  private var savedDelay: FiniteDuration = 0.millis
  private var savedKeepScroll = true

  override def beforeAll(): Unit = {
    savedDelay = ScalactiveConfig.reloadTimerLazyDuration
    savedKeepScroll = ScalactiveConfig.keepWindowScrollPositionWhenReload
    ScalactiveConfig.reloadTimerLazyDuration = 0.millis
    // jsdomはwindow.scrollToを実装していない
    ScalactiveConfig.keepWindowScrollPositionWhenReload = false
  }

  override def afterAll(): Unit = {
    ScalactiveConfig.reloadTimerLazyDuration = savedDelay
    ScalactiveConfig.keepWindowScrollPositionWhenReload = savedKeepScroll
  }

  private def mount(gen: ComponentManager => HTML): (HTMLElement, NodesComponentController) = {
    val root = document.createElement("div").asInstanceOf[HTMLElement]
    document.body.appendChild(root)
    val component = new StatelessComponent {
      def genHtml(implicit cm: ComponentManager): HTML = gen(cm)
    }
    given HTMLElement = root
    given AddMethod = AddMethod.append(root)
    component().init() match {
      case Left(e) => fail(e)
      case Right(c) => (root, c)
    }
  }

  private def flush(): Future[Unit] = {
    js.Promise.resolve[Unit](()).toFuture.flatMap(_ => js.Promise.resolve[Unit](()).toFuture)
  }

  test("onMountedはDOMに挿入された後に、子から順に実行される") {
    var log = List.empty[String]
    val (root, _) = mount { implicit cm =>
      ComponentManager.onMounted {
        log :+= s"parent:${document.getElementById("lc-child") != null}"
      }
      val child = ComponentManager.%(new StatelessComponent {
        def genHtml(implicit cm: ComponentManager): HTML = {
          onMounted(log :+= s"child:${document.getElementById("lc-child") != null}")
          """<span id="lc-child">c</span>"""
        }
      }.apply()(_, _))
      s"<div>$child</div>"
    }
    assertEquals(log, Nil, "同期的には実行しない")
    flush().map { _ =>
      assertEquals(log, List("child:true", "parent:true"))
      root.remove()
    }
  }

  test("ifCの切り替えでonUnmounted/onMountedが実行される") {
    var log = List.empty[String]
    val show = RV(true)
    mount { implicit cm =>
      s"<div>${ComponentManager.ifC(show) { implicit cm =>
        ComponentManager.onMounted(log :+= "mounted")
        // DOMから取り除かれた後に実行される
        ComponentManager.onUnmounted(log :+= s"unmounted:${document.getElementById("lc-if") == null}")
        """<p id="lc-if">x</p>"""
      }}</div>"
    }
    flush().flatMap { _ =>
      show := false
      flush()
    }.flatMap { _ =>
      show := true
      flush()
    }.map { _ =>
      assertEquals(log, List("mounted", "unmounted:true", "mounted"))
    }
  }

  test("forCで消えた行だけonUnmountedが実行される") {
    var log = List.empty[String]
    val list = RV(List(1, 2, 3))
    mount { implicit cm =>
      s"<ul>${ComponentManager.forEachC(list) { (rowCm, v) =>
        given ComponentManager = rowCm
        ComponentManager.onMounted(log :+= s"m$v")
        ComponentManager.onUnmounted(log :+= s"u$v")
        s"<li>$v</li>"
      }}</ul>"
    }
    flush().flatMap { _ =>
      assertEquals(log, List("m1", "m2", "m3"))
      log = Nil
      list := List(1, 3, 4)
      flush()
    }.map(_ => assertEquals(log, List("u2", "m4")))
  }

  test("挿入の直後に片付けられた場合はonMountedを実行しない. 親の片付けで子のonUnmountedも実行される") {
    var log = List.empty[String]
    val (_, controller) = mount { implicit cm =>
      ComponentManager.onMounted(log :+= "mounted")
      ComponentManager.onUnmounted(log :+= "unmounted")
      val child = ComponentManager.%(new StatelessComponent {
        def genHtml(implicit cm: ComponentManager): HTML = {
          onUnmounted(log :+= "child-unmounted")
          "<span>c</span>"
        }
      }.apply()(_, _))
      s"<div>$child</div>"
    }
    controller.clear()
    flush().map(_ => assertEquals(log, List("child-unmounted", "unmounted")))
  }

}
