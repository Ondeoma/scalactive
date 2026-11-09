<script lang="scala">
// コンポーネントの外のリアクティブな値(全てのインスタンスで共有されます)
val shared = RV(0)

case class State(count: RV[Int] = RV(0),
                 log: RV[String] = RV("-"),
                 sharedLog: RV[String] = RV("-"))

def initState() = State()

// mountedの中のwatchは、コンポーネントが片付けられると自動で解除されます
def mounted(s: State): Unit = {
  watch(s.count)((oldV, newV) => s.log := s"count: $oldV -> $newV")
  watch(shared, immediate = true)((_, newV) => s.sharedLog := s"shared: $newV")
}
</script>

<template>
  <button @click="s.count := s.count.v + 1">count +1</button>
  <button @click="shared := shared.v + 1">shared +1</button>
  <p>{{ s.log }}</p>
  <p>{{ s.sharedLog }}</p>
</template>
