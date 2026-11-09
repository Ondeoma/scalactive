package io.github.ondeoma.scalactive.compiler

class ScalaSpacingSuite extends munit.FunSuite {

  private def fmt(s: String): String = ScalaSpacing.format(s).get

  test("行の途中の連続した空白を1つにし、括弧の内側と , の前の空白を取り除く") {
    assertEquals(fmt("val aa   =  A(10    )"), "val aa = A(10)")
    assertEquals(fmt("f( a ,  b )(List[ Int ]( 1 ))"), "f(a, b)(List[Int](1))")
    assertEquals(fmt("if (  x  ) {\n  y   +  1\n}"), "if (x) {\n  y + 1\n}")
  }

  test("インデント・文字列・文字リテラル・コメントは変えない") {
    val src =
      """case class Props(title: String,
        |                 onClose: Unit => Unit = _ => ())
        |val s = "a   b ( c )" + s"x  ${ a  +  b }  y"
        |val c = ' ' ; val q = '{  x  }
        |val t = TQmulti   line
        |   (  keep  )TQ
        |/* block   /* nested   */  ( x ) */
        |val d = 1   // comment   here""".stripMargin.replace("TQ", "\"\"\"")
    val expected =
      """case class Props(title: String,
        |                 onClose: Unit => Unit = _ => ())
        |val s = "a   b ( c )" + s"x  ${ a  +  b }  y"
        |val c = ' ' ; val q = '{ x }
        |val t = TQmulti   line
        |   (  keep  )TQ
        |/* block   /* nested   */  ( x ) */
        |val d = 1   // comment   here""".stripMargin.replace("TQ", "\"\"\"")
    assertEquals(fmt(src), expected)
    assertEquals(fmt(expected), expected, "2回目は変わらない")
  }

  test("閉じていない文字列・コメントはNone") {
    assertEquals(ScalaSpacing.format("val s = \"abc"), None)
    assertEquals(ScalaSpacing.format("/* x"), None)
  }

  test(".stの整形: scriptの余分な空白を取り除く") {
    val src = "<script lang=\"scala\">\ncase class A(a: Int)\nval aa   = A(10    )\n</script>\n<template>\n<p>{{ aa }}</p>\n</template>\n"
    val expected = "<script lang=\"scala\">\ncase class A(a: Int)\nval aa = A(10)\n</script>\n<template>\n  <p>{{ aa }}</p>\n</template>\n"
    assertEquals(StFormatter.format(src), expected)
  }

}
