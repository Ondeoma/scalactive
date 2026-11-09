package io.github.ondeoma.scalactive.compiler

import scala.collection.mutable.ArrayBuffer

/**
 * .st上のオフセットを1文字ずつ持つ文字列.
 * エンティティのデコードやtrimをしても、各文字の元の位置を追えるようにします.
 */
final case class Located(text: String, offsets: Array[Int]) {

  require(text.length == offsets.length)

  def isEmpty: Boolean = text.isEmpty

  def substring(from: Int, until: Int): Located = Located(text.substring(from, until), offsets.slice(from, until))

  def substring(from: Int): Located = substring(from, text.length)

  def trim: Located = {
    var s = 0
    var e = text.length
    while (s < e && text.charAt(s) <= ' ') s += 1
    while (e > s && text.charAt(e - 1) <= ' ') e -= 1
    substring(s, e)
  }

}

object Located {

  /** .stのstart位置から連続する文字列 */
  def apply(text: String, start: Int): Located = Located(text, Array.tabulate(text.length)(start + _))

  private val entities = List(
    "&quot;" -> '"',
    "&#39;" -> '\'',
    "&apos;" -> '\'',
    "&lt;" -> '<',
    "&gt;" -> '>',
    "&amp;" -> '&'
  )

  /** 属性値中のHTMLエンティティをデコードします. デコード後の1文字には、エンティティ先頭の位置を対応させます. */
  def decodeEntities(l: Located): Located = {
    val sb = new StringBuilder
    val offs = ArrayBuffer[Int]()
    var i = 0
    while (i < l.text.length) {
      entities.find { case (e, _) => l.text.startsWith(e, i) } match {
        case Some((e, c)) =>
          sb += c
          offs += l.offsets(i)
          i += e.length
        case None =>
          sb += l.text.charAt(i)
          offs += l.offsets(i)
          i += 1
      }
    }
    Located(sb.toString, offs.toArray)
  }

}

/**
 * 生成コードにユーザーのコードを埋め込む際の目印.
 * mark()で囲んだ部分は、extract()で目印を取り除いた上でSourceMapに記録されます.
 */
final class Marks {

  private val table = ArrayBuffer[Array[Int]]()

  def mark(l: Located): String = {
    table += l.offsets
    s"${Marks.Start}${table.size - 1}${Marks.Sep}${l.text}${Marks.End}"
  }

  def mark(text: String, start: Int): String = mark(Located(text, start))

  /** 目印を取り除いたコードと、そのSourceMapを返します. */
  def extract(marked: String): (String, SourceMap) = {
    val out = new StringBuilder
    val map = ArrayBuffer[Int]()
    var i = 0
    while (i < marked.length) {
      marked.charAt(i) match {
        case Marks.Start =>
          val sep = marked.indexOf(Marks.Sep, i)
          val end = marked.indexOf(Marks.End, sep)
          val offsets = table(marked.substring(i + 1, sep).toInt)
          var k = sep + 1
          while (k < end) {
            out += marked.charAt(k)
            map += offsets(k - sep - 1)
            k += 1
          }
          i = end + 1
        case c =>
          out += c
          map += -1
          i += 1
      }
    }
    (out.toString, new SourceMap(map.toArray))
  }

}

object Marks {
  // Unicodeの私用領域. ユーザーのコードに現れることはない前提です.
  private[compiler] val Start = ''
  private[compiler] val Sep = ''
  private[compiler] val End = ''
}

/**
 * 生成コードのオフセット ↔ .stのオフセットの対応.
 * 生成コードのうち、ユーザーが書いたコード(<script>の中身、テンプレート中の式など)の文字だけが対応を持ちます.
 *
 * @param genToSt 生成コードの各文字に対応する.st上のオフセット. 生成されたコード(グルー)は-1.
 */
final class SourceMap(genToSt: Array[Int]) {

  private lazy val stToGen: Map[Int, Int] = {
    val b = Map.newBuilder[Int, Int]
    var seen = Set.empty[Int]
    var g = 0
    while (g < genToSt.length) {
      val s = genToSt(g)
      if (s >= 0 && !seen.contains(s)) {
        seen += s
        b += s -> g
      }
      g += 1
    }
    b.result()
  }

  def genLength: Int = genToSt.length

  /** 生成コードの文字位置 → .stの文字位置 */
  def toSt(gen: Int): Option[Int] = {
    if (gen >= 0 && gen < genToSt.length && genToSt(gen) >= 0) Some(genToSt(gen)) else None
  }

  /** .stの文字位置 → 生成コードの文字位置 */
  def toGen(st: Int): Option[Int] = stToGen.get(st)

  /**
   * .stのカーソル位置(文字と文字の間) → 生成コードのカーソル位置.
   * 式の末尾(直後が閉じ引用符や}}など)にあるカーソルも、直前の文字から対応を求めます.
   */
  def cursorToGen(st: Int): Option[Int] = {
    toGen(st).orElse(toGen(st - 1).map(_ + 1))
  }

  /** 生成コードのカーソル位置 → .stのカーソル位置 */
  def cursorToSt(gen: Int): Option[Int] = {
    toSt(gen).orElse(toSt(gen - 1).map(_ + 1))
  }

  /** 生成コードの範囲[start, end) → .stの範囲. 両端が対応を持つ場合のみ. */
  def rangeToSt(start: Int, end: Int): Option[(Int, Int)] = {
    if (end <= start) cursorToSt(start).map(s => (s, s))
    else for {
      s <- toSt(start).orElse(cursorToSt(start))
      e <- toSt(end - 1).map(_ + 1).orElse(cursorToSt(end))
      if s <= e
    } yield (s, e)
  }

  /**
   * 生成コードの範囲を.stに対応付けます. 対応がない場合(生成されたコード上のエラーなど)は、
   * 範囲内か、それより前で最も近いユーザーのコードの位置を返します.
   */
  def nearestRangeToSt(start: Int, end: Int): Option[(Int, Int)] = {
    rangeToSt(start, end).orElse {
      def first(r: Range): Option[Int] = r.iterator.map(toSt).collectFirst { case Some(s) => s }
      first(start until math.min(math.max(end, start + 1), genToSt.length))
        .orElse(first(math.min(start, genToSt.length) - 1 to 0 by -1))
        .map(s => (s, s + 1))
    }
  }

}

object SourceMap {
  val empty: SourceMap = new SourceMap(Array.emptyIntArray)
}
