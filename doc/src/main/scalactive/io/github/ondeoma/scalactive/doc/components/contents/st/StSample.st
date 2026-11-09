<script lang="scala">
case class State(count: RV[Int] = RV(0),
                 todos: RVList[String] = RVList(List("Scala.js", "Scalactive")),
                 input: RV[String] = RV(""),
                 step: RV[Int] = RV(1),
                 agree: RV[Boolean] = RV(false),
                 color: RV[String] = RV("red"),
                 tags: RV[List[String]] = RV(List("scala")),
                 cardOpen: RV[Boolean] = RV(true),
                 mountedAt: RV[String] = RV("-"),
                 todoInput: ElementRef[HTMLInputElement] = ElementRef(),
                 modal: RV[Boolean] = RV(false),
                 crash: RV[Boolean] = RV(false),
                 failLoad: RV[Boolean] = RV(false),
                 quoteOn: RV[Boolean] = RV(true))

def initState() = State()

// 0.8秒後に完了する非同期の処理(Awaitのデモ)
def fetchQuote(fail: Boolean): scala.concurrent.Future[String] = {
  val p = scala.concurrent.Promise[String]()
  window.setTimeout(
    () => {
      if (fail) p.failure(new RuntimeException("読み込みに失敗しました"))
      else p.success("Scalactiveは、Scala.jsのリアクティブなフレームワークです.")
    },
    800)
  p.future
}

// 描画中に例外を起こす(エラーの境界のデモ)
def boom(): String = throw new RuntimeException("描画中にエラーが起きました")

// DOMに挿入された後に呼ばれる(ライフサイクルフック)
def mounted(s: State): Unit = s.mountedAt := new scala.scalajs.js.Date().toLocaleTimeString()

def add(s: State): Unit = {
  if (s.input.v.trim.nonEmpty) {
    s.todos.add(s.input.v.trim)
    s.input := ""
  }
}
</script>

<template>
  <div class="st-sample">
    <h3>Counter <StBadge /> <StBadge label="props" :count="Some(s.count)" :class='"big-badge" -> s.count.mapC(_ >= 3)' data-demo="fallthrough" /></h3>
    <p>mounted: {{ s.mountedAt }}</p>
    <p>count: <b>{{ s.count }}</b></p>
    <button @click="s.count := s.count.v + s.step.v">+{{ s.step }}</button>
    <button :disabled="s.count.mapC(_ <= 0)" @click="s.count := s.count.v - s.step.v">-{{ s.step }}</button>
    <label>step: <input type="number" v-model="s.step" style="width: 4em"></label>
    <p v-if="s.count.mapC(_ >= 10)" class="big">10以上です!!</p>
    <p v-else-if="s.count.mapC(_ >= 3)" class="big">3以上です!</p>
    <p v-else>3未満です.</p>
    <p :class="s.count.mapC(c => if (c % 2 == 0) &quot;even&quot; else &quot;odd&quot;)">偶奇で色が変わります</p>

    <h3>Todo</h3>
    <p>
      <input type="text" ref="s.todoInput" v-model.trim="s.input" @keydown.enter="add(s)" placeholder="Enterで追加">
      <button @click="add(s)">追加</button>
      <button @click="s.todoInput.foreach(_.focus())">入力欄へ(ref)</button>
      入力中: {{ s.input }}
    </p>
    <StList :items="s.todos">
      <template #item="(todo, i)"><b>{{ i + 1 }}.</b> {{ todo }}</template>
      <template #empty><p>(Todoがありません)</p></template>
    </StList>

    <h3>v-model</h3>
    <p><StField label="コンポーネントのv-model" v-model="s.input" /> → {{ s.input }}</p>
    <p><label><input type="checkbox" v-model="s.agree"> 同意する</label> → {{ s.agree }}</p>
    <p :class='List("note", "agreed" -> s.agree)' :style='Map("fontWeight" -> s.agree.mapC(a => if (a) "bold" else "normal"))'>
      :class / :style のオブジェクト記法(同意すると太字・緑色になります)
    </p>
    <p>
      <label><input type="radio" name="st-color" value="red" v-model="s.color"> red</label>
      <label><input type="radio" name="st-color" value="blue" v-model="s.color"> blue</label>
      <select v-model="s.color">
        <option value="red">red</option>
        <option value="blue">blue</option>
      </select>
      → <span :style="s.color.mapC(c => s&quot;color: $c&quot;)">{{ s.color }}</span>
    </p>
    <p>
      <label><input type="checkbox" value="scala" v-model="s.tags"> scala</label>
      <label><input type="checkbox" value="js" v-model="s.tags"> js</label>
      <label><input type="checkbox" value="css" v-model="s.tags"> css</label>
      → {{ s.tags.mapC(_.mkString(", ")) }}
    </p>

    <h3>&lt;template&gt; / slot</h3>
    <dl>
      <template v-for="todo in s.todos" :key="todo.v">
        <dt>{{ todo }}</dt>
        <dd>{{ todo.mapC(_.length) }}文字</dd>
      </template>
    </dl>
    <StCard v-if="s.cardOpen" title="slotの例" @close="s.cardOpen := false">
      <p>親のテンプレートで書いた内容です. count = {{ s.count }}</p>
      <template #footer><small>名前付きスロット(#footer)の内容です.</small></template>
    </StCard>
    <button v-else @click="s.cardOpen := true">カードを再表示</button>
    <StCard title="slotなし" />

    <h3>ErrorBoundary</h3>
    <ErrorBoundary @error="println(event)">
      <p>中身: <span v-if="s.crash">{{ boom() }}</span><span v-else>正常です</span></p>
      <button @click="s.crash := true">エラーを起こす</button>
      <template #fallback="err">
        <p class="st-error">{{ err.message }} <button @click="s.crash := false; err.retry()">戻す</button></p>
      </template>
    </ErrorBoundary>

    <h3>Await</h3>
    <p>
      <label><input type="checkbox" v-model="s.failLoad"> 失敗させる</label>
      <!-- v-ifを付け直してAwaitを作り直す(もう1度読み込む) -->
      <button @click="s.quoteOn := false; s.quoteOn := true">再読み込み</button>
    </p>
    <Await v-if="s.quoteOn" :load="() => fetchQuote(s.failLoad.v)">
      <template #pending><p>読み込み中...</p></template>
      <template #default="quote"><p>{{ quote }}</p></template>
      <template #error="err"><p class="st-error">{{ err.message }} <button @click="err.retry()">再試行</button></p></template>
    </Await>

    <h3>Teleport</h3>
    <button @click="s.modal := true">モーダルを開く</button>
    <Teleport v-if="s.modal" to="body">
      <div class="st-modal" @click.self="s.modal := false">
        <div class="st-modal-body">
          <p>bodyの直下に描画されています. count = {{ s.count }}</p>
          <button @click="s.modal := false">閉じる</button>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.st-sample button { margin-right: 4px; }
.big { font-weight: bold; color: tomato; }
.even { color: seagreen; }
.odd { color: royalblue; }
.agreed { color: seagreen; }
.big-badge { font-size: 16px !important; }
.st-error { color: tomato; }
.st-modal { position: fixed; inset: 0; background: rgba(0, 0, 0, 0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.st-modal-body { background: #fff; color: #222; padding: 16px; border-radius: 6px; }
</style>
