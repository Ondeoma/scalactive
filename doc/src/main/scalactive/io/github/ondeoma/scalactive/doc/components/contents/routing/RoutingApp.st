<script lang="scala">
import io.github.ondeoma.scalactive.components.routes.RouterComponent
import Router.*

// パスと、描画するコンポーネントの組のリスト
val routes: PathPatternRoutes = List(
  ("/", _ => PageTitle(title = "TOP")),
  ("/items/:id", r => PageTitle(title = "商品 " + r.pathname.groups("id"))),
  ("/guide/*", _ => GuideLayout()),
)
</script>

<template>
  <nav>
    <RouterLink to="/">TOP</RouterLink>
    <RouterLink to="/guide/intro">ガイド</RouterLink>
    <button @click='go("/items/1")'>商品1へ</button>
  </nav>
  <main>
    <RouterComponent :routes="routes" />
  </main>
</template>
