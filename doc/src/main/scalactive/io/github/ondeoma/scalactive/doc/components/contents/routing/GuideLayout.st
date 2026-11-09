<script lang="scala">
import io.github.ondeoma.scalactive.components.routes.RouterComponent
import Router.*

// /guide より下のパス(/guide からの相対パスで書く)
val routes: PathPatternRoutes = List(
  ("intro", _ => PageTitle(title = "はじめに")),             // /guide/intro
  ("api/:name", r => PageTitle(title = r.pathname.groups("name"))), // /guide/api/xxx
)
</script>

<template>
  <nav>
    <RouterLink to="/guide/intro">はじめに</RouterLink>
    <RouterLink to="/guide/api/router">API</RouterLink>
  </nav>
  <section>
    <RouterComponent :routes="routes" :base='fixUrl("/guide")' />
  </section>
</template>
