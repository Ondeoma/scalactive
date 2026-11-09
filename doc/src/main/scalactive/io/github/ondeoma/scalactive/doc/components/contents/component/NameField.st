<script lang="scala">
case class Props(label: String, model: RV[String], note: RV[String] = RV(""))
</script>

<template>
  <div>
    <label>{{ props.label }}: <input type="text" v-model="props.model"></label>
    <label>メモ: <input type="text" v-model="props.note"></label>
  </div>
</template>
