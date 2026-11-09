<script lang="scala">
case class State(count: RV[Int] = RV(0),
                 pos: RV[String] = RV("-"))

def initState() = State()

def showPos(s: State, e: MouseEvent): Unit = {
  s.pos := s"${e.clientX.toInt}, ${e.clientY.toInt}"
}
</script>

<template>
  <button @click="s.count := s.count.v + 1">クリック: {{ s.count }}</button>
  <button v-on:click="s.count := 0">リセット</button>
  <div class="area" @mousemove="showPos(s, event.asInstanceOf[MouseEvent])">
    マウスの位置: {{ s.pos }}
  </div>
</template>

<style scoped>
.area { margin-top: 8px; padding: 16px; background: mediumpurple; color: white; }
</style>
