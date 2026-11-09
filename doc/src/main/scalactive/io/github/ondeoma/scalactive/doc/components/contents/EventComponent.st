<script lang="scala">
val code = // language=scala
  s"""// EvnetTypeに標準的なイベント定数が定義されています.
     |import io.github.ondeoma.scalactive.enums.EventType.*
     |import org.scalajs.dom.window
     |
     |s\"\"\"<button $${ev(click, ev => alert("CLICK!"))}>
     |    :  CLICK ME!(ev)
     |    :</button>
     |    :<button $${evClick(alert("CLICK!"))}>
     |    :  CLICK ME!(evClick)
     |    :</button>
     |    :<span style="background: mediumpurple;" 
     |    :      $${ev0(mouseover, alert("MOUSEOVER!"))}>
     |    :  MOUSEOVER ME!
     |    :</span>\"\"\".stripMagin(':')
     |""".stripMargin
</script>

<template>
  <h1>イベントハンドリング</h1>

  <p>
    要素で発生したイベント(クリック等)を検知して処理を行う方法です.
  </p>

  <h2 class="ev">ev / ev0 / evClick</h2>

  <p>
    イベントハンドラー関数に
    <a href="https://github.com/scala-js/scala-js-dom/blob/main/dom/src/main/scala/org/scalajs/dom/Event.scala" target="_blank">Event</a>
    が必要な場合はev関数を利用して下さい.<br>
    不要な場合はev0関数を利用すると記述がシンプルになります.<br>
    またClick専用のevClick関数も用意しています.
  </p>

  <pre><code class="language-scala">{{ code }}</code></pre>

  <div class="rendered">
    <button @click='window.alert("CLICK!")'>
      CLICK ME!(ev)
    </button>
    <button @click='window.alert("CLICK!")'>
      CLICK ME!(evClick)
    </button>
    <span style="background: mediumpurple;" @mouseover='window.alert("MOUSEOVER!")'>
      MOUSEOVER ME!
    </span>
  </div>
</template>
