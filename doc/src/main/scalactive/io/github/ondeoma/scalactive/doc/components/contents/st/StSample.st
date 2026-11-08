<script lang="scala">
case class State(count: RV[Int] = RV(0),
                 todos: RVList[String] = RVList(List("Scala.js", "Scalactive")),
                 input: RV[String] = RV(""),
                 step: RV[Int] = RV(1),
                 agree: RV[Boolean] = RV(false),
                 color: RV[String] = RV("red"),
                 tags: RV[List[String]] = RV(List("scala")),
                 cardOpen: RV[Boolean] = RV(true),
                 mountedAt: RV[String] = RV("-"))

def initState() = State()

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
    <h3>Counter <StBadge /> <StBadge label="props" :count="Some(s.count)" /></h3>
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
      <input type="text" v-model.trim="s.input" @keydown.enter="add(s)" placeholder="Enterで追加">
      <button @click="add(s)">追加</button>
      入力中: {{ s.input }}
    </p>
    <ul>
      <li v-for="(todo, i) in s.todos">{{ i + 1 }}. {{ todo }}</li>
    </ul>

    <h3>v-model</h3>
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
    </StCard>
    <button v-else @click="s.cardOpen := true">カードを再表示</button>
    <StCard title="slotなし" />
  </div>
</template>

<style scoped>
.st-sample button { margin-right: 4px; }
.big { font-weight: bold; color: tomato; }
.even { color: seagreen; }
.odd { color: royalblue; }
.agreed { color: seagreen; }
</style>
