// .st文法のトークン化テスト.
// shiki(VS Codeと同じTextMateエンジン)で、ScalaとCSSの文法を埋め込んだ状態でトークン化し、
// 指定した文字列のトークンに期待するスコープが付くことを確認します.
//
//   npm install && npm test
import { createHighlighter } from "shiki";
import { readFileSync, readdirSync } from "node:fs";
import assert from "node:assert/strict";

const grammar = JSON.parse(readFileSync(new URL("../syntaxes/scalactive.tmLanguage.json", import.meta.url), "utf8"));

const highlighter = await createHighlighter({
  themes: ["github-dark"],
  langs: ["scala", "css", { ...grammar, name: "scalactive", embeddedLangs: ["scala", "css"] }],
});

/** 行ごとのトークン列([{content, scopes}]) */
function tokenize(code) {
  return highlighter
    .codeToTokens(code, { lang: "scalactive", theme: "github-dark", includeExplanation: true })
    .tokens.map((line) =>
      line.flatMap((t) => t.explanation.map((e) => ({ content: e.content, scopes: e.scopes.map((s) => s.scopeName) }))),
    );
}

let failures = 0;

function test(name, f) {
  try {
    f();
    console.log(`  + ${name}`);
  } catch (e) {
    failures++;
    console.log(`  x ${name}\n${e.message}`);
  }
}

/**
 * lineIndex行目で、textに一致する(前後の空白を除く. なければtextを含む)n番目のトークンのスコープに、
 * scopesがすべて含まれることを確認します.
 */
function find(tokens, lineIndex, text) {
  const exact = tokens[lineIndex].filter((t) => t.content.trim() === text);
  return exact.length > 0 ? exact : tokens[lineIndex].filter((t) => t.content.includes(text));
}

function expectScopes(tokens, lineIndex, text, scopes, n = 0) {
  const found = find(tokens, lineIndex, text);
  assert.ok(found.length > n, `token '${text}' not found on line ${lineIndex}: ${JSON.stringify(tokens[lineIndex].map((t) => t.content))}`);
  const actual = found[n].scopes;
  for (const s of scopes) {
    assert.ok(actual.some((a) => a === s || a.startsWith(s + ".")), `'${text}' on line ${lineIndex}: expected scope ${s} in\n    ${actual.join("\n    ")}`);
  }
}

function expectNotScope(tokens, lineIndex, text, scope) {
  const t = find(tokens, lineIndex, text)[0];
  assert.ok(t, `token '${text}' not found on line ${lineIndex}`);
  assert.ok(!t.scopes.some((a) => a.startsWith(scope)), `'${text}' should not have ${scope}: ${t.scopes.join(", ")}`);
}

const sample = `<script lang="scala">
case class State(count: RV[Int] = RV(0))
def initState() = State()
</script>

<template>
  <div class="box" :title="s.count.mapC(_.toString)">
    <p v-if="s.count.mapC(_ >= 3)">{{ s.count }}</p>
    <p v-else-if="ok">b</p>
    <p v-else>c &amp; d</p>
    <li v-for="(todo, i) in s.todos">{{ i + 1 }}</li>
    <button @click.prevent="s.count := s.count.v + 1" v-on:keydown.enter.ctrl="go()">+</button>
    <input v-model.trim="s.name">
    <template v-for="x in xs"><dt>{{ x }}</dt></template>
    <MyCard title="t" :count="Some(s.count)" @close="open := false">
      <slot>fallback</slot>
    </MyCard>
    <textarea>{{ not interpolated }}</textarea>
    <!-- {{ comment }} -->
    <p :class="if (a) &quot;x&quot; else &quot;y&quot;">e</p>
  </div>
</template>

<style scoped>
.box { color: red; }
</style>
`;

const tokens = tokenize(sample);

console.log("scalactive grammar");

test("設定ファイルが厳密なJSONとして読める(IntelliJは拡張JSONを受け付けない)", () => {
  for (const f of ["../package.json", "../language-configuration.json", "../syntaxes/scalactive.tmLanguage.json"]) {
    JSON.parse(readFileSync(new URL(f, import.meta.url), "utf8"));
  }
});

