<script lang="scala">
case class State(open: RV[Boolean] = RV(true))

def initState() = State()
</script>

<template>
  <div>
    <Closable v-if="s.open" message="×で閉じます." @close="s.open := false" />
    <button v-else @click="s.open := true">再表示</button>
  </div>
</template>
