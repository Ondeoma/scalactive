<script lang="scala">
case class State(now: RV[String] = RV(""), var timer: Int = 0)

def initState() = State()

def tick(s: State): Unit = s.now := new scala.scalajs.js.Date().toLocaleTimeString()

// DOMに挿入された後に呼ばれる
def mounted(s: State): Unit = {
  tick(s)
  s.timer = window.setInterval(() => tick(s), 1000)
}

// 片付けられた(DOMから取り除かれた)後に呼ばれる
def unmounted(s: State): Unit = window.clearInterval(s.timer)
</script>

<template>
  <p>現在時刻: {{ s.now }}</p>
</template>
