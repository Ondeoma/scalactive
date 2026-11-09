<script lang="scala">
case class Props(label: String, name: RV[String], note: RV[String] = RV(""))
</script>

<template>
  <div>
    <label>{{ props.label }}: <input type="text" v-model="props.name"></label>
    <label>メモ: <input type="text" v-model="props.note"></label>
  </div>
</template>
