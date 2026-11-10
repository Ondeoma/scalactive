package io.github.ondeoma.scalactive.storage

import scala.compiletime.{constValueTuple, erasedValue, summonFrom}
import scala.deriving.Mirror
import scala.scalajs.js

/**
 * PersistentStoreで保存する値と、JSON(js.Any)との変換.
 *
 * String・Int・Long・Double・Boolean・Option・List・Vector・Seq・Set・Map[String, A] は標準で使えます.
 * case class・enum・sealed traitは `derives StorageCodec` で使えます(項目の型も自動で導出します).
 * それ以外の型は [[StorageCodec.from]] で定義してください.
 */
trait StorageCodec[A] {

  def encode(a: A): js.Any

  def decode(json: js.Any): Either[String, A]

}

object StorageCodec {

  def apply[A](using c: StorageCodec[A]): StorageCodec[A] = c

  /** 変換を関数で定義します. decodeの失敗はLeft(理由)で返してください. */
  def from[A](enc: A => js.Any,
              dec: js.Any => Either[String, A]): StorageCodec[A] = new StorageCodec[A] {
    def encode(a: A): js.Any = enc(a)

    def decode(json: js.Any): Either[String, A] = dec(json)
  }

  /** 別の型の変換を使って定義します. 例: `StorageCodec.map[String, Color](_.name, n => Color.parse(n))` */
  def map[B, A](to: A => B,
                back: B => Either[String, A])
               (using cb: StorageCodec[B]): StorageCodec[A] = {
    from(a => cb.encode(to(a)), j => cb.decode(j).flatMap(back))
  }

  private def show(j: js.Any): String = {
    if (js.isUndefined(j)) "undefined" else js.JSON.stringify(j)
  }

  private def expected(t: String, j: js.Any): Left[String, Nothing] = Left(s"expected $t but got ${show(j)}")

  private def isObject(j: js.Any): Boolean = {
    j != null && js.typeOf(j) == "object" && !js.Array.isArray(j)
  }

  given StorageCodec[String] = from(
    s => s,
    j => if (js.typeOf(j) == "string") Right(j.asInstanceOf[String]) else expected("string", j)
  )

  given StorageCodec[Boolean] = from(
    b => b,
    j => if (js.typeOf(j) == "boolean") Right(j.asInstanceOf[Boolean]) else expected("boolean", j)
  )

  given StorageCodec[Double] = from(
    d => d,
    j => if (js.typeOf(j) == "number") Right(j.asInstanceOf[Double]) else expected("number", j)
  )

  given StorageCodec[Int] = from(
    i => i,
    j => {
      val ok = js.typeOf(j) == "number" && {
        val d = j.asInstanceOf[Double]
        d.isValidInt
      }
      if (ok) Right(j.asInstanceOf[Double].toInt) else expected("int", j)
    }
  )

  /** JavaScriptの数値では精度が足りないため、文字列で保存します. */
  given StorageCodec[Long] = from(
    l => l.toString,
    j => js.typeOf(j) match {
      case "string" => j.asInstanceOf[String].toLongOption.toRight(s"expected long but got ${show(j)}")
      case "number" if j.asInstanceOf[Double].isWhole => Right(j.asInstanceOf[Double].toLong)
      case _ => expected("long", j)
    }
  )

  /** Noneはnullで保存します. */
  given [A](using c: StorageCodec[A]): StorageCodec[Option[A]] = from(
    {
      case Some(a) => c.encode(a)
      case None => null
    },
    j => if (j == null || js.isUndefined(j)) Right(None) else c.decode(j).map(Some(_))
  )

  private def decodeArray[A](j: js.Any)(using c: StorageCodec[A]): Either[String, List[A]] = {
    if (!js.Array.isArray(j)) expected("array", j)
    else {
      val arr = j.asInstanceOf[js.Array[js.Any]]
      val buf = List.newBuilder[A]
      var err: Option[String] = None
      var i = 0
      while (err.isEmpty && i < arr.length) {
        c.decode(arr(i)) match {
          case Right(a) => buf += a
          case Left(e) => err = Some(s"[$i]: $e")
        }
        i += 1
      }
      err.toLeft(buf.result())
    }
  }

  private def encodeArray[A](as: Iterable[A])(using c: StorageCodec[A]): js.Any = {
    val arr = js.Array[js.Any]()
    as.foreach(a => arr.push(c.encode(a)))
    arr
  }

  given [A](using StorageCodec[A]): StorageCodec[List[A]] = from(encodeArray(_), decodeArray(_))

  given [A](using StorageCodec[A]): StorageCodec[Vector[A]] = from(encodeArray(_), decodeArray(_).map(_.toVector))

