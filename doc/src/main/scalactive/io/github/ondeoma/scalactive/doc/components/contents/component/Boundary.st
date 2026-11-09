<script lang="scala">
case class State(crash: RV[Boolean] = RV(false))

def initState() = State()

// 描画中に例外を起こす
def boom(): String = throw new RuntimeException("描画中にエラーが起きました.")
</script>

<template>
  <div>
    <ErrorBoundary @error="println(event)">
      <p>中身: <span v-if="s.crash">{{ boom() }}</span><span v-else>正常です.</span></p>
      <button @click="s.crash := true">エラーを起こす</button>
      <template #fallback="err">
        <p>{{ err.message }} <button @click="s.crash := false; err.retry()">戻す</button></p>
      </template>
    </ErrorBoundary>
  </div>
</template>
