package io.github.ondeoma.scalactive.bench

import io.github.ondeoma.scalactive.ScalactiveConfig
import io.github.ondeoma.scalactive.components.ComponentManager.*
import org.scalajs.dom.*

import scala.concurrent.duration.*
import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

/**
 * ベンチマーク用のページ. window.scalactiveBench からbench/run.mjsが操作します.
 *
 * URLパラメータ:
 *  - impl=scala|st  表をScala API(RowsScala)と.st(RowsSt)のどちらで描くか(既定: scala)
 *  - delay=ミリ秒    ScalactiveConfig.reloadTimerLazyDuration(指定しなければライブラリの既定値のまま)
 */
object BenchApp {

  /** 画面への反映を待つ上限 */
  private val timeoutMillis = 30000.0

  private def app: HTMLElement = document.getElementById("app").asInstanceOf[HTMLElement]

  @main
  def main(): Unit = {
    val params = new URLSearchParams(window.location.search)
    val impl = Option(params.get("impl")).getOrElse("scala")
    Option(params.get("delay")).filter(_.nonEmpty).foreach { d =>
      ScalactiveConfig.reloadTimerLazyDuration = d.toDouble.millis
    }
    // スクロール位置の保存・復元は計測の邪魔になるので切る
    ScalactiveConfig.keepWindowScrollPositionWhenReload = false

    val component = impl match {
      case "st" => RowsSt()(app, AddMethod.append(app))
      case _ => RowsScala()(app, AddMethod.append(app))
    }
    component.init().left.foreach(e => throw new IllegalStateException(e))

    window.asInstanceOf[js.Dynamic].scalactiveBench = js.Dynamic.literal(
      impl = impl,
      delay = ScalactiveConfig.reloadTimerLazyDuration.toMillis.toDouble,
      scenarios = () => Scenarios.all.map { s =>
        js.Dynamic.literal(name = s.name, description = s.description, kind = s.kind, iterations = s.iterations)
      }.toJSArray,
      metrics = () => metrics(),
      run = (name: String, warmup: Int, iterations: Int) => run(name, warmup, iterations).toJSPromise,
    )
  }

  /** doneがtrueになるまで、#app以下のDOMの変化のたびに確認します. */
  private def waitUntil(done: () => Boolean): Future[Unit] = {
    if (done()) Future.successful(())
    else {
      val p = Promise[Unit]()
      var timer = 0
      lazy val observer: MutationObserver = new MutationObserver((_, _) => {
        if (!p.isCompleted && done()) {
          observer.disconnect()
          window.clearTimeout(timer)
          p.success(())
        }
      })
      observer.observe(app, new MutationObserverInit {
        childList = true
        subtree = true
        characterData = true
        attributes = true
      })
      timer = window.setTimeout(() => {
        observer.disconnect()
        p.tryFailure(new IllegalStateException("timed out waiting for the DOM"))
      }, timeoutMillis)
      p.future
    }
  }

  /** 計測. 描画(レイアウト)まで含めるため、最後にレイアウトを強制します. */
  private def measure(kind: String, step: Step): Future[Double] = {
    val t0 = window.performance.now()
    step.action()
    if (kind == "sync") Future.successful(window.performance.now() - t0)
    else waitUntil(step.done).map { _ =>
      app.offsetHeight
      window.performance.now() - t0
    }
  }

  private def runSteps(steps: List[Step]): Future[Unit] = {
    steps.foldLeft(Future.successful(())) { (f, s) =>
      f.flatMap { _ =>
        s.action()
        waitUntil(s.done)
      }
    }
  }

  /** 1フレーム待つ(前の計測の後片付けを計測に含めないため) */
  private def nextFrame(): Future[Unit] = {
    val p = Promise[Unit]()
    window.requestAnimationFrame(_ => window.setTimeout(() => p.success(()), 0))
    p.future
  }

  private def countNodes(n: Node): Int = {
    var count = 1
    var c = n.firstChild
    while (c != null) {
      count += countNodes(c)
      c = c.nextSibling
    }
    count
  }

  /** 解除漏れなどを見つけるための数 */
  private def metrics(): js.Dynamic = js.Dynamic.literal(
    domNodes = countNodes(app) - 1,
    selectedWatchers = Store.selected.watcherCount,
    rowsWatchers = Store.rows.watcherCount,
  )

  private def run(name: String, warmup: Int, iterations: Int): Future[js.Dynamic] = {
    val scenario = Scenarios.find(name).getOrElse(throw new IllegalArgumentException(s"unknown scenario: $name"))
    val total = warmup + iterations
    (1 to total).foldLeft(Future.successful(List.empty[Double])) { (f, _) =>
      for {
        times <- f
        _ <- runSteps(scenario.setup())
        _ <- nextFrame()
        t <- measure(scenario.kind, scenario.measured())
        _ <- nextFrame()
      } yield t :: times
    }.flatMap { timesRev =>
      val after = metrics()
      // 表を空にした後にも残っているDOMや監視は解除漏れ
      runSteps(List(Step(() => Store.reset(), () => Option(document.getElementById("tbody")).forall(_.children.length == 0))))
        .flatMap(_ => nextFrame())
        .map { _ =>
          js.Dynamic.literal(
            name = name,
            times = timesRev.reverse.drop(warmup).toJSArray,
            after = after,
            afterReset = metrics(),
          )
        }
    }
  }

}
