<script lang="scala">
case class State(open: RV[Boolean] = RV(false),
                 editing: RV[Boolean] = RV(false),
                 name: RV[String] = RV("Scalactive"),
                 color: RV[String] = RV("tomato"))

def initState() = State()

// 独自のディレクティブ: 値の色で文字を塗る(値が変わると updated が呼ばれる)
val tint: Directive[String] = Directive((el, c) => el.style.color = c)
</script>

<template>
  <div class="directive-demo">
    <div class="menu-wrap" v-click-outside="s.open := false">
      <button @click="s.open := !s.open.v">メニュー</button>
      <ul v-if="s.open" class="menu">
        <li>項目1</li>
        <li>項目2(外を押すと閉じます)</li>
      </ul>
    </div>
    <p v-if="!s.editing.v">名前: {{ s.name }} <button @click="s.editing := true">編集</button></p>
    <p v-else>
      名前: <input v-model="s.name" v-focus.select @keydown.enter="s.editing := false"> <button @click="s.editing := false">完了</button>
    </p>
    <p>
      <select v-model="s.color">
        <option value="tomato">tomato</option>
        <option value="seagreen">seagreen</option>
        <option value="royalblue">royalblue</option>
      </select>
      <span v-tint="s.color">v-tint で色を付けた文字</span>
    </p>
  </div>
</template>

<style scoped>
.menu-wrap { position: relative; display: inline-block; }
.menu { position: absolute; top: 100%; left: 0; margin: 0; padding: 4px 12px; list-style: none; background: var(--bg, #fff); border: 1px solid #ccc; border-radius: 4px; z-index: 1; white-space: nowrap; }
</style>
