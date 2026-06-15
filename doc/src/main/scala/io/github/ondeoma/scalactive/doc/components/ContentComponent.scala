package io.github.ondeoma.scalactive.doc.components

import io.github.ondeoma.scalactive.components.*
import io.github.ondeoma.scalactive.components.routes.RouterComponent
import io.github.ondeoma.scalactive.doc.components.contents.*

object ContentComponent extends StatelessComponent {

  def genHtml(implicit cm: ComponentManager): HTML = {

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
      ("/deploy", _ => DeployComponent()),
      ("/contribute", _ => ContributeComponent()),
    )

    // language=html
    s"""<main>
       |    ${%(RouterComponent(routes))}
       |</main>
       |""".stripMargin
  }

}
