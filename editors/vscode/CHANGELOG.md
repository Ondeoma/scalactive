# Change Log

## 0.6.1

The version now matches the Scalactive library (0.6.1).

- Component tags starting with an uppercase letter (`<Link>`, `<Input>`, ...) are no longer treated as HTML void elements,
  so they can have children and closing tags.
- A `.st` component named like a built-in one (`RouterLink`, `Route`, ...) is used instead of the built-in one in the same package.
- The generated code imports only what it uses, so `-Wunused:imports` no longer warns about it.

## 0.6.0

The version now matches the Scalactive library (0.6.0). This version needs Scalactive 0.6.0 or later;
a warning is shown when the project uses an older library, since errors in `.st` files may be wrong.

- The language server compiles templates the same way as the library: expressions that read reactive values (`.v`)
  in `{{ }}`, bindings, `v-if`, `v-for` and child component props are tracked.
  Props of type `Reactive` accept value expressions such as `:count="s.n.v * 2"` without false type errors.

## 0.4.4

The version now matches the Scalactive library (0.4.4), which compiles `<style lang="scss">` to CSS.

- `<style lang="scss">` is highlighted as SCSS and `<style lang="sass">` as Sass
  (the indented syntax needs a Sass extension, since VS Code has no built-in grammar for it).
- The language server reads CSS classes in SCSS and Sass styles too (nested rules, `&-suffix`, `//` comments),
  for class completion, go to definition and unused-class hints.

## 0.4.2

- `.st` files get their own icon in the Explorer and editor tabs (used when the file icon theme has no icon for them).

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
