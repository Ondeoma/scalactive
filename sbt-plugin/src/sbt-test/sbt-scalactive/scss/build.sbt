// <style lang="scss"> を、同梱のDart SassでCSSへ変換するテスト(Scala.jsでのコンパイルは行わない)
scalaVersion := "3.3.7"
enablePlugins(ScalactivePlugin)

TaskKey[Unit]("checkScss") := {
  val files = (Compile / scalactiveGenerate).value
  val styled = files.find(_.getName == "Styled.scala").getOrElse(sys.error(s"Styled.scala not generated: $files"))
  val code = IO.read(styled)
  def has(s: String): Unit = assert(code.contains(s), s"'$s' not found in:\n$code")
  // 入れ子・変数(.stからの相対パスの@useと、ソースディレクトリからの@use)
  has(".card .title[data-st-")
  has("color: red;")
  has("padding: 8px;")
  // &による結合
  has("]:hover {")
  // 変換後のCSSに、Sassの記法は残らない
  assert(!code.contains("$"), code)
  assert(!code.contains("@use"), code)
  // lang="sass"(インデント記法)
  has("em {\\n  font-weight: bold;\\n}")
}
