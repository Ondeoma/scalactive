<script lang="scala">
import scala.scalajs.js

// 描画されるときに初めてHelloのコードを読み込む
object LazyHello {
  def apply(): GenComponentF =
    asyncComponent(() => js.dynamicImport[GenComponentF](Hello()).toFuture, _ => "<p>読み込み中...</p>")
}

case class State(show: RV[Boolean] = RV(false))

def initState() = State()
</script>

<template>
  <div>
    <button @click="s.show := true">読み込む</button>
    <LazyHello v-if="s.show" />
  </div>
</template>
