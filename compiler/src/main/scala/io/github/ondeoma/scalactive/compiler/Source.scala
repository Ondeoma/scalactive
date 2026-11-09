package io.github.ondeoma.scalactive.compiler

/**
 * @param offset エラー位置の.st上のオフセット
 */
final case class StError(fileName: String, line: Int, message: String, offset: Int = 0) {
  override def toString: String = s"$fileName:$line: $message"
}

final class StCompileException(val offset: Int, message: String) extends RuntimeException(message)

/**
 * .stファイルの全文. オフセットから行番号(1始まり)を求めるために使います.
 */
final class SourceText(val text: String) {

  private val lineStarts: Array[Int] = {
    val b = Array.newBuilder[Int]
    b += 0
    var i = 0
    while (i < text.length) {
      if (text.charAt(i) == '\n') b += i + 1
      i += 1
    }
    b.result()
  }

  def lineOf(offset: Int): Int = {
    val i = java.util.Arrays.binarySearch(lineStarts, offset)
    if (i >= 0) i + 1 else -i - 1
  }

  def fail(offset: Int, message: String): Nothing = {
    throw new StCompileException(offset, message)
  }

}
