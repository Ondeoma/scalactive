# Change Log

## 0.4.1

First release on the Visual Studio Marketplace and Open VSX.

- Completion in the HTML part of the template: component names, props, events and `v-model` of child components,
  directives, slot names, and standard HTML tags, attributes and enumerated values.
- Go to definition from a child component's tag or attribute to the `Props` of that `.st` file,
  and into library sources (downloaded source jars).
- Auto-import of classes on completion and a quick fix for "Not found" errors.
- Find references and rename of props, inspections of child component tags with quick fixes, and inlay hints.
- Document formatting (template indentation, `<style>` rules and scalafmt for `<script>`).
- Commands: New Scalactive Component, Open Generated Scala Source and Restart Language Server.
- `|` is inserted on Enter inside `stripMargin` strings.

## 0.1.0

- Syntax highlighting for `.st` (Scalactive single file component) files.
- Completion, type errors, hover, signature help and go to definition by the language server.
