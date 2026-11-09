<script lang="scala">
case class State(count: RV[Int] = RV(0))

def initState() = State()
</script>

<template>
  <div>
    <Greeting user-name="Scala" />
    <Greeting user-name="Scalactive" :count="s.count" />
    <button @click="s.count := s.count.v + 1">+1</button>
  </div>
</template>
