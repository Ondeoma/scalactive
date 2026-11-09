package io.github.ondeoma.scalactive.doc.components.contents.singlefile

/**
 * 単一ファイルコンポーネントのページで表示する、.stのソースのサンプル.
 * .stの<script>に書くと、文字列の中の </script> や case class Props(...)・def initState() などを
 * .stのコンパイラーがそのコンポーネント自身の定義として読んでしまうため、Scalaのソースに分けています.
 */
object SampleCodes {

  val codeSt = // language=html
    s"""<!-- src/main/scalactive/app/Counter.st -->
       |<script lang="scala">
       |case class State(count: RV[Int] = RV(0))
       |
       |def initState() = State()
       |</script>
       |
       |<template>
       |  <div class="counter">
       |    <p>count: {{ s.count }}</p>
       |    <button @click="s.count := s.count.v + 1">+1</button>
       |    <p v-if="s.count.mapC(_ >= 3)">3以上です!</p>
       |    <p v-else>3未満です.</p>
       |  </div>
       |</template>
       |
       |<style scoped>
       |.counter { padding: 8px; }
       |</style>
       |""".stripMargin

  val codeProps = // language=html
    s"""<!-- src/main/scalactive/app/Badge.st -->
       |<script lang="scala">
       |case class Props(label: String,
       |                 count: Reactive[Int] = RV(0),
       |                 onReset: Unit => Unit = _ => ())
       |</script>
       |
       |<template>
       |  <span>{{ props.label }}: {{ props.count }}</span>
       |  <button @click="props.onReset(())">reset</button>
       |</template>
       |
       |<!-- 親(.st)からの利用 -->
       |<Badge label="count" :count="s.count" @reset="s.count := 0" />
       |""".stripMargin

  val codeComponentModel = // language=html
    s"""<!-- src/main/scalactive/app/Field.st -->
       |<script lang="scala">
       |case class Props(label: String, model: RV[String], note: RV[String] = RV(""))
       |</script>
       |
       |<template>
       |  <label>{{ props.label }}: <input v-model="props.model"></label>
       |  <input v-model="props.note">
       |</template>
       |
       |<!-- 親(.st)からの利用 -->
       |<Field label="名前" v-model="s.name" v-model:note="s.memo" />
       |""".stripMargin

  val codeSlot = // language=html
    s"""<!-- src/main/scalactive/app/Card.st -->
       |<script lang="scala">
       |case class Props(title: String,
       |                 slot: Slot = Slot.empty,
       |                 footer: Slot = Slot.empty)
       |</script>
       |
       |<template>
       |  <div class="card">
       |    <h4>{{ props.title }}</h4>
       |    <slot><p>(内容がありません)</p></slot>
       |    <footer><slot name="footer">(footerなし)</slot></footer>
       |  </div>
       |</template>
       |
       |<!-- 親(.st)からの利用. <template #footer>の中身はfooterに、それ以外の子要素はslotに展開されます -->
       |<Card title="お知らせ">
       |  <p>count = {{ s.count }}</p>
       |  <template #footer><small>更新: 今日</small></template>
       |</Card>
       |""".stripMargin

  val codeScopedSlot = // language=html
    s"""<!-- src/main/scalactive/app/TodoList.st -->
       |<script lang="scala">
       |case class Todo(id: Int, title: String)
       |case class Props(todos: List[Todo],
       |                 item: ScopedSlot[Todo] = ScopedSlot.empty)
       |</script>
       |
       |<template>
       |  <ul>
       |    <!-- :valueで値を渡す. 親が内容を渡さなければ中身(既定の内容)を表示 -->
       |    <li v-for="t in props.todos" :key="t.id"><slot name="item" :value="t">{{ t.title }}</slot></li>
       |  </ul>
       |</template>
       |
       |<!-- 親(.st)からの利用. #item="t" で子から受け取った値を使う -->
       |<TodoList :todos="s.todos">
       |  <template #item="t"><b>{{ t.id }}</b> {{ t.title }}</template>
       |</TodoList>
       |""".stripMargin

  val codeLifecycle = // language=html
    s"""<script lang="scala">
       |case class State(input: RV[String] = RV(""),
       |                 search: ElementRef[HTMLInputElement] = ElementRef())
       |
       |def initState() = State()
       |
       |// DOMに挿入された後に実行される(引数はなし、またはProps・Stateを受け取れる)
       |def mounted(s: State): Unit = s.search.foreach(_.focus())
       |
       |// 片付けられた(DOMから取り除かれた)後に実行される
       |def unmounted(): Unit = println("unmounted")
       |</script>
       |
       |<template>
       |  <input ref="s.search" v-model="s.input">
       |</template>
       |""".stripMargin

}
