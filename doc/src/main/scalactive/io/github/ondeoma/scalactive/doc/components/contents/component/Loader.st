<script lang="scala">
import scala.concurrent.{Future, Promise}

case class State(fail: RV[Boolean] = RV(false), on: RV[Boolean] = RV(true))

def initState() = State()

// 0.8秒後に完了する非同期の処理
def fetchMessage(fail: Boolean): Future[String] = {
  val p = Promise[String]()
  window.setTimeout(() => {
    if (fail) p.failure(new RuntimeException("読み込みに失敗しました."))
    else p.success("読み込みました.")
  }, 800)
  p.future
}
</script>

<template>
  <div>
    <label><input type="checkbox" v-model="s.fail"> 失敗させる</label>
    <button @click="s.on := false; s.on := true">再読み込み</button>
    <Await v-if="s.on" :load="() => fetchMessage(s.fail.v)">
      <template #pending><p>読み込み中...</p></template>
      <template #default="msg"><p>{{ msg }}</p></template>
      <template #error="err"><p>{{ err.message }} <button @click="err.retry()">再試行</button></p></template>
    </Await>
  </div>
</template>
