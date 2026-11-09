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

  test("テンプレートの補完: HTMLの標準のタグ・要素ごとの属性・全体の属性・イベント") {
    val tags = labels(counterUri, counter.replace("<button @click", "<tex|<button @click"))
    assert(tags.contains("textarea") && tags.contains("details") && tags.contains("video"), tags)
    val input = labels(counterUri, counter.replace("<button @click", "<input | /><button @click"))
    assert(input.contains("type") && input.contains("placeholder") && input.contains(":value"), input)
    assert(input.contains("id") && input.contains("aria-label"), "全体の属性")
    assert(input.contains("@keydown") && input.contains("@blur"), "イベント")
    assert(!input.contains("href"), "ほかの要素の属性は出さない")
    val a = labels(counterUri, counter.replace("<span v-show", "<a | v-show"))
    assert(a.contains("href") && a.contains("target"), a)
  }

  test("テンプレートの補完: 列挙された属性値(inputのtypeなど)") {
    val types = labels(counterUri, counter.replace("<button @click", "<input type=\"|\" /><button @click"))
    assert(types.contains("checkbox") && types.contains("email"), types)
    val target = labels(counterUri, counter.replace("<button @click", "<a target=\"|\"></a><button @click"))
    assertEquals(target.sorted, List("_blank", "_parent", "_self", "_top"))
    val dir = labels(counterUri, counter.replace("<button @click", "<p dir=\"|\"></p><button @click"))
    assertEquals(dir.sorted, List("auto", "ltr", "rtl"))
  }

  test("テンプレートの補完: htmlCompletion = false(IntelliJ)ではHTMLの標準のタグ・属性・属性値を出さない") {
    service.htmlCompletion = false
    try {
      val tags = labels(counterUri, counter.replace("<button @click", "<|<button @click"))
      assert(!tags.contains("textarea") && tags.contains("Card"), tags)
      val input = labels(counterUri, counter.replace("<button @click", "<input | /><button @click"))
      assert(!input.contains("type") && !input.contains("id"), input)
      assert(input.contains(":value") && input.contains("v-model") && input.contains("@keydown"), input)
      val types = labels(counterUri, counter.replace("<button @click", "<input type=\"|\" /><button @click"))
      assert(types.isEmpty, types)
    } finally service.htmlCompletion = true
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

  test("ホバー: 子コンポーネントのタグ名でPropsの一覧、属性名でpropの型、HTMLのタグ・属性・イベントの説明") {
    val text = counter.replace("<button @click", "<Card title=\"t\" :count=\"s.count\" @close=\"()\"></Card><input type=\"text\" @keydown=\"()\" /><button @click")
    withText(counterUri, text) {
      def hoverAt(marker: String, delta: Int): String = {
        val h = service.hover(counterUri, pos(text, marker, delta))
        assert(h.isDefined, marker)
        h.get.getContents.getRight.getValue
      }
      val card = hoverAt("<Card title", 2)
      assert(card.contains("Card.st") && card.contains("title: String") && card.contains("count: RV[Int] = …"), card)
      val title = hoverAt("title=\"t\" :count", 1)
      assert(title.contains("title: String") && title.contains("必須"), title)
      val close = hoverAt("@close", 2)
      assert(close.contains("onClose: Unit => Unit"), close)
      assert(hoverAt("<input type", 2).contains("入力欄"))
      val tpe = hoverAt("type=\"text\"", 1)
      assert(tpe.contains("入力の種類") && tpe.contains("`checkbox`"), tpe)
      assert(hoverAt("@keydown", 2).contains("キーを押した"))
      // Scalaの部分は従来どおり型を表示する
      assert(hoverAt("{{ s.count }}", 6).contains("RV[Int]"))
    }
  }

  test("propsの検査: 未知のprop・スロット、必須のpropの不足を警告する") {
    val text = counter.replace("<button @click", "<Card titel=\"t\" :count=\"s.count\" class=\"x\" v-if=\"true\"><template #heder>h</template></Card><button @click")
    withText(counterUri, text) {
      val ds = service.diagnostics(counterUri).filter(_.getSeverity == org.eclipse.lsp4j.DiagnosticSeverity.Warning)
      val msgs = ds.map(_.getMessage)
      assert(msgs.exists(_.contains("has no prop 'titel'")), msgs)
      assert(msgs.exists(_.contains("has no slot 'heder'")), msgs)
      assert(msgs.exists(_.contains("missing required prop: title")), msgs)
      assert(!msgs.exists(m => m.contains("'class'") || m.contains("'count'") || m.contains("v-if")), msgs)
      val titel = ds.find(_.getMessage.contains("titel")).get
      assertEquals(titel.getRange.getStart, pos(text, "titel"))
    }
    val ok = counter.replace("<button @click", "<Card title=\"t\" @close=\"()\" v-model:count=\"s.count\"><template #footer>f</template>body</Card><button @click")
    withText(counterUri, ok) {
      val ws = service.diagnostics(counterUri).filter(_.getSeverity == org.eclipse.lsp4j.DiagnosticSeverity.Warning)
      assertEquals(ws.map(_.getMessage), Nil)
    }
  }

  test("propsの検査: ドキュメントサイトの.stには警告がない") {
    val root = Seq("doc/src/main/scalactive", "../doc/src/main/scalactive").map(java.nio.file.Paths.get(_)).find(Files.exists(_)).get
    val files = Files.walk(root).iterator().asScala.filter(_.toString.endsWith(".st")).toList
    assert(files.nonEmpty)
    files.foreach { f =>
      val problems = PropsCheck.check(new String(Files.readAllBytes(f), StandardCharsets.UTF_8), f.toAbsolutePath)
      assertEquals(problems, Nil, f.toString)
    }
  }

  private val childFile = write(srcRoot.resolve("ren/Child.st"),
    """<script lang="scala">
      |case class Props(title: String,
      |                 onClose: Unit => Unit = _ => (),
      |                 footerArea: Slot = Slot.empty)
      |</script>
      |<template>
      |  <div :title="props.title">
      |    <b>{{ props.title }}</b>
      |    <slot name="footer-area" />
      |  </div>
      |</template>
      |""".stripMargin)
  private val parentText =
    """<script lang="scala">
      |def x = 1
      |</script>
      |<template>
      |  <div>
      |    <Child title="a" @close="()"><template #footer-area>f</template></Child>
      |    <Child :title="x.toString" />
      |  </div>
      |</template>
      |""".stripMargin
  private val parentFile = write(srcRoot.resolve("ren/Parent.st"), parentText)

  test("参照の検索: propの定義・属性・スロットと、定義した.stの中のScalaの参照") {
    val parentUri = parentFile.toUri.toString
    val locs = service.references(parentUri, pos(parentText, "title=\"a\"", 1), includeDeclaration = true)
    val childText = new String(Files.readAllBytes(childFile), StandardCharsets.UTF_8)
    val inChild = locs.filter(_.getUri.endsWith("Child.st")).map(_.getRange.getStart)
    val inParent = locs.filter(_.getUri.endsWith("Parent.st")).map(_.getRange.getStart)
    assertEquals(inParent.toSet, Set(pos(parentText, "title=\"a\""), pos(parentText, ":title")))
    assert(inChild.contains(pos(childText, "title: String")), inChild)
    assert(inChild.contains(pos(childText, "title\">", 0)) && inChild.contains(pos(childText, "title }}")), "Scalaの参照: " + inChild)
    val noDecl = service.references(parentUri, pos(parentText, ":title", 2), includeDeclaration = false)
    assertEquals(noDecl.size, locs.size - 1)
    // コンポーネントのタグ
    val tags = service.references(parentUri, pos(parentText, "<Child", 2), includeDeclaration = true)
    assertEquals(tags.size, 3, "開始タグ2つと終了タグ1つ")
  }

  test("名前の変更: propの名前を変えると、属性・スロット・イベントの書き方に合わせて書き換える") {
    val parentUri = parentFile.toUri.toString
    val childUri = childFile.toUri.toString
    val childText = new String(Files.readAllBytes(childFile), StandardCharsets.UTF_8)
    def applyEdits(text: String, es: List[org.eclipse.lsp4j.TextEdit]): String = {
      val index = new LineIndex(text)
      es.sortBy(e => -index.offset(e.getRange.getStart)).foldLeft(text) { (t, e) =>
        t.substring(0, index.offset(e.getRange.getStart)) + e.getNewText + t.substring(index.offset(e.getRange.getEnd))
      }
    }
    def renamed(at: Position, name: String): Map[String, String] = {
      val changes = service.rename(parentUri, at, name).fold(e => fail(e), identity)
      changes.map { case (u, es) => (if (u.endsWith("Child.st")) "child" else "parent") -> applyEdits(if (u.endsWith("Child.st")) childText else parentText, es) }
    }
    assertEquals(service.prepareRename(parentUri, pos(parentText, ":title", 2)).map(_._2), Some("title"))
    val r1 = renamed(pos(parentText, "title=\"a\"", 1), "heading")
    assert(r1("parent").contains("<Child heading=\"a\"") && r1("parent").contains("<Child :heading=\"x.toString\""), r1("parent"))
    assert(r1("child").contains("heading: String") && r1("child").contains("{{ props.heading }}") && r1("child").contains(":title=\"props.heading\""), r1("child"))
    val r2 = renamed(pos(parentText, "#footer-area", 2), "bottomBar")
    assert(r2("parent").contains("<template #bottom-bar>"), r2("parent"))
    assert(r2("child").contains("bottomBar: Slot") && r2("child").contains("<slot name=\"bottom-bar\" />"), r2("child"))
    val r3 = renamed(pos(parentText, "@close", 2), "onDismiss")
    assert(r3("parent").contains("@dismiss=\"()\""), r3("parent"))
    assert(service.rename(parentUri, pos(parentText, "@close", 2), "dismiss").isLeft, "イベントのpropはonXxxの名前だけ")
    assert(service.rename(parentUri, pos(parentText, "def x", 4), "y").isLeft, "propでなければ変更しない")
    assert(service.prepareRename(childUri, pos(childText, "footerArea", 3)).isDefined, "定義の上からも変更できる")
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
