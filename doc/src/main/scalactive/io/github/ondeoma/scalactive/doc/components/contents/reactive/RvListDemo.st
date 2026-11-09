<script lang="scala">
case class State(nums: RVList[Int] = RVList(List(1, 2)))

def initState() = State()
</script>

<template>
  <button @click="s.nums.add(s.nums.v.size + 1)">add</button>
  <button @click="s.nums.liftV(0).foreach(n => s.nums.update(0, n * 10))">update(0)</button>
  <button @click="if (s.nums.v.nonEmpty) s.nums.rm(0)">rm(0)</button>
  <button @click="s.nums := List(1, 2)">:=</button>
  <p>合計: {{ s.nums.mapC(_.sum) }}</p>
  <ul>
    <li v-for="n in s.nums">{{ n }}</li>
  </ul>
</template>
