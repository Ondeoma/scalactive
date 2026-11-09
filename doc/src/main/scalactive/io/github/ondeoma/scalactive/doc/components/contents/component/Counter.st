<script lang="scala">
case class State(count: RV[Int] = RV(0))

def initState() = State()
</script>

<template>
  <div>
    <p>count: {{ s.count }}</p>
    <button @click="s.count := s.count.v + 1">+1</button>
  </div>
</template>
