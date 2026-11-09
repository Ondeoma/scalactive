<script lang="scala">
case class State(fruits: RVList[String] = RVList(List("りんご", "みかん", "ぶどう")))

def initState() = State()
</script>

<template>
  <ul>
    <li v-for="(f, i) in s.fruits">
      {{ i + 1 }}. {{ f }}
      <button @click="s.fruits.rm(i)">削除</button>
    </li>
  </ul>
  <button @click='s.fruits := List("りんご", "みかん", "ぶどう")'>元に戻す</button>
</template>
