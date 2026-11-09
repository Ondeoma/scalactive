<script lang="scala">
case class State(name: RV[String] = RV(""),
                 log: RVList[String] = RVList(List[String]()))

def initState() = State()

def put(s: State, msg: String): Unit = {
  s.log.add(msg)
  if (s.log.v.size > 3) s.log.rm(0)
}
</script>

<template>
  <!-- .prevent: 送信でページが移動する(既定の動作)のを止めます -->
  <form @submit.prevent='put(s, "送信: " + s.name.v)'>
    <input type="text" v-model="s.name">
    <button type="submit">送信</button>
  </form>
  <!-- .stop: 親要素へ伝わるのを止めます -->
  <div class="box" @click='put(s, "外側のdiv")'>
    <button @click='put(s, "伝わるボタン")'>伝わる</button>
    <button @click.stop='put(s, "止めるボタン")'>止める</button>
  </div>
  <!-- .self: 子要素ではなく、その要素自体で起きた時だけ実行します -->
  <div class="box" @click.self='put(s, "余白(.self)")'>
    <button>子要素のボタン</button> 余白をクリック
  </div>
  <ul>
    <li v-for="l in s.log">{{ l }}</li>
  </ul>
</template>

<style scoped>
.box { margin: 8px 0; padding: 8px; border: 1px dashed #999; }
</style>
