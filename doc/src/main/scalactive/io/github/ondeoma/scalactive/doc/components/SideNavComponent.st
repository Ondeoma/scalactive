<script lang="scala">
import Router.*
</script>

<template>
  <nav class="side-nav">
    <ul class="links">
      <li @click='go("/")'>はじめに</li>
      <li @click='go("/start")'>クイックスタート</li>
      <li @click='go("/component")'>コンポーネント(.st)</li>
      <li @click='go("/reactive")'>リアクティビティ</li>
      <li @click='go("/attr")'>属性バインディング</li>
      <li @click='go("/conditionalRendering")'>条件付きレンダリング</li>
      <li @click='go("/listRendering")'>リストレンダリング</li>
      <li @click='go("/event")'>イベントハンドリング</li>
      <li @click='go("/builtinComponents")'>組込コンポーネント</li>
      <li @click='go("/routing")'>ルーティング</li>
      <li @click='go("/state")'>状態管理</li>
      <li @click='go("/editor")'>エディタ</li>
      <li @click='go("/devtools")'>テスト・デバッグ</li>
      <li @click='go("/deploy")'>本番ビルド</li>
      <li @click='go("/contribute")'>コントリビュート</li>
      <li @click='go("/scalaApi")'>Scala API(低水準API)</li>
    </ul>
  </nav>
</template>
