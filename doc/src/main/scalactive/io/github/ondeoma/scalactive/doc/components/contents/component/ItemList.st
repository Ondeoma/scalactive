<script lang="scala">
case class Props(items: List[String],
                 item: ScopedSlot[(String, Int)] = ScopedSlot.empty)
</script>

<template>
  <ul>
    <li v-for="(x, i) in props.items"><slot name="item" :value="(x, i)">{{ x }}</slot></li>
  </ul>
</template>
