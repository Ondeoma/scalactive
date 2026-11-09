<script lang="scala">
// v-modelで値を受け取る入力欄. 親は <StField label="名前" v-model="s.name" /> と書く
case class Props(label: String, model: RV[String])
</script>

<template>
  <label class="st-field">{{ props.label }}: <input type="text" v-model="props.model"></label>
</template>

<style scoped>
.st-field input { margin-left: 4px; }
</style>
