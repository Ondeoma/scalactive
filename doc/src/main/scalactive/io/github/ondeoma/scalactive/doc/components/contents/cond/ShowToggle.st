<script lang="scala">
case class State(open: RV[Boolean] = RV(true))

def initState() = State()
</script>

<template>
  <label><input type="checkbox" v-model="s.open"> 表示する</label>
  <p v-show="s.open">v-showで切り替えています(非表示の間もDOMは残ります).</p>
  <!-- display: flex で表示したい場合 -->
  <div class="row" :style='"display" -> s.open.mapC(o => if (o) "flex" else "none")'>
    <span>flexの</span><span>要素です.</span>
  </div>
</template>

<style scoped>
.row { gap: 8px; }
</style>
