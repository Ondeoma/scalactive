package io.github.ondeoma.scalactive.sbtplugin

import com.sass_lang.embedded_protocol.InboundMessage.CompileRequest.{Importer, StringInput}
import com.sass_lang.embedded_protocol.{OutputStyle, Syntax}
import de.larsgrefer.sass.embedded.{SassCompilationFailedException, SassCompiler, SassCompilerFactory}
import io.github.ondeoma.scalactive.compiler.{StyleCompiler, StyleError}

import java.io.File
import java.nio.file.Path
import scala.collection.JavaConverters._

/**
 * <style lang="scss"> / <style lang="sass"> を、同梱のDart SassでCSSへ変換します.
 *
 * Dart Sassのプロセスは、最初に変換するときに起動し、closeで止めます(scssを使わない場合は起動しません).
 * `@use` / `@import` は、.stのあるディレクトリからの相対パスと、loadPaths(.stのソースディレクトリ)から探します.
 */
final class SassStyles(loadPaths: Seq[File]) extends AutoCloseable {

  private var compiler: Option[SassCompiler] = None

  private def sass: SassCompiler = compiler.getOrElse {
    val c = SassCompilerFactory.bundled()
    c.setLoadPaths(loadPaths.asJava)
    compiler = Some(c)
    c
  }

  /** fileの<style>を変換するStyleCompiler */
  def forFile(file: Path): StyleCompiler = new StyleCompiler {
    private val url = file.toUri.toString

    override def compile(lang: String, source: String): Either[StyleError, String] = {
      val input = StringInput.newBuilder()
        .setSource(source)
        .setUrl(url)
        .setSyntax(if (lang == "sass") Syntax.INDENTED else Syntax.SCSS)
        .setImporter(Importer.newBuilder().setPath(file.getParent.toString))
        .build()
      try Right(sass.compileString(input, OutputStyle.EXPANDED).getCss)
      catch {
        case e: SassCompilationFailedException =>
          val f = e.getCompileFailure
          // .stの<style>の中で失敗した場合だけ行を返す(@useで読み込んだ別のファイルの場合は<style>の位置にする)
          val line = if (f.hasSpan && f.getSpan.getUrl == url) Some(f.getSpan.getStart.getLine) else None
          val where = if (f.hasSpan && line.isEmpty && f.getSpan.getUrl.nonEmpty) s" (${f.getSpan.getUrl}:${f.getSpan.getStart.getLine + 1})" else ""
          Left(StyleError(f.getMessage + where, line))
      }
    }
  }

  override def close(): Unit = {
    compiler.foreach(_.close())
    compiler = None
  }

}
