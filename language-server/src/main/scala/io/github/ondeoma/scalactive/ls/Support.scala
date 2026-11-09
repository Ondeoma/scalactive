package io.github.ondeoma.scalactive.ls

import org.eclipse.lsp4j.{Position, Range}

import java.net.URI
import java.util.concurrent.{CompletableFuture, CompletionStage}
import scala.meta.pc.{CancelToken, OffsetParams, VirtualFileParams}

/**
 * テキストのオフセット ↔ LSPの位置(行と、UTF-16単位の桁)の変換.
 */
final class LineIndex(text: String) {

  private val starts: Array[Int] = {
    val b = Array.newBuilder[Int]
    b += 0
    var i = 0
    while (i < text.length) {
      if (text.charAt(i) == '\n') b += i + 1
      i += 1
    }
    b.result()
  }

  def offset(p: Position): Int = {
    val line = math.max(0, math.min(p.getLine, starts.length - 1))
    val lineEnd = if (line + 1 < starts.length) starts(line + 1) - 1 else text.length
    math.min(starts(line) + math.max(0, p.getCharacter), lineEnd)
  }

  def position(offset: Int): Position = {
    val o = math.max(0, math.min(offset, text.length))
    val i = java.util.Arrays.binarySearch(starts, o)
    val line = if (i >= 0) i else -i - 2
    new Position(line, o - starts(line))
  }

  def range(start: Int, end: Int): Range = new Range(position(start), position(end))

}

/** キャンセルしないトークン */
object NoCancel extends CancelToken {
  override def onCancel(): CompletionStage[java.lang.Boolean] = CompletableFuture.completedFuture(java.lang.Boolean.FALSE)

  override def checkCanceled(): Unit = ()
}

final case class FileParams(uri: URI, text: String) extends VirtualFileParams {
  override def token(): CancelToken = NoCancel
}

final case class PositionParams(uri: URI, text: String, offset: Int) extends OffsetParams {
  override def token(): CancelToken = NoCancel
}

/** 推論した型のインレイヒントだけを求める範囲 */
final case class HintParams(uri: URI, text: String, offset: Int, endOffset: Int) extends scala.meta.pc.InlayHintsParams {
  override def token(): CancelToken = NoCancel
  override def inferredTypes(): Boolean = true
  override def typeParameters(): Boolean = false
  override def implicitParameters(): Boolean = false
  override def implicitConversions(): Boolean = false
  override def byNameParameters(): Boolean = false
  override def namedParameters(): Boolean = false
  override def hintsXRayMode(): Boolean = false
  override def hintsInPatternMatch(): Boolean = false
}
