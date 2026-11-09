<script lang="scala">
val langs = List("Scala", "Scala.js", "Scalactive")
</script>

<template>
  <div>
    <ItemList :items="langs" />
    <ItemList :items="langs">
      <template #item="(name, i)"><b>{{ i + 1 }}.</b> {{ name }}</template>
    </ItemList>
  </div>
</template>
