<script lang="scala">
case class State(trimmed: RV[String] = RV(""),
                 lazyText: RV[String] = RV(""))

def initState() = State()
</script>

<template>
  <p><input type="text" v-model.trim="s.trimmed"> → [{{ s.trimmed }}]</p>
  <p><input type="text" v-model.lazy="s.lazyText"> → {{ s.lazyText }}</p>
</template>
