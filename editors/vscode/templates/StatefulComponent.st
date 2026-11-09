<script lang="scala">
case class State(count: RV[Int] = RV(0))

def initState() = State()
</script>

<template>
  <div>
    <button @click="s.count := s.count.v + 1">count: {{ s.count }}</button>
  </div>
</template>

<style scoped>
</style>
