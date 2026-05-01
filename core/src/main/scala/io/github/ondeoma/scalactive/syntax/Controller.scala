package io.github.ondeoma.scalactive.syntax

object Controller {

  import io.github.ondeoma.scalactive.controllers

  export controllers.ComponentController
  export controllers.HtmlElementsComponentController
  export controllers.HtmlElementsController
  export controllers.NodesComponentController
  export controllers.NodesController
  export controllers.TextComponentController

  export controllers.events.EventController

  export controllers.attrs.AttrBooleanController
  export controllers.attrs.AttrController
  export controllers.attrs.AttrStringController
  export controllers.attrs.ClassBooleanController
  export controllers.attrs.ClassBrieflyController
  export controllers.attrs.ClassController
  export controllers.attrs.ClassStringController
  export controllers.attrs.ClassStringsController
  export controllers.attrs.ClassToggleController
  export controllers.attrs.StyleBooleanController
  export controllers.attrs.StyleBrieflyController
  export controllers.attrs.StyleController
  export controllers.attrs.StyleStringController
  export controllers.attrs.StyleToggleController

}


