<script lang="scala">
case class Props(message: String, onClose: Unit => Unit = _ => ())
</script>

<template>
  <p>{{ props.message }} <button @click="props.onClose(())">×</button></p>
</template>
