<script lang="scala">
def greet(): Unit = window.alert("Hello!")
</script>

<template>
  <div class="hello">
    <p>これは Hello です.</p>
    <button @click="greet()">Click</button>
  </div>
</template>

<style scoped>
.hello p { color: seagreen; }
</style>
