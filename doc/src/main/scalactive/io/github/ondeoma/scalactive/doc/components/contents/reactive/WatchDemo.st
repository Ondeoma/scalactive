<script lang="scala">
import io.github.ondeoma.scalactive.models.WatchInfo

// コンポーネントの外のリアクティブな値(全てのインスタンスで共有されます)
val shared = RV(0)

case class State(count: RV[Int] = RV(0),
                 log: RV[String] = RV("-"),
                 sharedLog: RV[String] = RV("-"),
                 var sharedWatch: Option[WatchInfo] = None)

def initState() = {
  val s = State()
  // 自分のStateの値の監視(Stateと一緒に片付くので、解除は不要です)
  s.count.addWatcher { case (oldV, newV) => s.log := s"count: $oldV -> $newV" }
  s
}

// 外の値の監視は、mountedで始めて、unmountedで解除します
def mounted(s: State): Unit = {
  s.sharedWatch = Some(shared.addWatcher { case (oldV, newV) => s.sharedLog := s"shared: $oldV -> $newV" })
}

def unmounted(s: State): Unit = s.sharedWatch.foreach(_.abort())
</script>

<template>
  <button @click="s.count := s.count.v + 1">count +1</button>
  <button @click="shared := shared.v + 1">shared +1</button>
  <p>{{ s.log }}</p>
  <p>{{ s.sharedLog }}</p>
</template>
