package io.github.ondeoma.scalactive.ls

import org.eclipse.lsp4j.{DiagnosticSeverity, Position}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.collection.JavaConverters._
import scala.io.Source

/**
 * 実際のScala 3 presentation compilerを使った言語機能のテスト.
 * coreのクラスパスで、一時ディレクトリに置いた.stを型検査します.
 * (初回はpresentation compilerをMaven Centralから取得します)
 */
class StLanguageServiceSuite extends munit.FunSuite {

  override val munitTimeout = scala.concurrent.duration.Duration(5, "min")

  private val coreClasspath: Seq[String] =
    Source.fromResource("core-classpath.txt").getLines().filter(_.nonEmpty).toList

  private val dir: Path = Files.createTempDirectory("scalactive-ls")
  private val srcRoot = dir.resolve("src/main/scalactive")
  private val otherScala = dir.resolve("src/main/scala")
  /** 定義ジャンプの確認用に、coreのソースも探索対象にする */
  private val coreSources: Seq[String] = Source.fromResource("core-sources.txt").getLines().filter(_.nonEmpty).toList

  private def write(p: Path, s: String): Path = {
    Files.createDirectories(p.getParent)
    Files.write(p, s.getBytes(StandardCharsets.UTF_8))
  }

  private def json(xs: Seq[Any]): String = xs.map(x => "\"" + x.toString.replace("\\", "\\\\") + "\"").mkString("[", ",", "]")

  write(dir.resolve("target/scalactive-lsp.json"),
    s"""{
       |  "version": 1,
       |  "scalaVersion": "3.3.7",
       |  "classpath": ${json(coreClasspath)},
       |  "scalacOptions": ["-scalajs"],
       |  "sourceRoots": ${json(Seq(srcRoot, otherScala))},
       |  "sourceDirectories": ${json(Seq(otherScala, srcRoot) ++ coreSources)},
       |  "generatedDirectory": "${dir.resolve("target/gen").toString.replace("\\", "\\\\")}"
       |}""".stripMargin)

  private val counter =
    """<script lang="scala">
      |  case class State(count: RV[Int] = RV(0), name: RV[String] = RV(""))
      |  def initState() = State()
      |  def twice(n: Int): Int = n * 2
      |</script>
      |
      |<template>
      |  <div :title="s.name">
      |    <p v-if="s.count.mapC(_ > 3)">{{ s.count }}</p>
      |    <button @click="s.count := twice(s.count.v)">+</button>
      |    <span v-show="s.name.mapC(_.isEmpty)">{{ s.name }}</span>
      |  </div>
      |</template>
      |""".stripMargin

  private val counterFile = write(srcRoot.resolve("app/Counter.st"), counter)
  private val counterUri = counterFile.toUri.toString

  private val logs = new StringBuilder
  private val service = new StLanguageService(m => logs.synchronized(logs ++= m + "\n"))

  override def afterAll(): Unit = service.shutdown()

  /** text(n番目の出現)の先頭からdelta文字目のLSP位置 */
  private def pos(src: String, text: String, delta: Int = 0, n: Int = 0): Position = {
    val offset = Iterator.iterate(src.indexOf(text))(i => src.indexOf(text, i + 1)).drop(n).next() + delta
    assert(offset >= delta, s"'$text' not found")
    new LineIndex(src).position(offset)
  }

  private def withText[A](uri: String, text: String)(f: => A): A = {
    service.open(uri, text)
    try f
    finally service.change(uri, Files.readAllLines(java.nio.file.Paths.get(new java.net.URI(uri))).asScala.mkString("", "\n", "\n"))
  }

  test("正しい.stにはエラーがない") {
    val errors = service.diagnostics(counterUri).filter(_.getSeverity == DiagnosticSeverity.Error)
    assertEquals(errors.map(_.getMessage), Nil, logs.toString)
  }

  test("テンプレート中の型エラーを.st上の位置で報告する") {
    val broken = counter.replace("{{ s.count }}", "{{ s.count.nothing }}")
    withText(counterUri, broken) {
      val errors = service.diagnostics(counterUri).filter(_.getSeverity == DiagnosticSeverity.Error)
      assertEquals(errors.size, 1, errors)
      val r = errors.head.getRange
      assertEquals(r.getStart, pos(broken, "s.count.nothing"))
      assert(errors.head.getMessage.contains("nothing"), errors.head.getMessage)
    }
  }

  test("<script>中の型エラー") {
    val broken = counter.replace("n * 2", "n * \"x\"")
    withText(counterUri, broken) {
      val errors = service.diagnostics(counterUri).filter(_.getSeverity == DiagnosticSeverity.Error)
      assertEquals(errors.size, 1, errors)
      assertEquals(errors.head.getRange.getStart.getLine, pos(broken, "n * \"x\"").getLine)
    }
  }

