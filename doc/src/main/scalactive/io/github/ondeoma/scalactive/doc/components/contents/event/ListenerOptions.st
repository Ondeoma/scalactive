<script lang="scala">
case class State(once: RV[Int] = RV(0),
                 order: RV[String] = RV("-"),
                 wheel: RV[Int] = RV(0))

def initState() = State()
</script>

<template>
  <!-- .once: 最初の1回だけ実行します -->
  <button @click.once="s.once := s.once.v + 1">1回だけ数えます: {{ s.once }}</button>
  <!-- .capture: 子要素より先に実行します -->
  <div class="box" @click.capture='s.order := "div"'>
    <button @click='s.order := s.order.v + " → button"'>実行順を確認</button>
    {{ s.order }}
  </div>
  <!-- .passive: スクロールを妨げません -->
  <div class="box" @wheel.passive="s.wheel := s.wheel.v + 1">
    ここでホイールを回してください: {{ s.wheel }}
  </div>
</template>

<style scoped>
.box { margin: 8px 0; padding: 8px; border: 1px dashed #999; }
</style>
