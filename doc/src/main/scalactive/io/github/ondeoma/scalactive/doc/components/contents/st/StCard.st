<script lang="scala">
case class Props(title: String,
                 onClose: Unit => Unit = _ => (),
                 slot: Slot = Slot.empty,
                 footer: Slot = Slot.empty)
</script>

<template>
  <div class="card">
    <div class="card-head">
      <b>{{ props.title }}</b>
      <button @click="props.onClose(())">×</button>
    </div>
    <slot><p>(内容がありません)</p></slot>
    <div class="card-foot"><slot name="footer"><small>(footerなし)</small></slot></div>
  </div>
</template>

<style scoped>
.card { border: 1px solid #ccc; border-radius: 6px; padding: 8px; margin: 8px 0; }
.card-head { display: flex; justify-content: space-between; align-items: center; }
.card-foot { border-top: 1px dashed #ccc; margin-top: 4px; padding-top: 4px; }
</style>
