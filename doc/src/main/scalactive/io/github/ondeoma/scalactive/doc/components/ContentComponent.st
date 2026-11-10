<script lang="scala">
import io.github.ondeoma.scalactive.components.routes.RouterComponent
import io.github.ondeoma.scalactive.doc.components.contents.*

// 画面のタイトルは、サイドナビと同じ名前にする(ScalactiveConfig.titleTemplate で「 | Scalactive Doc」を付ける)
val routes = List(
  Route("/", _ => IntroductionComponent()),
  Route("/start", _ => QuickStartComponent(), title = _ => "クイックスタート"),
  Route("/component", _ => ComponentComponent(), title = _ => "コンポーネント(.st)"),
  Route("/reactive", _ => ReactiveComponent(), title = _ => "リアクティビティ"),
  Route("/attr", _ => AttrBindingComponent(), title = _ => "属性バインディング"),
  Route("/builtinComponents", _ => BuiltinComponentsComponent(), title = _ => "組込コンポーネント"),
  Route("/conditionalRendering", _ => ConditionalRenderingComponent(), title = _ => "条件付きレンダリング"),
  Route("/listRendering", _ => ListRenderingComponent(), title = _ => "リストレンダリング"),
  Route("/event", _ => EventComponent(), title = _ => "イベントハンドリング"),
  Route("/routing", _ => RoutingComponent(), title = _ => "ルーティング"),
  Route("/state", _ => StateComponent(), title = _ => "状態管理"),
  Route("/editor", _ => EditorComponent(), title = _ => "エディタ"),
  Route("/devtools", _ => DevtoolsComponent(), title = _ => "テスト・デバッグ"),
  Route("/deploy", _ => DeployComponent(), title = _ => "本番ビルド"),
  Route("/contribute", _ => ContributeComponent(), title = _ => "コントリビュート"),
  Route("/scalaApi", _ => ScalaApiComponent(), title = _ => "Scala API(低水準API)"),
)
</script>

<template>
  <main>
    <RouterComponent.of :routes="routes" />
  </main>
</template>
