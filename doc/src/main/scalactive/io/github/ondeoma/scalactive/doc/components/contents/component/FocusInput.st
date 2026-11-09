<script lang="scala">
case class State(text: RV[String] = RV("Scalactive"),
                 input: ElementRef[HTMLInputElement] = ElementRef())

def initState() = State()
</script>

<template>
  <div>
    <input type="text" ref="s.input" v-model="s.text">
    <button @click="s.input.foreach(_.focus())">入力欄へ</button>
    <button @click="s.input.foreach(_.select())">全選択</button>
  </div>
</template>