test("<script>の中身はScala", () => {
  expectScopes(tokens, 0, "script", ["entity.name.tag.script.html"]);
  expectScopes(tokens, 1, "case", ["source.scalactive", "meta.embedded.block.scala", "keyword"]);
  expectScopes(tokens, 2, "def", ["meta.embedded.block.scala", "keyword"]);
  expectScopes(tokens, 3, "script", ["entity.name.tag.script.html"]);
});

test("<style>の中身はCSS", () => {
  expectScopes(tokens, 24, "color", ["meta.embedded.block.css", "support.type.property-name.css"]);
});

test("HTMLタグと通常の属性", () => {
  expectScopes(tokens, 6, "div", ["entity.name.tag.html"]);
  expectScopes(tokens, 6, "class", ["entity.other.attribute-name.html"]);
  expectScopes(tokens, 6, "box", ["string.quoted.html"]);
});

test(":attr の値はScala", () => {
  expectScopes(tokens, 6, ":", ["punctuation.attribute-shorthand.bind.scalactive"]);
  expectScopes(tokens, 6, "title", ["entity.other.attribute-name.scalactive"]);
  expectScopes(tokens, 6, "mapC", ["meta.embedded.inline.scala"]);
});

test("v-if / v-else-if / v-else", () => {
  expectScopes(tokens, 7, "v-if", ["entity.other.attribute-name.directive.scalactive"]);
  expectScopes(tokens, 7, ">=", ["meta.embedded.inline.scala", "keyword.operator"]);
  expectScopes(tokens, 8, "v-else-if", ["entity.other.attribute-name.directive.scalactive"]);
  expectScopes(tokens, 9, "v-else", ["entity.other.attribute-name.directive.scalactive"]);
  expectScopes(tokens, 9, "amp", ["constant.character.entity.html"]);
});

test("{{ }} の中身はScala", () => {
  expectScopes(tokens, 7, "{{", ["punctuation.definition.interpolation.begin.scalactive"]);
  expectScopes(tokens, 7, "s.count", ["meta.interpolation.scalactive", "meta.embedded.inline.scala"]);
  expectScopes(tokens, 7, "}}", ["punctuation.definition.interpolation.end.scalactive"]);
  expectScopes(tokens, 10, "1", ["meta.embedded.inline.scala", "constant.numeric"], 0);
});

test("v-for の変数と in", () => {
  expectScopes(tokens, 10, "v-for", ["entity.other.attribute-name.directive.scalactive"]);
  expectScopes(tokens, 10, "todo", ["variable.parameter.scalactive"]);
  expectScopes(tokens, 10, "i", ["variable.parameter.scalactive"]);
  expectScopes(tokens, 10, "in", ["keyword.control.in.scalactive"]);
  expectScopes(tokens, 10, "todos", ["meta.embedded.inline.scala"]);
  expectScopes(tokens, 13, "x", ["variable.parameter.scalactive"]);
});

test("@event と修飾子", () => {
  expectScopes(tokens, 11, "@", ["punctuation.attribute-shorthand.event.scalactive"]);
  expectScopes(tokens, 11, "click", ["entity.other.attribute-name.event.scalactive"]);
  expectScopes(tokens, 11, ".prevent", ["entity.other.attribute-name.modifier.scalactive"]);
  expectScopes(tokens, 11, ":=", ["meta.embedded.inline.scala"]);
  expectScopes(tokens, 11, "v-on:", ["punctuation.attribute-shorthand.event.scalactive"]);
  expectScopes(tokens, 11, ".enter.ctrl", ["entity.other.attribute-name.modifier.scalactive"]);
});

test("v-model と修飾子", () => {
  expectScopes(tokens, 12, "v-model", ["entity.other.attribute-name.directive.scalactive"]);
  expectScopes(tokens, 12, ".trim", ["entity.other.attribute-name.modifier.scalactive"]);
  expectScopes(tokens, 12, "name", ["meta.embedded.inline.scala"]);
});

test("入れ子の<template>で外側のテンプレートが閉じない", () => {
  expectScopes(tokens, 13, "dt", ["entity.name.tag.html"]);
  expectScopes(tokens, 14, "MyCard", ["meta.template.scalactive"]);
  expectScopes(tokens, 20, "div", ["meta.template.scalactive", "entity.name.tag.html"]);
});

