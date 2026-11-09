<script lang="scala">
case class State(box: ElementRef[HTMLElement] = ElementRef())

def initState() = State()

// 外部のライブラリが、自分で作った要素の中への描画を任せてくる想定です
def mounted(s: State): Unit = {
  s.box.foreach { box =>
    val node = document.createElement("div") // ライブラリが作った要素
    box.appendChild(node)
    // このコンポーネントが片付けられると、mountしたCounterも片付けられます
    mount(node) { Counter() }
  }
}
</script>

<template>
  <div ref="s.box" class="external"></div>
</template>

<style scoped>
.external {
  border: 1px dashed #999;
  padding: 4px 8px;
}
</style>
