package io.github.ondeoma.scalactive.compiler

/**
 * <style lang="scss"> などの、CSS以外で書いたスタイルをCSSへ変換します.
 * sbtプラグインがDart Sassで実装します(コンパイラ本体はSassに依存しません).
 */
trait StyleCompiler {

  /**
   * @param lang   <style>のlang("scss" または "sass")
   * @param source <style>の中身
   */
  def compile(lang: String, source: String): Either[StyleError, String]

}

/**
 * @param line 失敗した位置の、<style>の中身の先頭を0とする行(@useで読み込んだ別のファイルの場合はNone)
 */
final case class StyleError(message: String, line: Option[Int])

object StyleCompiler {

  /** StyleCompilerで変換するlang */
  val Langs: Set[String] = Set("scss", "sass")

}
