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

  test("HTMLのテキストの部分では補完しない") {
    assertEquals(service.completion(counterUri, pos(counter, ">+<", 1)).getItems.size, 0)
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

  // ---- テンプレートの補完 ----

  write(srcRoot.resolve("app/Card.st"),
    """<script lang="scala">
      |case class Props(title: String,
      |                 count: RV[Int] = RV(0),
      |                 onClose: Unit => Unit = _ => (),
      |                 slot: Slot = Slot.empty,
      |                 footer: Slot = Slot.empty,
      |                 row: ScopedSlot[Int] = ScopedSlot.empty)
      |</script>
      |<template><div><slot name="foo" /></div></template>
      |""".stripMargin)

  private def labels(uri: String, text: String, marker: String = "|"): List[String] = {
    val offset = text.indexOf(marker)
    val src = text.replace(marker, "")
    withText(uri, src) {
      service.completion(uri, new LineIndex(src).position(offset)).getItems.asScala.map(_.getLabel).toList
    }
  }

  test("テンプレートの補完: タグ名に同じパッケージのコンポーネントと組み込みのコンポーネント") {
    val ls = labels(counterUri, counter.replace("<button @click", "<Ca|<button @click"))
    assert(ls.contains("Card"), ls)
    assert(ls.contains("Teleport") && ls.contains("RouterLink"), ls)
    assert(ls.contains("div"), ls)
  }

  test("テンプレートの補完: コンパイルできる.stでも、既存の子コンポーネントのタグ名ではテンプレートの補完") {
    val text = counter.replace("<button @click", "<Card title=\"t\" /><button @click")
    val ls = labels(counterUri, text.replace("<Card title", "<Ca|rd title"))
    assert(ls.contains("Card") && ls.contains("Teleport"), ls)
    assert(!ls.exists(_.startsWith("StringBuilder")), ls)
  }

  test("テンプレートの補完: 子コンポーネントの属性にprops・イベント・v-model") {
    val ls = labels(counterUri, counter.replace("<button @click", "<Card | /><button @click"))
    assert(ls.contains("title") && ls.contains(":title"), ls)
    assert(ls.contains("@close"), ls)
    assert(ls.contains("v-model:count"), ls)
    assert(!ls.contains("footer"), "スロットは属性に出さない")
    assert(ls.contains("v-if") && ls.contains("class"), ls)
  }

  test("テンプレートの補完: 子コンポーネントの中の<template>にスロット名") {
    val ls = labels(counterUri, counter.replace("<button @click", "<Card title=\"t\"><template |></template></Card><button @click"))
    assertEquals(ls.filter(_.startsWith("#")).sorted, List("#default", "#footer", "#row"))
  }

  test("テンプレートの補完: 要素の属性にディレクティブ. Scalaの部分は従来どおり") {
    val ls = labels(counterUri, counter.replace("<span v-show", "<span | v-show"))
    assert(ls.contains("v-if") && ls.contains("v-for") && ls.contains(":class") && ls.contains("ref"), ls)
    val scala = labels(counterUri, counter.replace("{{ s.count }}", "{{ s.cou| }}"))
    assert(scala.exists(_.startsWith("count")), scala)
  }

  test("テンプレートの補完: <slot name=\"\">に自分のPropsのスロット名") {
    val cardUri = srcRoot.resolve("app/Card.st").toUri.toString
    val text = new String(Files.readAllBytes(srcRoot.resolve("app/Card.st")), StandardCharsets.UTF_8)
    val ls = labels(cardUri, text.replace("<slot name=\"foo\" />", "<slot name=\"|\" />"))
    assertEquals(ls.sorted, List("footer", "row"))
  }

  test("定義ジャンプ: 子コンポーネントのタグ名はその.stのProps、属性名はPropsのパラメータへ") {
    val cardPath = srcRoot.resolve("app/Card.st")
    val cardText = new String(Files.readAllBytes(cardPath), StandardCharsets.UTF_8)
    val text = counter.replace("<button @click", "<Card title=\"t\" :count=\"s.count\" @close=\"()\"></Card><button @click")
    withText(counterUri, text) {
      def defAt(marker: String, delta: Int) = service.definition(counterUri, pos(text, marker, delta))
      val tag = defAt("<Card title", 2)
      assertEquals(tag.map(_.getUri), List(cardPath.toUri.toString))
      assertEquals(tag.head.getRange.getStart, pos(cardText, "case class Props"))
      val close = defAt("</Card>", 3)
      assertEquals(close.map(_.getUri), List(cardPath.toUri.toString))
      assertEquals(defAt("Card title=", 6).head.getRange.getStart, pos(cardText, "title: String"))
      assertEquals(defAt(":count=", 2).head.getRange.getStart, pos(cardText, "count: RV"))
      assertEquals(defAt("@close=", 2).head.getRange.getStart, pos(cardText, "onClose:"))
    }
  }

  test("整形: 変わる場合は全体を置き換える編集を返し、整形済みなら返さない") {
    val messy = counter.replace("    <p v-if", "<p v-if")
    withText(counterUri, messy) {
      val edits = service.formatting(counterUri)
      assertEquals(edits.size, 1)
      assertEquals(edits.head.getNewText, counter)
    }
    assertEquals(service.formatting(counterUri), Nil)
  }

  test("TemplateCompletion.context: コメント・属性値・入れ子を正しく扱う") {
    import TemplateCompletion._
    def ctx(s: String) = context(s.replace("|", ""), 0, s.indexOf("|"))
    assertEquals(ctx("<div><!-- <a | --></div>"), Other)
    assertEquals(ctx("<div title=\"<a |\">"), AttrValue("div", "title", 12))
    assertEquals(ctx("<Card><template |"), AttrName("template", 16, List("Card")))
    assertEquals(ctx("<p>a</p><Ca|"), TagName(9, Nil))
    assertEquals(ctx("<br><input |"), AttrName("input", 11, Nil))
  }

  test("SemanticDBのシンボルを分解する") {
    assertEquals(SourceSymbolSearch.parse("a/b/Foo."), Some((List("a", "b"), List("Foo"))))
    assertEquals(SourceSymbolSearch.parse("a/b/Foo#bar(+1)."), Some((List("a", "b"), List("Foo", "bar"))))
    assertEquals(SourceSymbolSearch.parse("a/Foo.Props#title."), Some((List("a"), List("Foo", "Props", "title"))))
    assertEquals(SourceSymbolSearch.parse("local3"), None)
  }

}
