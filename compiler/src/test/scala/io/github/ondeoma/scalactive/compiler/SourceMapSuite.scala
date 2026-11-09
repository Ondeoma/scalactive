package io.github.ondeoma.scalactive.compiler

/**
 * 生成コードと.stの位置の対応(LSPで補完・ホバーなどの位置変換に使う)のテスト.
 */
class SourceMapSuite extends munit.FunSuite {

  private val src =
    """<script lang="scala">
      |  case class State(count: RV[Int] = RV(0))
      |  def initState() = State()
      |</script>
      |
      |<template>
      |  <div :title="s.count.mapC(_.toString)" @click.prevent="s.count := 1">
      |    <p v-if="if (true) &quot;a&quot; == &quot;b&quot; else false">{{ s.count }}</p>
      |    <li v-for="(x, i) in List(1, 2)">{{ x + i }}</li>
      |    <Child :count="s.count" title="t" />
      |  </div>
      |</template>
      |""".stripMargin

  private val out = StCompiler.compile(StInput(src, "a/Foo.st", "Foo", Some("a"))).fold(e => fail(e.toString), identity)

  /** .st上のtext(n番目の出現)が、生成コード上の同じ文字列に対応することを確認します. */
  private def assertMapped(text: String, n: Int = 0)(implicit loc: munit.Location): Unit = {
    val st = Iterator.iterate(src.indexOf(text))(i => src.indexOf(text, i + 1)).drop(n).next()
    assert(st >= 0, s"'$text' not found in .st")
    val gen = out.sourceMap.toGen(st).getOrElse(fail(s"'$text' at $st is not mapped"))
    assertEquals(out.code.substring(gen, gen + text.length), text)
    assertEquals(out.sourceMap.rangeToSt(gen, gen + text.length), Some((st, st + text.length)))
  }

  test("生成コードには目印が残らない") {
    assert(!out.code.exists(c => c >= '' && c <= ''))
    assertEquals(out.sourceMap.genLength, out.code.length)
  }

  test("<script>の各行") {
    assertMapped("case class State")
    assertMapped("def initState() = State()")
  }

  test("属性値・{{ }}・イベントの式") {
    assertMapped("s.count.mapC(_.toString)")
    assertMapped("s.count := 1")
    assertMapped("s.count }}".dropRight(3), 2)
    assertMapped("List(1, 2)")
    assertMapped("x + i")
  }

  test("v-forの変数") {
    val st = src.indexOf("(x, i)") + 1
    val gen = out.sourceMap.toGen(st).get
    assert(out.code.substring(0, gen).matches("(?s).*\\(__cm\\d+, $"), clue = out.code.substring(gen - 10, gen + 5))
    assertEquals(out.code.substring(gen, gen + 4), "x, i")
    assertEquals(out.sourceMap.toSt(gen + 3), Some(st + 3))
  }

  test("エンティティをデコードした式は、エンティティ先頭の位置に対応する") {
    val st = src.indexOf("&quot;a")
    val gen = out.sourceMap.toGen(st).get
    assertEquals(out.code.substring(gen, gen + 3), "\"a\"")
    assertEquals(out.sourceMap.toSt(gen + 1), Some(st + "&quot;".length))
    assertMapped("== ")
  }

  test("子コンポーネントのタグ名とprops名") {
    assertMapped("Child")
    assertMapped("count=\"s.count\"".take(5))
    assertMapped("title=\"t\"".take(5), 1)
    // 文字列リテラルとして渡す値そのものは対応を持たない
    assert(out.sourceMap.toGen(src.indexOf("title=\"t\"") + 7).isEmpty)
  }

  test("カーソル位置: 式の末尾は直前の文字から求める") {
    val st = src.indexOf("s.count }}") + "s.count".length
    val gen = out.sourceMap.cursorToGen(st).get
    assert(out.code.substring(0, gen).endsWith("s.count"))
    assertEquals(out.sourceMap.cursorToSt(gen), Some(st))
  }

  test("生成されたコード上の位置は、近いユーザーのコードに対応付ける") {
    val gen = out.code.indexOf("__st.text")
    assertEquals(out.sourceMap.toSt(gen), None)
    assert(out.sourceMap.nearestRangeToSt(gen, gen + 9).isDefined)
  }

  test("エラーは.st上のオフセットを持つ") {
    val e = StCompiler.compile(StInput("<template>\n  <p v-else>a</p>\n</template>", "Foo.st", "Foo", None)).left.get
    assertEquals(e.offset, "<template>\n  <p ".length)
  }

}
