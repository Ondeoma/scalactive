<script lang="scala">
import Router.*
import io.github.ondeoma.scalactive.doc.components.contents.scalaapi.*
</script>

<template>
  <h1>Scala API(従来の書き方)</h1>

  <p>
    .stが使えるようになる前の、Scalaだけで書く方法です.<br>
    HTMLをScalaの文字列として書き、<br>
    ifC・forC・evClickなどの関数で、動きを付けます.
  </p>
  <p>
    通常は<a @click='go("/component")'>stファイル(.st)</a>で書いてください.<br>
    .stで書いたコンポーネントも、生成されるのは同じScalaのコンポーネントなので、<br>
    この書き方のコンポーネントと組み合わせて使えます.
  </p>

  <ul class="toc">
    <li><a @click='setHash("introduction")'>はじめに</a></li>
    <li><a @click='setHash("start")'>クイックスタート</a></li>
    <li><a @click='setHash("component")'>コンポーネント</a></li>
    <li><a @click='setHash("reactive")'>リアクティブ</a></li>
    <li><a @click='setHash("attr")'>属性バインディング</a></li>
    <li><a @click='setHash("cond")'>条件付きレンダリング</a></li>
    <li><a @click='setHash("list")'>リストレンダリング</a></li>
    <li><a @click='setHash("event")'>イベントハンドリング</a></li>
    <li><a @click='setHash("builtin")'>組込コンポーネント</a></li>
    <li><a @click='setHash("routing")'>ルーティング</a></li>
    <li><a @click='setHash("sfc")'>stファイルとScala API</a></li>
  </ul>

  <IntroductionScalaApi />
  <QuickStartScalaApi />
  <ComponentScalaApi />
  <ReactiveScalaApi />
  <AttrBindingScalaApi />
  <ConditionalRenderingScalaApi />
  <ListRenderingScalaApi />
  <EventScalaApi />
  <BuiltinComponentsScalaApi />
  <RoutingScalaApi />
  <SingleFileComponentScalaApi />
</template>
