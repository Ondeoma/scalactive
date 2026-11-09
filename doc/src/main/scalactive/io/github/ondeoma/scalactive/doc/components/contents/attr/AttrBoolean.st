<script lang="scala">
case class State(locked: RV[Boolean] = RV(true))

def initState() = State()
</script>

<template>
  <label><input type="checkbox" v-model="s.locked"> ロックする</label>
  <input type="text" :readonly="s.locked" value="編集できますか?">
  <button :disabled="s.locked">送信</button>
</template>
