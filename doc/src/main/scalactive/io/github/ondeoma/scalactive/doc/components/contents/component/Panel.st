<script lang="scala">
case class Props(title: String,
                 slot: Slot = Slot.empty,
                 footer: Slot = Slot.empty)
</script>

<template>
  <div class="panel">
    <b>{{ props.title }}</b>
    <slot><p>(内容がありません)</p></slot>
    <small><slot name="footer">(footerなし)</slot></small>
  </div>
</template>

<style scoped>
.panel { border: 1px solid #ccc; border-radius: 6px; padding: 8px; margin: 4px 0; }
</style>
