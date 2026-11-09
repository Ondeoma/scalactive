<script lang="scala">
case class State(crash: RV[Boolean] = RV(false))

def initState() = State()

// 描画中に例外を起こす
def boom(): String = throw new RuntimeException("描画中にエラーが起きました")
</script>

<template>
  <ErrorBoundary @error="println(event)">
    <p>
      <span v-if="s.crash">{{ boom() }}</span><span v-else>正常です.</span>
      <button @click="s.crash := true">エラーを起こす</button>
    </p>
    <template #fallback="err">
      <p class="error">{{ err.message }} <button @click="s.crash := false; err.retry()">戻す</button></p>
    </template>
  </ErrorBoundary>
</template>

<style scoped>
.error { color: tomato; }
</style>
