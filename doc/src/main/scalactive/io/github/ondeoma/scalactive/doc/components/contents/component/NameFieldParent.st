<script lang="scala">
case class State(name: RV[String] = RV("Scala"), memo: RV[String] = RV(""))

def initState() = State()
</script>

<template>
  <div>
    <NameField label="名前" v-model="s.name" v-model:note="s.memo" />
    <p>名前: {{ s.name }} / メモ: {{ s.memo }}</p>
  </div>
</template>
