<script lang="scala">
case class Props(title: String,
                 onClose: Unit => Unit = _ => (),
                 slot: Slot = Slot.empty)
</script>

<template>
  <div class="card">
    <div class="card-head">
      <b>{{ props.title }}</b>
      <button @click="props.onClose(())">×</button>
    </div>
    <slot><p>(内容がありません)</p></slot>
  </div>
</template>

<style scoped>
.card { border: 1px solid #ccc; border-radius: 6px; padding: 8px; margin: 8px 0; }
.card-head { display: flex; justify-content: space-between; align-items: center; }
</style>