test("子コンポーネントのタグ", () => {
  expectScopes(tokens, 14, "MyCard", ["support.class.component.scalactive"]);
  expectScopes(tokens, 14, "Some", ["meta.embedded.inline.scala"]);
  expectScopes(tokens, 14, "close", ["entity.other.attribute-name.event.scalactive"]);
  expectScopes(tokens, 15, "slot", ["entity.name.tag.html"]);
  expectScopes(tokens, 16, "MyCard", ["support.class.component.scalactive"]);
});

test("textareaとコメントの中は展開しない", () => {
  expectNotScope(tokens, 17, "{{ not interpolated }}", "meta.interpolation");
  expectScopes(tokens, 17, "{{ not interpolated }}", ["string.unquoted.raw-text.html"]);
  expectScopes(tokens, 18, "{{ comment }}", ["comment.block.html"]);
});

test("属性値中のエンティティ", () => {
  expectScopes(tokens, 19, "quot", ["constant.character.entity.html"]);
  expectScopes(tokens, 19, "if", ["meta.embedded.inline.scala", "keyword"]);
});

test("最上位の</template>でテンプレートが閉じる", () => {
  expectScopes(tokens, 21, "template", ["entity.name.tag.template.html"]);
  expectNotScope(tokens, 23, "<", "meta.template");
});

// リポジトリ内の実際の.stファイルで、Scalaの文字列がタグを飲み込む(閉じ引用符の取りこぼし)ことがないか確認する
const stDir = new URL("../../../doc/src/main/scalactive/io/github/ondeoma/scalactive/doc/components/contents/st/", import.meta.url);
for (const file of readdirSync(stDir).filter((f) => f.endsWith(".st"))) {
  test(`doc/${file} でScalaの文字列がテンプレートに漏れない`, () => {
    const lines = tokenize(readFileSync(new URL(file, stDir), "utf8"));
    lines.forEach((line, i) => {
      for (const t of line) {
        const leaked = t.scopes.some((s) => s.startsWith("meta.template")) &&
          t.scopes.some((s) => s.startsWith("string.quoted") && s.endsWith(".scala")) &&
          /[<>]/.test(t.content);
        assert.ok(!leaked, `line ${i + 1}: '${t.content}' is inside a Scala string: ${t.scopes.join(", ")}`);
      }
    });
    const last = lines.filter((l) => l.length > 0).at(-1);
    assert.ok(!last.some((t) => t.scopes.some((s) => s.startsWith("meta.template"))), "template block is not closed");
  });
}

// ---- 追加の構文 ----

test("v-html", () => {
  const t = tokenize(`<template>\n  <div v-html="s.body"></div>\n</template>\n`);
  expectScopes(t, 1, "v-html", ["entity.other.attribute-name.directive.scalactive"]);
  expectScopes(t, 1, "body", ["meta.embedded.inline.scala"]);
});

test("コンポーネントのv-model / v-model:name", () => {
  const t = tokenize(`<template>\n  <Field v-model="s.name" v-model:first-name="s.first" />\n</template>\n`);
  expectScopes(t, 1, "v-model", ["entity.other.attribute-name.directive.scalactive"]);
  expectScopes(t, 1, "v-model:first-name", ["entity.other.attribute-name.directive.scalactive"]);
  expectScopes(t, 1, "first", ["meta.embedded.inline.scala"]);
});

test("名前付きスロット #name / v-slot:name", () => {
  const t = tokenize(`<template>\n  <Card><template #header>h</template><template v-slot:footer>f</template></Card>\n</template>\n`);
  expectScopes(t, 1, "#", ["punctuation.attribute-shorthand.slot.scalactive"]);
  expectScopes(t, 1, "header", ["entity.other.attribute-name.directive.scalactive"]);
  expectScopes(t, 1, "v-slot:", ["punctuation.attribute-shorthand.slot.scalactive"]);
  expectScopes(t, 1, "footer", ["entity.other.attribute-name.directive.scalactive"]);
});

test("テンプレート参照 ref", () => {
  const t = tokenize(`<template>\n  <input ref="s.box">\n</template>\n`);
  expectScopes(t, 1, "ref", ["entity.other.attribute-name.directive.scalactive"]);
  expectScopes(t, 1, "box", ["meta.embedded.inline.scala"]);
});

if (failures > 0) {
  console.log(`${failures} test(s) failed`);
  process.exit(1);
}
console.log("all passed");
