# Scalactive Single File Component (.st)

Language support for single file components (`.st`) of [Scalactive](https://ondeoma.github.io/scalactive/),
a reactive frontend framework for Scala.js.

日本語の説明は [ドキュメントの「エディタ」](https://ondeoma.github.io/scalactive/editor) を参照してください.

## Syntax highlighting

- `<script lang="scala">` is highlighted as Scala, `<style>` as CSS.
- In `<template>`, the HTML is highlighted, and these parts are highlighted as Scala:
  - `{{ expr }}`
  - `v-if` / `v-else-if` / `v-show` / `v-model` / `v-for`
  - `:attr="expr"` / `v-bind:attr="expr"`
  - `@event.modifier="stmt"` / `v-on:event="stmt"`
- Tags starting with an uppercase letter (child components) are highlighted separately.
- In the script, a triple-quoted string after a `// language=scala` / `html` / `css` comment is highlighted in that language.

Scala highlighting uses the [Scala Syntax (official)](https://marketplace.visualstudio.com/items?itemName=scala-lang.scala)
extension, which is installed automatically.

## Language server

The Scala parts of `.st` files get these features from the Scalactive language server:

- Completion, including classes that are not imported yet (an import is added to the `<script>`).
  In the HTML part of the template: component names, props, events and `v-model` of child components, directives,
  slot names, and standard HTML tags, attributes and enumerated values.
- Type errors and other diagnostics, reported at their position in the `.st` file.
- Hover with types and documentation, and signature help.
- Go to definition: within the `.st` file, to `.scala` / `.st` files of the project, and into library sources.
  From a child component's tag or attribute, it goes to the `Props` of that `.st` file.
- Find references and rename (F2) of child component props.
- Inspections of child component tags (unknown props and slots, missing required props) with quick fixes.
- Inlay hints for inferred types.
- Document formatting (template indentation, `<style>` rules, and scalafmt for `<script>`).

### Requirements

1. An sbt project that enables `ScalactivePlugin`. Run `sbt compile` once so that `target/scalactive-lsp.json`
   (the classpath and other settings for the language server) is written.
   If it is missing, a notification offers to run `sbt compile`.
2. Java 11 or later. The extension looks at `scalactive.languageServer.javaHome`, `JAVA_HOME` and `PATH` in this order.

On first use, the language server downloads the Scala 3 presentation compiler for the project's Scala version
from Maven Central. Definitions in other files are read from the last sbt compilation.

### Settings

| Setting | Default | Description |
| --- | --- | --- |
| `scalactive.languageServer.enabled` | `true` | Use the language server |
| `scalactive.languageServer.javaHome` | `""` | JDK to run the language server |
| `scalactive.languageServer.jvmOptions` | `["-Xss4m", "-Xmx2G"]` | JVM options for the language server |
| `scalactive.trace.server` | `off` | Trace the communication with the language server |

## Commands

- **Scalactive: New Scalactive Component** (also in the Explorer's context menu) creates a `.st` file from a template.
- **Scalactive: Open Generated Scala Source** opens the Scala code generated from the current `.st` file.
- **Scalactive: Restart Language Server**

## Links

- [Scalactive documentation](https://ondeoma.github.io/scalactive/)
- [Source code and issues](https://github.com/Ondeoma/scalactive)
