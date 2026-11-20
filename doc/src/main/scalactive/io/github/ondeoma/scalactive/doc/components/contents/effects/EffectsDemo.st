<script lang="scala">
import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

private val fruits = List("apple", "apricot", "banana", "blueberry", "cherry", "grape", "lemon", "mango", "melon", "orange", "peach", "pear")

/** 疑似的なAPI(ms ミリ秒かかる) */
private def later[A](ms: Int)(a: => A): Future[A] = {
  val p = Promise[A]()
  org.scalajs.dom.window.setTimeout(() => p.success(a), ms)
  p.future
}

case class State(q: RV[String],
                 result: AsyncState[List[String]],
                 save: ActionState = useAction(),
                 saved: RV[Int] = RV(0),
                 now: RV[String] = RV(""))

def initState() = {
  val q = RV("")
  // reload() のたびに、その時の q で検索する
  State(q, useAsync(later(300)(fruits.filter(_.contains(q.v.trim.toLowerCase)))))
}

def mounted(s: State): Unit = {
  // 入力が止まってから検索する. どちらもコンポーネントが片付けられると止まる
  val search = useDebounce[String](250)(_ => s.result.reload())
  watch(s.q)((_, q) => search(q))
  // タブが表示されている間だけ、1秒ごとに時刻を更新する
  useVisiblePolling(1000, immediate = true)(s.now := new js.Date().toLocaleTimeString())
}

def save(s: State): Unit = s.save.run(later(1000)(())).foreach(_ => s.saved := s.saved.v + 1)
</script>

<template>
  <div>
    <p>
      <input v-model="s.q" placeholder="果物の名前(英語)">
      <span v-if="s.result.loading"> 検索中…</span>
    </p>
    <ul>
      <li v-for="f in s.result.getOrElse(Nil)">{{ f }}</li>
    </ul>
    <p>
      <button :disabled="s.save.busy" @click="save(s)">保存(1秒かかる)</button>
      保存した回数: {{ s.saved }}
    </p>
    <p>今の時刻: {{ s.now }}</p>
  </div>
</template>
