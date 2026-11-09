<script lang="scala">
import scala.concurrent.{Future, Promise}

case class State(fail: RV[Boolean] = RV(false),
                 show: RV[Boolean] = RV(true))

def initState() = State()

// 0.8秒後に完了する非同期の処理
def fetchQuote(fail: Boolean): Future[String] = {
  val p = Promise[String]()
  window.setTimeout(
    () => {
      if (fail) p.failure(new RuntimeException("読み込みに失敗しました"))
      else p.success("Scalactiveは、Scala.jsのリアクティブなフレームワークです.")
    },
    800)
  p.future
}
</script>

<template>
  <div>
    <label><input type="checkbox" v-model="s.fail"> 失敗させる</label>
    <!-- v-ifを付け直してAwaitを作り直す(もう1度読み込む) -->
    <button @click="s.show := false; s.show := true">読み込み直す</button>
    <Await v-if="s.show" :load="() => fetchQuote(s.fail.v)">
      <template #pending><p>読み込み中...</p></template>
      <template #default="quote"><p>{{ quote }}</p></template>
      <template #error="err"><p class="error">{{ err.message }} <button @click="err.retry()">再試行</button></p></template>
    </Await>
  </div>
</template>

<style scoped>
.error { color: tomato; }
</style>
