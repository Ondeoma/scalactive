package io.github.ondeoma.scalactive.st

import scala.collection.mutable.ListBuffer

/**
 * 子コンポーネントのprops(`:name="expr"`)の値を集めます.
 * propsの式で読んだリアクティブ値が変わっても、propsの値が前と同じ(==)なら子を作り直さないために使います.
 */
private[scalactive] object PropKeys {

  /** Reactive型のprops(子が自分で追従するので、作り直しの判断には使わない) */
  case object ReactiveProp

  private var current: ListBuffer[Any] = null

  /** trueの間は、値を集めるだけ(Reactive型のpropsのcomputedを作らない) */
  private[st] var keyOnly: Boolean = false

  private[st] def record(v: Any): Unit = {
    if (current != null) current += v
  }

  /** bodyの中で計算したpropsの値を集めます */
  def collect[T](body: => T): (T, List[Any]) = {
    val prev = current
    val buf = ListBuffer[Any]()
    current = buf
    try (body, buf.toList)
    finally current = prev
  }

  /** propsの値だけを計算します(Reactive型のpropsは計算しません) */
  def keysOnly[T](body: => T): (T, List[Any]) = {
    val prev = keyOnly
    keyOnly = true
    try collect(body)
    finally keyOnly = prev
  }

}
