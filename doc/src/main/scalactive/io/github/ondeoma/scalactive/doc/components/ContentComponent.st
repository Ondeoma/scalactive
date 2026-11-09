<script lang="scala">
import io.github.ondeoma.scalactive.components.routes.RouterComponent
import io.github.ondeoma.scalactive.doc.components.contents.*

val routes: PathPatternRoutes = List(
  ("/", _ => IntroductionComponent()),
  ("/start", _ => QuickStartComponent()),
  ("/component", _ => ComponentComponent()),
  ("/reactive", _ => ReactiveComponent()),
  ("/attr", _ => AttrBindingComponent()),
  ("/builtinComponents", _ => BuiltinComponentsComponent()),
  ("/conditionalRendering", _ => ConditionalRenderingComponent()),
  ("/listRendering", _ => ListRenderingComponent()),
  ("/event", _ => EventComponent()),
  ("/routing", _ => RoutingComponent()),
  ("/state", _ => StateComponent()),
  ("/editor", _ => EditorComponent()),
  ("/devtools", _ => DevtoolsComponent()),
  ("/deploy", _ => DeployComponent()),
  ("/contribute", _ => ContributeComponent()),
  ("/scalaApi", _ => ScalaApiComponent()),
)
</script>

<template>
  <main>
    <RouterComponent :routes="routes" />
  </main>
</template>
