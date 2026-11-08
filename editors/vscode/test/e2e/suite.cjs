// VS Codeの拡張ホスト内で実行されるテスト. VS Codeの各種プロバイダを通して言語サーバーの応答を確認します.
const vscode = require("vscode");
const assert = require("node:assert/strict");
const path = require("node:path");

const sample = path.resolve(
  __dirname,
  "../../../../doc/src/main/scalactive/io/github/ondeoma/scalactive/doc/components/contents/st/StSample.st",
);

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

/** fが値を返すまで繰り返します(言語サーバーの起動・presentation compilerの初回読み込みを待つため). */
async function poll(name, f, timeoutMs = 180_000) {
  const end = Date.now() + timeoutMs;
  let last;
  while (Date.now() < end) {
    try {
      const v = await f();
      if (v) return v;
    } catch (e) {
      last = e;
    }
    await sleep(1000);
  }
  throw new Error(`timed out: ${name} ${last ?? ""}`);
}

function positionOf(doc, text, delta = 0) {
  const i = doc.getText().indexOf(text);
  assert.ok(i >= 0, `'${text}' not found`);
  return doc.positionAt(i + delta);
}

const hoverText = (hs) =>
  hs.flatMap((h) => h.contents.map((c) => (typeof c === "string" ? c : c.value))).join("\n");

async function run() {
  const doc = await vscode.workspace.openTextDocument(sample);
  await vscode.window.showTextDocument(doc);
  assert.equal(doc.languageId, "scalactive");

  const countPos = positionOf(doc, "{{ s.count }}", "{{ s.c".length);

  const hover = await poll("hover", async () => {
    const hs = await vscode.commands.executeCommand("vscode.executeHoverProvider", doc.uri, countPos);
    const t = hoverText(hs);
    return t.includes("RV[Int]") ? t : undefined;
  });
  console.log(`  + hover: ${hover.replace(/\s+/g, " ")}`);

  const completion = await vscode.commands.executeCommand(
    "vscode.executeCompletionItemProvider",
    doc.uri,
    positionOf(doc, "{{ s.count }}", "{{ s.".length),
    ".",
  );
  const labels = completion.items.map((i) => (typeof i.label === "string" ? i.label : i.label.label));
  assert.ok(labels.some((l) => l.startsWith("count")), labels.join(", "));
  console.log(`  + completion: ${labels.slice(0, 5).join(", ")}, ...`);

  const defs = await vscode.commands.executeCommand("vscode.executeDefinitionProvider", doc.uri, countPos);
  assert.equal(defs.length, 1);
  const def = defs[0].uri ? defs[0] : { uri: defs[0].targetUri, range: defs[0].targetRange };
  assert.equal(def.uri.fsPath, doc.uri.fsPath);
  assert.ok(doc.lineAt(def.range.start.line).text.includes("count: RV[Int]"), doc.lineAt(def.range.start.line).text);
  console.log(`  + definition: line ${def.range.start.line + 1}`);

  // 正しいファイルにはエラーがない
  const initial = await poll("initial diagnostics", async () => {
    await sleep(2000);
    return { ds: vscode.languages.getDiagnostics(doc.uri) };
  });
  const initialErrors = initial.ds.filter((d) => d.severity === vscode.DiagnosticSeverity.Error);
  assert.deepEqual(initialErrors.map((d) => d.message), []);

  // 型エラーを入れると、その位置に診断が出る
  const editor = vscode.window.activeTextEditor;
  const start = doc.getText().indexOf("{{ s.count }}") + "{{ s.count".length;
  await editor.edit((b) => b.insert(doc.positionAt(start), ".nothing"));
  const error = await poll("type error", async () =>
    vscode.languages
      .getDiagnostics(doc.uri)
      .find((d) => d.severity === vscode.DiagnosticSeverity.Error && d.message.includes("nothing")),
  );
  assert.equal(error.range.start.line, doc.positionAt(start).line);
  console.log(`  + diagnostics: line ${error.range.start.line + 1}: ${error.message.split("\n")[0]}`);
  await vscode.commands.executeCommand("workbench.action.files.revert");
}

module.exports = { run };
