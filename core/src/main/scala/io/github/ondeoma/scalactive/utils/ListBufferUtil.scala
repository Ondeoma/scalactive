package io.github.ondeoma.scalactive.utils

import scala.collection.mutable

/** ListBuffer/ArrayBufferなど、可変のBufferで要素を1つ前後に動かします. */
object ListBufferUtil {

  extension [A](l: mutable.Buffer[A]) {

    def up(i: Int): Unit = {
      if (i <= 0) ()
      else l.lift(i).fold(l) { elem =>
        l.remove(i)
        l.insert(i - 1, elem)
      }
    }

    def down(i: Int): Unit = {
      if (i >= l.length - 1) ()
      else l.lift(i).fold(l) { elem =>
        l.remove(i)
        l.insert(i + 1, elem)
      }
    }
  }

}
