<script lang="scala">
case class State(count: RV[Int] = RV(0))

def initState() = State()
</script>

<template>
  <p>Hello, Scalactive!</p>
  <button @click="s.count := s.count.v + 1">count: {{ s.count }}</button>
</template>
