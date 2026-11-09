<script lang="scala">
case class State(count: RV[Int] = RV(0))

def initState() = State()
</script>

<template>
  <div>
    <p>count: {{ s.count }}</p>
    <button @click="s.count := s.count.v + 1">+1</button>
    <p v-if="s.count.mapC(_ >= 3)" class="many">3以上です!</p>
  </div>
</template>