  test(".stの構文エラー") {
    val broken = counter.replace("<p v-if", "<p v-else")
    withText(counterUri, broken) {
      val ds = service.diagnostics(counterUri)
      assertEquals(ds.size, 1)
      assertEquals(ds.head.getRange.getStart, pos(broken, "v-else"))
      assert(ds.head.getMessage.contains("v-else must follow"))
    }
  }

  test("テンプレート中の補完") {
    val text = counter.replace("{{ s.count }}", "{{ s.cou }}")
    withText(counterUri, text) {
      val items = service.completion(counterUri, pos(text, "s.cou }}", "s.cou".length)).getItems.asScala
      val count = items.find(_.getLabel.startsWith("count")).getOrElse(fail(s"count not in ${items.map(_.getLabel)}"))
      val edit = count.getTextEdit.getLeft
      // 置き換え範囲は.st上の "cou"
      assertEquals(edit.getRange.getStart, pos(text, "cou }}"))
      assertEquals(edit.getRange.getEnd, pos(text, "cou }}", 3))
    }
  }

  test("<script>中の補完") {
    val text = counter.replace("n * 2", "n.toStr")
    withText(counterUri, text) {
      val items = service.completion(counterUri, pos(text, "n.toStr", "n.toStr".length)).getItems.asScala
      assert(items.exists(_.getLabel.startsWith("toString")), items.map(_.getLabel))
    }
  }

  test("HTMLの部分では補完しない") {
    assertEquals(service.completion(counterUri, pos(counter, "<button", 3)).getItems.size, 0)
  }

  test("ホバーで型を表示し、範囲は.st上") {
    val h = service.hover(counterUri, pos(counter, "count }}", 1)).getOrElse(fail("no hover"))
    val text = h.getContents.getRight.getValue
    assert(text.contains("RV[Int]"), text)
    Option(h.getRange).foreach(r => assertEquals(r.getStart, pos(counter, "count }}")))
  }

  test("同じ.st内の定義へジャンプ") {
    val locs = service.definition(counterUri, pos(counter, "twice(s.count", 1))
    assertEquals(locs.map(_.getUri), List(counterUri))
    assertEquals(locs.head.getRange.getStart, pos(counter, "twice(n"))
  }

  test("ライブラリのソースディレクトリにある定義へジャンプ") {
    val locs = service.definition(counterUri, pos(counter, "mapC(_.isEmpty)", 1))
    assertEquals(locs.map(l => java.nio.file.Paths.get(new java.net.URI(l.getUri)).getFileName.toString), List("Reactive.scala"))
    val file = new String(Files.readAllBytes(java.nio.file.Paths.get(new java.net.URI(locs.head.getUri))), StandardCharsets.UTF_8)
    val line = file.split("\n")(locs.head.getRange.getStart.getLine)
    assert(line.contains("def mapC"), line)
  }

  test("SourceSymbolSearchは同じパッケージのファイルからも定義を探す") {
    write(otherScala.resolve("app/util/Format.scala"),
      """package app.util
        |
        |object Format {
        |  def label(n: Int): String = s"#$n"
        |}
        |""".stripMargin)
    val loc = SourceSymbolSearch.find(Seq(otherScala), "app/util/Format.label().").getOrElse(fail("not found"))
    assertEquals(loc.getRange.getStart, new Position(3, 6))
    write(otherScala.resolve("app/util/Other.scala"), "package app.util\n\ncase class Point(x: Int, y: Int)\n")
    val y = SourceSymbolSearch.find(Seq(otherScala), "app/util/Point#y.").getOrElse(fail("not found"))
    assertEquals(y.getRange.getStart, new Position(2, 25))
  }

  test("関数の引数のヒント") {
    val sh = service.signatureHelp(counterUri, pos(counter, "twice(s.count", 6)).getOrElse(fail("no signature help"))
    assert(sh.getSignatures.asScala.exists(_.getLabel.contains("n: Int")), sh)
  }

  test("設定ファイルがないプロジェクトでは案内を出す") {
    val other = Files.createTempDirectory("scalactive-ls-none").resolve("Foo.st")
    write(other, "<template><p>a</p></template>")
    val ds = service.diagnostics(other.toUri.toString)
    assertEquals(ds.map(_.getSeverity), List(DiagnosticSeverity.Information))
    assert(ds.head.getMessage.contains("scalactive-lsp.json"))
  }

  test("SemanticDBのシンボルを分解する") {
    assertEquals(SourceSymbolSearch.parse("a/b/Foo."), Some((List("a", "b"), List("Foo"))))
    assertEquals(SourceSymbolSearch.parse("a/b/Foo#bar(+1)."), Some((List("a", "b"), List("Foo", "bar"))))
    assertEquals(SourceSymbolSearch.parse("a/Foo.Props#title."), Some((List("a"), List("Foo", "Props", "title"))))
    assertEquals(SourceSymbolSearch.parse("local3"), None)
  }

}
