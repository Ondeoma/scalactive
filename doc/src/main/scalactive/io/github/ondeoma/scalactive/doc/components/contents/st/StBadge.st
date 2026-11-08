<script lang="scala">
case class Props(label: String = ".st",
                 count: Option[Reactive[Int]] = None)
</script>

<template>
  <span class="badge">{{ props.label }}<b v-for="c in props.count.toList">: {{ c }}</b></span>
</template>

<style scoped>
.badge {
  display: inline-block;
  padding: 0 6px;
  border-radius: 4px;
  background: #3b82f6;
  color: white;
  font-size: 12px;
}
</style>
