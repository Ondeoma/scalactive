package io.github.ondeoma.scalactive.compiler

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import scala.collection.JavaConverters._

class StFormatterSuite extends munit.FunSuite {

  test("テンプレートを入れ子に合わせてインデントし直し、script/styleは前後の空行と行末だけ整える") {
    val src =
      """<script lang="scala">
        |
        |case class State(n: RV[Int] = RV(0))
        |  def initState() = State()
        |
        |</script>
        |<template>
        |<div class="a">
        |      <p v-if="s.n.mapC(_ > 0)">{{ s.n }}</p>
        |<ul>
        |<li v-for="x in xs">{{ x }}</li>
        |    </ul>
        |  <br>
        |<input v-model="s.name" />
        |</div>
        |</template>
        |<style scoped>
        |.a { color: red; }
        |</style>""".stripMargin
    val expected =
      """<script lang="scala">
        |case class State(n: RV[Int] = RV(0))
        |  def initState() = State()
        |</script>
        |<template>
        |  <div class="a">
        |    <p v-if="s.n.mapC(_ > 0)">{{ s.n }}</p>
        |    <ul>
        |      <li v-for="x in xs">{{ x }}</li>
        |    </ul>
        |    <br>
        |    <input v-model="s.name" />
        |  </div>
        |</template>
        |<style scoped>
        |.a { color: red; }
        |</style>
        |""".stripMargin
    assertEquals(StFormatter.format(src), expected)
    assertEquals(StFormatter.format(expected), expected, "2回目は変わらない")
  }

  test("複数行の開始タグ・コメント・{{ }}・<pre>の中身") {
    val src =
      """<template>
        |<div>
        |<Card title="t"
        |:count="s.n"
        |    @close="close()">
        |<p>a</p>
        |</Card>
        |<!-- comment
        |      keep -->
        |<p>{{ if (a < b)
        |        x else y }}</p>
        |<pre>
        |  keep
        |    this
        |</pre>
        |</div>
        |</template>""".stripMargin
    val expected =
      """<template>
        |  <div>
        |    <Card title="t"
        |        :count="s.n"
        |        @close="close()">
        |      <p>a</p>
        |    </Card>
        |    <!-- comment
        |      keep -->
        |    <p>{{ if (a < b)
        |        x else y }}</p>
        |    <pre>
        |  keep
        |    this
        |</pre>
        |  </div>
        |</template>
        |""".stripMargin
    assertEquals(StFormatter.format(src), expected)
    assertEquals(StFormatter.format(expected), expected)
  }

  test("構文エラーのある.stはそのまま返す") {
    val src = "<template>\n<p>"
    assertEquals(StFormatter.format(src), src)
  }

  test("ドキュメントサイトの.stは、1回整形したら2回目は変わらない") {
    val dir = Paths.get("doc/src/main/scalactive")
    val root = if (Files.exists(dir)) dir else Paths.get("../doc/src/main/scalactive")
    val files = Files.walk(root).iterator().asScala.filter(_.toString.endsWith(".st")).toList
    assert(files.nonEmpty)
    files.foreach { f =>
      val once = StFormatter.format(new String(Files.readAllBytes(f), StandardCharsets.UTF_8))
      assertEquals(StFormatter.format(once), once, f.toString)
      // 整形してもコンパイルできる
      assert(StCompiler.compile(StInput(once, f.getFileName.toString, f.getFileName.toString.stripSuffix(".st"), None)).isRight, f.toString)
    }
  }

}
