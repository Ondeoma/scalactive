<script lang="scala">
import io.github.ondeoma.scalactive.components.routes.RouterComponent
import Router.*

val loggedIn = RV(false)
val itemsLoaded = RV(false)

val routes = List(
  Route("/", _ => PageTitle(title = "TOP")),
  Route("/login", _ => PageTitle(title = "ログイン")),
  // ルートに入るときだけ呼ばれるガード
  Route("/items/:id", r => PageTitle(title = "商品 " + r.pathname.groups("id")),
    beforeEnter = (to, from) => if (itemsLoaded.v) Continue else Redirect("/")),
  // ガードの中で to.meta として参照できる情報
  Route("/admin", _ => PageTitle(title = "管理"), meta = Map("requiresAuth" -> true)),
)

// すべての遷移の前に呼ばれるガード
val removeGuard = Router.beforeEach { (to, from) =>
  if (to.meta.get("requiresAuth").contains(true) && !loggedIn.v) Redirect("/login")
  else Continue
}

// 遷移の後に呼ばれる
Router.afterEach((to, from) => document.title = "MyApp " + to.path)
</script>

<template>
  <main>
    <RouterComponent.of :routes="routes" />
  </main>
</template>