  given [A](using StorageCodec[A]): StorageCodec[Seq[A]] = from(encodeArray(_), decodeArray(_))

  given [A](using StorageCodec[A]): StorageCodec[Set[A]] = from(encodeArray(_), decodeArray(_).map(_.toSet))

  given [A](using c: StorageCodec[A]): StorageCodec[Map[String, A]] = from(
    m => {
      val d = js.Dictionary[js.Any]()
      m.foreach((k, v) => d(k) = c.encode(v))
      d
    },
    j => {
      if (!isObject(j)) expected("object", j)
      else {
        val d = j.asInstanceOf[js.Dictionary[js.Any]]
        d.toList.foldLeft[Either[String, Map[String, A]]](Right(Map.empty)) {
          case (acc, (k, v)) => acc.flatMap(m => c.decode(v).left.map(e => s"$k: $e").map(a => m + (k -> a)))
        }
      }
    }
  )

  // ---- case class・enum・sealed traitの自動導出 ----

  /**
   * case classは項目名をキーにしたオブジェクト、enum・sealed traitは `{"$type": 名前, "value": 中身}` で保存します.
   * 項目の型に変換がなければ、その型も自動で導出します.
   */
  inline def derived[A](using m: Mirror.Of[A]): StorageCodec[A] = {
    val labels = constValueTuple[m.MirroredElemLabels].toList.asInstanceOf[List[String]]
    val codecs = summonCodecs[m.MirroredElemTypes]
    inline m match {
      case p: Mirror.ProductOf[A] => product(p, labels, codecs)
      case s: Mirror.SumOf[A] => sum(s, labels, codecs)
    }
  }

  private inline def summonCodecs[T <: Tuple]: List[StorageCodec[?]] = {
    inline erasedValue[T] match {
      case _: EmptyTuple => Nil
      case _: (t *: ts) => summonCodec[t] :: summonCodecs[ts]
    }
  }

  private inline def summonCodec[T]: StorageCodec[T] = {
    summonFrom {
      case c: StorageCodec[T] => c
      case m: Mirror.Of[T] => derived[T](using m)
    }
  }

  private def product[A](p: Mirror.ProductOf[A],
                         labels: List[String],
                         codecs: List[StorageCodec[?]]): StorageCodec[A] = {
    val fields = labels.zip(codecs.map(_.asInstanceOf[StorageCodec[Any]])).toArray
    from(
      a => {
        val d = js.Dictionary[js.Any]()
        val it = a.asInstanceOf[Product].productIterator
        var i = 0
        while (it.hasNext) {
          val (name, c) = fields(i)
          d(name) = c.encode(it.next())
          i += 1
        }
        d
      },
      j => {
        if (!isObject(j)) expected("object", j)
        else {
          val d = j.asInstanceOf[js.Dictionary[js.Any]]
          val values = new Array[Any](fields.length)
          var err: Option[String] = None
          var i = 0
          while (err.isEmpty && i < fields.length) {
            val (name, c) = fields(i)
            d.get(name) match {
              case Some(v) => c.decode(v) match {
                case Right(x) => values(i) = x
                case Left(e) => err = Some(s"$name: $e")
              }
              case None =>
                // Optionの項目は、無ければNoneにする
                c.decode(js.undefined) match {
                  case Right(x) => values(i) = x
                  case Left(_) => err = Some(s"$name: missing")
                }
            }
            i += 1
          }
          err.toLeft(p.fromProduct(Tuple.fromArray(values.asInstanceOf[Array[Object]])))
        }
      }
    )
  }

  private def sum[A](s: Mirror.SumOf[A],
                     labels: List[String],
                     codecs: List[StorageCodec[?]]): StorageCodec[A] = {
    val cases = labels.zip(codecs.map(_.asInstanceOf[StorageCodec[Any]])).toArray
    from(
      a => {
        val (name, c) = cases(s.ordinal(a))
        js.Dictionary[js.Any]("$type" -> name, "value" -> c.encode(a))
      },
      j => {
        if (!isObject(j)) expected("object", j)
        else {
          val d = j.asInstanceOf[js.Dictionary[js.Any]]
          d.get("$type") match {
            case Some(t) if js.typeOf(t) == "string" =>
              cases.find(_._1 == t.asInstanceOf[String]) match {
                case Some((_, c)) => c.decode(d.getOrElse("value", js.undefined)).map(_.asInstanceOf[A])
                case None => Left(s"unknown type ${show(t)}")
              }
            case _ => Left(s"missing $$type in ${show(j)}")
          }
        }
      }
    )
  }

}
