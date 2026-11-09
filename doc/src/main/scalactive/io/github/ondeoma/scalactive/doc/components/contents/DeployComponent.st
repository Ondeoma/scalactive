<script lang="scala">
import Router.*
</script>

<template>
  <h1>本番ビルド</h1>

  <p>
    Scalactiveを使うことによる特別なデプロイ手順の変化はありません.<br>
    <a href="https://www.scala-js.org/doc/tutorial/scalajs-vite.html#production-build" target="_blank">Scala.js本家</a>
    のビルド手順等を参照ください.
  </p>

  <p>
    ただし<a @click='go("/routing")'>Routing</a>機能を利用している場合は、<br>
    HTTPサーバー等によるパスのフォールバック設定を実施してください.<br>
    ※詳細は各HTTPサーバー等のドキュメントを参照ください.
  </p>
</template>
