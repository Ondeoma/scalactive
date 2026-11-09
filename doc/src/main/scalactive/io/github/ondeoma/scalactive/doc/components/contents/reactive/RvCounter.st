<script lang="scala">
case class State(name: RV[String] = RV("Taro"),
                 count: RV[Int] = RV(0))

def initState() = State()
</script>

<template>
  <p><input type="text" v-model="s.name"></p>
  <p>{{ s.name }}さんのカウント: {{ s.count }}</p>
  <button @click="s.count := s.count.v + 1">+1</button>
  <button @click="s.count := 0">0に戻す</button>
</template>
