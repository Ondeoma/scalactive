<script lang="scala">
case class State(n: RV[Int] = RV(0))
def initState() = State()
</script>
<template>
  <p>{{ s.n }}</p>
</template>
