<script lang="scala">
case class Props(label: String)
</script>

<template>
  <span class="chip">{{ props.label }}</span>
</template>

<style scoped>
.chip { display: inline-block; padding: 0 8px; border-radius: 4px; background: #ddd; color: #222; }
</style>
