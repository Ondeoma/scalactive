<script lang="scala">
// スコープ付きスロットで、各行の表示を親に任せるリスト
case class Props(items: RVList[String],
                 item: ScopedSlot[(String, Int)] = ScopedSlot.empty,
                 empty: Slot = Slot.empty)
</script>

<template>
  <ul class="st-list">
    <li v-for="(rv, i) in props.items"><slot name="item" :value="(rv.v, i)">{{ rv }}</slot></li>
  </ul>
  <slot name="empty" v-if="props.items.mapC(_.isEmpty)" />
</template>

<style scoped>
.st-list { padding-left: 1.2em; }
</style>
