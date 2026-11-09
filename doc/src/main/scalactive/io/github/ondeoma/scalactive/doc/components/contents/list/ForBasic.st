<script lang="scala">
case class State(fruits: RVList[String] = RVList(List("りんご", "みかん")),
                 input: RV[String] = RV(""))

def initState() = State()

def add(s: State): Unit = {
  if (s.input.v.trim.nonEmpty) {
    s.fruits.add(s.input.v.trim)
    s.input := ""
  }
}
</script>

<template>
  <p>
    <input type="text" v-model="s.input" @keydown.enter="add(s)">
    <button @click="add(s)">追加</button>
  </p>
  <ul>
    <li v-for="f in s.fruits">{{ f }}</li>
  </ul>
</template>
