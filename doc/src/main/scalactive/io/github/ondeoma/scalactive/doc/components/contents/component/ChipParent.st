<script lang="scala">
case class State(on: RV[Boolean] = RV(false))

def initState() = State()
</script>

<template>
  <div>
    <Chip label="そのまま" />
    <Chip label="引き継ぎ" :style='Map("background" -> s.on.mapC(o => if (o) "gold" else "#ddd"))' data-demo="chip" />
    <button @click="s.on := !s.on.v">色を切り替え</button>
  </div>
</template>
