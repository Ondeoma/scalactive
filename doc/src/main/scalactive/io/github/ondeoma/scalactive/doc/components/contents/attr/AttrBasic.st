<script lang="scala">
case class State(url: RV[String] = RV("https://www.scala-js.org/"))

def initState() = State()
</script>

<template>
  <p><input type="text" v-model="s.url" size="30"></p>
  <p><a :href="s.url" v-bind:title="s.url" target="_blank">リンク</a></p>
  <p><input type="text" :maxlength="10" placeholder="10文字まで"></p>
</template>
