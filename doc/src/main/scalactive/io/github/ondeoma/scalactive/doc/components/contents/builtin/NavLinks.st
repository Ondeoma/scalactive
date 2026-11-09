<template>
  <nav class="nav-links">
    <RouterLink to="/">はじめに</RouterLink> /
    <RouterLink to="/builtinComponents">組込コンポーネント(このページ)</RouterLink> /
    <RouterLink to="/sfc" active-class="on">単一ファイルコンポーネント</RouterLink>
  </nav>
</template>

<style>
.nav-links .router-link-active { font-weight: bold; }
.nav-links .router-link-exact-active { color: tomato; }
.nav-links .on { text-decoration: underline; }
</style>
