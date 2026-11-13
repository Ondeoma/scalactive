<script lang="scala">
case class Props(to: String, slot: Slot = Slot.empty)
</script>
<template>
  <a :href="props.to"><slot /></a>
</template>
