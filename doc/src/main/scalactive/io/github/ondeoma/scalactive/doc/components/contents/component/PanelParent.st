<script lang="scala">
case class State(count: RV[Int] = RV(0))

def initState() = State()
</script>

<template>
  <div>
    <Panel title="お知らせ">
      <p>親の状態も使えます. count = {{ s.count }}</p>
      <button @click="s.count := s.count.v + 1">+1</button>
      <template #footer>名前付きスロット(#footer)の内容です.</template>
    </Panel>
    <Panel title="slotなし" />
  </div>
</template>
