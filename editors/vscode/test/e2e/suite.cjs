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

  // テンプレートの補完: タグ名に同じパッケージのコンポーネント
  const tagCompletion = await vscode.commands.executeCommand(
    "vscode.executeCompletionItemProvider",
    doc.uri,
    positionOf(doc, "<StCard v-if", "<St".length),
    "<",
  );
  const tagLabels = tagCompletion.items.map((i) => (typeof i.label === "string" ? i.label : i.label.label));
  assert.ok(tagLabels.includes("StCard") && tagLabels.includes("StBadge"), tagLabels.join(", "));
  console.log(`  + template completion: ${tagLabels.filter((l) => l.startsWith("St")).join(", ")}`);

  // 子コンポーネントのタグ名から、その.stへの定義ジャンプ
  const tagDefs = await vscode.commands.executeCommand(
    "vscode.executeDefinitionProvider",
    doc.uri,
    positionOf(doc, "<StCard v-if", 2),
  );
  const tagDef = tagDefs[0].uri ? tagDefs[0] : { uri: tagDefs[0].targetUri };
  assert.equal(path.basename(tagDef.uri.fsPath), "StCard.st");
  console.log(`  + component definition: ${path.basename(tagDef.uri.fsPath)}`);

  // 子コンポーネントのタグのホバー: Propsの一覧
  const tagHover = hoverText(
    await vscode.commands.executeCommand("vscode.executeHoverProvider", doc.uri, positionOf(doc, "<StCard v-if", 2)),
  );
  assert.ok(tagHover.includes("StCard.st") && tagHover.includes("title: String"), tagHover);
  console.log(`  + component hover: ${tagHover.replace(/\s+/g, " ").slice(0, 80)}...`);

  // propの参照: 定義(StCard.st)と、StSample.stの2つのタグの属性
  const titlePos = positionOf(doc, 'title="slotの例"', 1);
  const refs = await vscode.commands.executeCommand("vscode.executeReferenceProvider", doc.uri, titlePos);
  const refFiles = refs.map((r) => path.basename(r.uri.fsPath));
  assert.equal(refFiles.filter((f) => f === "StSample.st").length, 2, refFiles.join(", "));
  assert.ok(refFiles.includes("StCard.st"), refFiles.join(", "));
  console.log(`  + references: ${refFiles.join(", ")}`);

  // propの名前の変更(適用はしない)
  const edit = await vscode.commands.executeCommand("vscode.executeDocumentRenameProvider", doc.uri, titlePos, "heading");
  const renamedFiles = edit.entries().map(([u, es]) => `${path.basename(u.fsPath)}:${es.length}`);
  assert.ok(renamedFiles.some((f) => f.startsWith("StSample.st:2")), renamedFiles.join(", "));
  assert.ok(renamedFiles.some((f) => f.startsWith("StCard.st:")), renamedFiles.join(", "));
  console.log(`  + rename: ${renamedFiles.join(", ")}`);

  // 整形: 整形済みのファイルは変わらない. インデントを崩すと元に戻す
  const noEdits = await vscode.commands.executeCommand("vscode.executeFormatDocumentProvider", doc.uri, { tabSize: 2, insertSpaces: true });
  assert.equal((noEdits ?? []).length, 0);

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

  // propsの検査: 未知のpropを、その属性の位置で指摘する(型検査が同じpropについて報告していれば、そちらだけ)
  const tagStart = doc.getText().indexOf('<StCard title="slotなし"') + "<StCard ".length;
  await editor.edit((b) => b.insert(doc.positionAt(tagStart), 'titel="x" '));
  const unknownProp = await poll("unknown prop", async () =>
    vscode.languages.getDiagnostics(doc.uri).find((d) => d.message.includes("titel")),
  );
  assert.equal(doc.offsetAt(unknownProp.range.start), tagStart);
  console.log(`  + props check: ${unknownProp.message.split("\n")[0]}`);
  await vscode.commands.executeCommand("workbench.action.files.revert");

  // 整形: インデントを崩した行を元に戻す
  const original = doc.getText();
  const line = positionOf(doc, "<p>count: <b>").line;
  await editor.edit((b) => b.replace(new vscode.Range(line, 0, line, doc.lineAt(line).firstNonWhitespaceCharacterIndex), ""));
  assert.notEqual(doc.getText(), original);
  await vscode.commands.executeCommand("editor.action.formatDocument");
  await poll("formatting", async () => doc.getText() === original);
  console.log("  + formatting: restored the indentation");
  await vscode.commands.executeCommand("workbench.action.files.revert");

  // コンポーネントの抽出: 選んだ範囲を新しい.stにし、元の場所をタグに置き換える
  {
    const fs = require("node:fs");
    const extracted = path.join(path.dirname(sample), "Extracted.st");
    const start = doc.getText().indexOf("<p>mounted:");
    const end = doc.getText().indexOf("</p>", start) + "</p>".length;
    const range = new vscode.Range(doc.positionAt(start), doc.positionAt(end));
    try {
      const actions = await poll("extract action", async () => {
        const as = await vscode.commands.executeCommand("vscode.executeCodeActionProvider", doc.uri, range, "refactor.extract");
        return as.find((a) => a.title === "Extract component");
      });
      await vscode.commands.executeCommand(actions.command.command, ...actions.command.arguments);
      await poll("extracted", async () => fs.existsSync(extracted) && doc.getText().includes('<Extracted :s="s" />'));
      const content = fs.readFileSync(extracted, "utf8");
      assert.ok(content.includes("case class Props(s: StSample.State)"), content);
      assert.ok(content.includes("<p>mounted: {{ props.s.mountedAt }}</p>"), content);
      // 新しいコンポーネントは型検査を通る(docはsbtでコンパイル済み)
      const created = await vscode.workspace.openTextDocument(extracted);
      await sleep(3000);
      const errors = await poll("extracted diagnostics", async () => {
        const ds = vscode.languages.getDiagnostics(created.uri);
        return ds.length >= 0 ? ds.filter((d) => d.severity === vscode.DiagnosticSeverity.Error) : undefined;
      });
      assert.deepEqual(errors.map((d) => d.message), []);
      console.log("  + extract component: Extracted.st (props: s)");
    } finally {
      if (fs.existsSync(extracted)) fs.unlinkSync(extracted);
      await vscode.window.showTextDocument(doc);
      await vscode.commands.executeCommand("workbench.action.files.revert");
    }
  }

  // 生成コードを開く
  const generated = await vscode.commands.executeCommand("scalactive.openGeneratedSource", doc.uri);
  assert.ok(generated && generated.endsWith(path.join("st", "StSample.scala")), String(generated));
  assert.ok(vscode.window.activeTextEditor.document.uri.fsPath.endsWith("StSample.scala"));
  console.log(`  + open generated source: ${path.basename(generated)}`);

  // 新規作成のひな形
  const os = require("node:os");
  const fs = require("node:fs");
  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), "scalactive-"));
  const created = await vscode.commands.executeCommand("scalactive.newComponent", vscode.Uri.file(tmp), "UserCard", "PropsComponent.st");
  assert.equal(created, path.join(tmp, "UserCard.st"));
  assert.ok(fs.readFileSync(created, "utf8").includes("case class Props(title: String"));
  assert.equal(vscode.window.activeTextEditor.document.languageId, "scalactive");
  console.log("  + new component: UserCard.st");

  // scalactive-lsp.json の探し方
  const api = vscode.extensions.all.find((e) => e.packageJSON.name === require("../../package.json").name).exports;
  assert.ok(api.project.findConfig(sample));
  assert.equal(api.project.findConfig(created), undefined);
  console.log("  + project config: found for the doc site, not for a file outside sbt projects");

  // stripMarginの文字列の | の行で改行すると、同じ位置に | を補う
  const marginDoc = await vscode.workspace.openTextDocument({
    language: "scalactive",
    content: '<script lang="scala">\nval code =\n  """|<div>\n     |  <p>x</p>\n     |""".stripMargin\n</script>\n',
  });
  const marginEditor = await vscode.window.showTextDocument(marginDoc);
  const lineEnd = marginDoc.lineAt(3).range.end;
  marginEditor.selection = new vscode.Selection(lineEnd, lineEnd);
  await vscode.commands.executeCommand("type", { text: "\n" });
  // | の後ろのインデントも引き継ぐ
  await poll("margin", () => marginDoc.lineAt(4).text === "     |  " || undefined, 10_000);
  assert.equal(marginEditor.selection.active.character, "     |  ".length);
  // 開きの """| の行
  const openEnd = marginDoc.lineAt(2).range.end;
  marginEditor.selection = new vscode.Selection(openEnd, openEnd);
  await vscode.commands.executeCommand("type", { text: "\n" });
  await poll("margin on the opening line", () => marginDoc.lineAt(3).text === "     |" || undefined, 10_000);
  // 文字列の外では補わない
  const outEnd = marginDoc.lineAt(1).range.end;
  marginEditor.selection = new vscode.Selection(outEnd, outEnd);
  await vscode.commands.executeCommand("type", { text: "\n" });
  await new Promise((r) => setTimeout(r, 500));
  assert.ok(!marginDoc.lineAt(2).text.includes("|"), marginDoc.lineAt(2).text);
  console.log("  + enter in a stripMargin string adds |");
  await vscode.commands.executeCommand("workbench.action.revertAndCloseActiveEditor");
  await vscode.commands.executeCommand("workbench.action.closeAllEditors");
}

module.exports = { run };
