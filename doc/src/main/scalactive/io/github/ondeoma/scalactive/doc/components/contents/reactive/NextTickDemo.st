<script lang="scala">
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

case class State(items: RVList[String] = RVList(List("りんご")),
                 list: ElementRef[HTMLElement] = ElementRef())

def initState() = State()

def add(s: State): Unit = {
  s.items.add("")
  // 描き直しが終わってから、追加した行の入力欄にフォーカスします
  nextTick().foreach { _ =>
    s.list.foreach { ul =>
      val inputs = ul.querySelectorAll("input")
      inputs(inputs.length - 1).asInstanceOf[HTMLInputElement].focus()
    }
  }
}
</script>

<template>
  <div>
    <ul ref="s.list">
      <li v-for="item in s.items"><input type="text" :value="item"></li>
    </ul>
    <button @click="add(s)">行を追加</button>
  </div>
</template>
