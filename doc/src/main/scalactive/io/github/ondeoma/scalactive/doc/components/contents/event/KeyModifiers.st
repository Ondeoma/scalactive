<script lang="scala">
case class State(input: RV[String] = RV(""),
                 msg: RV[String] = RV("-"))

def initState() = State()
</script>

<template>
  <input type="text" v-model="s.input" placeholder="Enter / Ctrl+Enter / Esc" @keydown.enter='s.msg := "Enter: " + s.input.v' @keydown.enter.ctrl='s.msg := "Ctrl+Enter: " + s.input.v' @keydown.esc='s.input := ""'>
  <p>{{ s.msg }}</p>
</template>
