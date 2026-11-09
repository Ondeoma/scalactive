<script lang="scala">
case class State(loggedIn: RV[Boolean] = RV(false))

def initState() = State()
</script>

<template>
  <button @click="s.loggedIn := !s.loggedIn.v">ログイン状態を切り替え</button>
  <template v-if="s.loggedIn">
    <p><b>ようこそ!</b></p>
    <p>この2つの要素がまとめて表示されます.</p>
  </template>
  <p v-else>ログインしていません.</p>
</template>
