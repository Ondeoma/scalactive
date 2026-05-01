package io.github.ondeoma.scalactive.syntax

object Component {

  import io.github.ondeoma.scalactive.components

  export components.BaseComponent
  export components.ComponentManager

  export components.fors.ForGeneralComponent
  export components.fors.ForLRComponent
  export components.fors.ForRMComponent
  export components.fors.ForRVComponent
  export components.fors.ForStaticComponent

  export components.ifs.IfComponent

  export components.inputs.buttons.ButtonComponent
  export components.inputs.checkboxes.CheckboxBooleanComponent
  export components.inputs.checkboxes.CheckboxGeneralComponent
  export components.inputs.checkboxes.CheckboxGroupPartComponent
  export components.inputs.files.FileInputComponent
  export components.inputs.links.LinkComponent
  export components.inputs.links.LinkStaticComponent
  export components.inputs.radios.RadioGeneralComponent
  export components.inputs.radios.RadioStringComponent
  export components.inputs.selects.SelectGeneralComponent
  export components.inputs.selects.SelectStringComponent
  export components.inputs.textareas.TextAreaComponent
  export components.inputs.texts.TextInputGeneralComponent
  export components.inputs.texts.TextInputStringComponent

  export components.texts.TextComponent
  export components.texts.Text2Component
  export components.texts.Text3Component
  export components.texts.Text4Component
  export components.texts.Text5Component
  export components.texts.Text6Component
  export components.texts.HtmlComponent
  export components.texts.Html2Component
  export components.texts.Html3Component
  export components.texts.Html4Component
  export components.texts.Html5Component
  export components.texts.Html6Component

  export components.utils.BrieflyComponent

}


