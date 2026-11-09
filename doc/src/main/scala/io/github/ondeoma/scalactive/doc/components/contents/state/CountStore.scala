package io.github.ondeoma.scalactive.doc.components.contents.state

import io.github.ondeoma.scalactive.reactive.*

// 複数のコンポーネントで共有する状態
object CountStore {

  val count: RV[Int] = RV(0)

  def increment(): Unit = count := count.v + 1

}
