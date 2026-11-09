package io.github.ondeoma.scalactive.compiler

class CssFormatterSuite extends munit.FunSuite {

  private def fmt(css: String): String = CssFormatter.format(css).get

  test("1行のルールは1行のまま空白を整え、最後の宣言に;を補う") {
    assertEquals(fmt(".a{color:red;margin :0  4px}"), "\n.a { color: red; margin: 0 4px; }\n")
    assertEquals(fmt(".card { border: 1px solid #ccc; padding: 8px; }"), "\n.card { border: 1px solid #ccc; padding: 8px; }\n")
    assertEquals(fmt(":root{}"), "\n:root {}\n")
  }

  test("複数行のルールは宣言を1行ずつインデントする") {
    assertEquals(fmt("div{\n margin: 8px;\n      color:red}"), "\ndiv {\n  margin: 8px;\n  color: red;\n}\n")
  }

  test("@mediaの入れ子・空行・コメント・文字列") {
    val src =
      """
        |/* 見出し */
        |@import url("a.css");
        |
        |
        |@media (max-width: 600px) {
        |.a { color: red }
        |   .b{
        |content: "a  ;{ b";   /* 末尾のコメント */
        |background: url(data:image/png;base64,AAA)
        |}
        |}
        |a[title="x  y"],
        |b:hover { --main-color : #fff }
        |""".stripMargin
    val expected =
      """
        |/* 見出し */
        |@import url("a.css");
        |
        |@media (max-width: 600px) {
        |  .a { color: red; }
        |  .b {
        |    content: "a  ;{ b"; /* 末尾のコメント */
        |    background: url(data:image/png;base64,AAA);
        |  }
        |}
        |a[title="x  y"], b:hover {
        |  --main-color: #fff;
        |}
        |""".stripMargin
    assertEquals(fmt(src), expected)
    assertEquals(fmt(expected), expected, "2回目は変わらない")
  }

  test("読めないCSSはNone") {
    assertEquals(CssFormatter.format(".a { color: red;"), None)
    assertEquals(CssFormatter.format(".a { color: red; }}"), None)
    assertEquals(CssFormatter.format(".a { content: \"x }"), None)
    assertEquals(CssFormatter.format("/* x"), None)
  }

  test(".stの整形: <style>はCSSとして整形し、lang付きのstyleはそのまま") {
    val src = "<template>\n<p>a</p>\n</template>\n<style scoped>\ndiv{\n margin: 8px;\n}\n</style>\n<style lang=\"scss\">\n.a{ &:hover{color:red} }\n</style>\n"
    val expected = "<template>\n  <p>a</p>\n</template>\n<style scoped>\ndiv {\n  margin: 8px;\n}\n</style>\n<style lang=\"scss\">\n.a{ &:hover{color:red} }\n</style>\n"
    assertEquals(StFormatter.format(src), expected)
  }

}
