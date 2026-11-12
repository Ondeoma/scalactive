package io.github.ondeoma.scalactive.ls

/**
 * <style>からクラスの定義を探すテスト(lang="scss" / lang="sass" の入れ子・&・// コメントを含む).
 */
class CssClassesSuite extends munit.FunSuite {

  private def names(style: String): List[String] =
    CssClasses.definitions(s"<template><p></p></template>\n$style\n").map(_.name)

  /** 定義の位置の文字列 */
  private def at(style: String, name: String): String = {
    val text = s"<template><p></p></template>\n$style\n"
    val d = CssClasses.definitions(text).find(_.name == name).getOrElse(fail(s"$name not found"))
    text.substring(d.offset, d.offset + 6)
  }

  test("CSS: 宣言の中・コメント・文字列のクラスは数えない(これまでどおり)") {
    assertEquals(
      names("<style>/* .c1 {} */ .a { background: url(a.png); } .b, .x:hover { content: \".fake\"; } @media (w) { .m { } }</style>"),
      List("a", "b", "x", "m"))
  }

  test("SCSS: 入れ子のルール・&-suffix・&.class・@media・@includeの中") {
    val style =
      """<style lang="scss" scoped>
        |// .commented { }
        |$pad: 8px;
        |.card {
        |  padding: $pad;
        |  &-title, &-sub { color: red; }
        |  &.active { color: blue; }
        |  &:hover { opacity: 0.5; }
        |  .body {
        |    &__item { margin: 0; }
        |  }
        |  @media (max-width: 600px) {
        |    &-narrow { width: 100%; }
        |  }
        |  #{$sel} { x: y; }
        |}
        |@include mq { .mobile { a: b; } }
        |</style>""".stripMargin
    assertEquals(names(style), List("card", "card-title", "card-sub", "active", "body", "body__item", "card-narrow", "mobile"))
    assertEquals(at(style, "card-title"), "-title")
    assertEquals(at(style, "body"), "body {")
  }

  test("Sass(インデント記法): インデントで入れ子を読み、宣言・変数・a:hover を区別する") {
    val style =
      """<style lang="sass">
        |// .commented
        |$main: red
        |.card
        |  color: $main
        |  &-title
        |    font-weight: bold
        |  a:hover
        |    color: blue
        |.other
        |  +mixin
        |</style>""".stripMargin
    assertEquals(names(style), List("card", "card-title", "other"))
  }

}
