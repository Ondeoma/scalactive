<script lang="scala">
case class State(score: RV[Int] = RV(70))

def initState() = State()
</script>

<template>
  <p><input type="number" v-model="s.score" style="width: 5em">点</p>
  <p v-if="s.score.mapC(_ >= 80)">合格です!</p>
  <p v-else-if="s.score.mapC(_ >= 60)">あと少しです.</p>
  <p v-else>不合格です.</p>
</template>
