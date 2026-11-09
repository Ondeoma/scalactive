// Scalactive言語サーバー(補完・型エラー・ホバー・定義ジャンプ)を起動するVS Code拡張.
// 言語サーバーはJavaで動くjar(server/scalactive-language-server.jar)で、拡張に同梱しています.
const vscode = require("vscode");
const path = require("node:path");
const fs = require("node:fs");
const { LanguageClient, State } = require("vscode-languageclient/node");
const project = require("./project");

/** 新規作成のひな形(templates/*.st. IntelliJプラグインも同じものを使います) */
const TEMPLATES = [
  { label: "Component", file: "Component.st" },
  { label: "Component with Props", file: "PropsComponent.st" },
  { label: "Stateful component", file: "StatefulComponent.st" },
];

/** @type {LanguageClient | undefined} */
let client;

/** 設定、JAVA_HOME、PATHの順にjavaコマンドを決めます. */
function javaCommand(config) {
  const exe = process.platform === "win32" ? "java.exe" : "java";
  const homes = [config.get("javaHome"), process.env.JAVA_HOME].filter((h) => h && h.trim() !== "");
  for (const home of homes) {
    const candidate = path.join(home, "bin", exe);
    if (fs.existsSync(candidate)) return candidate;
  }
  return exe;
}

async function start(context) {
  const config = vscode.workspace.getConfiguration("scalactive.languageServer");
  if (!config.get("enabled", true)) return;

  const jar = context.asAbsolutePath(path.join("server", "scalactive-language-server.jar"));
  if (!fs.existsSync(jar)) {
    vscode.window.showErrorMessage(`Scalactive: language server not found: ${jar}`);
    return;
  }
  const serverOptions = {
    command: javaCommand(config),
    args: [...config.get("jvmOptions", []), "-jar", jar],
  };
  const clientOptions = {
    documentSelector: [{ scheme: "file", language: "scalactive" }],
    outputChannelName: "Scalactive Language Server",
  };
  client = new LanguageClient("scalactive", "Scalactive Language Server", serverOptions, clientOptions);
  try {
    await client.start();
  } catch (e) {
    vscode.window.showErrorMessage(
      `Scalactive: failed to start the language server (Java 11 or later is required; see 'scalactive.languageServer.javaHome'): ${e}`,
    );
  }
}

async function stop() {
  if (client && client.state !== State.Stopped) await client.stop();
  client = undefined;
}

/**
 * New Scalactive Component: ひな形から.stを作ります. ファイル名がコンポーネント(object)の名前になるため、大文字で始まる名前にします.
 * 引数(フォルダのURI、名前、ひな形のファイル名)を渡すと、問い合わせずに作ります(テスト用).
 */
async function newComponent(context, folderUri, name, templateFile) {
  let folder = folderUri?.fsPath;
  if (folder && fs.existsSync(folder) && fs.statSync(folder).isFile()) folder = path.dirname(folder);
  if (!folder) {
    const active = vscode.window.activeTextEditor?.document.uri;
    folder = active?.scheme === "file" ? path.dirname(active.fsPath) : vscode.workspace.workspaceFolders?.[0]?.uri.fsPath;
  }
  if (!folder) return;
  if (!templateFile) {
    const pick = await vscode.window.showQuickPick(TEMPLATES, { placeHolder: "Scalactive component template" });
    if (!pick) return;
    templateFile = pick.file;
  }
  if (!name) {
    name = await vscode.window.showInputBox({
      prompt: "Component name (file name)",
      placeHolder: "UserCard",
      validateInput: (v) =>
        /^[A-Z][A-Za-z0-9_]*(\.st)?$/.test(v.trim()) ? undefined : "大文字で始まる英数字にしてください(例: UserCard)",
    });
    if (!name) return;
  }
  const file = path.join(folder, name.trim().replace(/\.st$/, "") + ".st");
  if (fs.existsSync(file)) {
    vscode.window.showErrorMessage(`Scalactive: ${file} already exists`);
    return;
  }
  fs.writeFileSync(file, fs.readFileSync(context.asAbsolutePath(path.join("templates", templateFile)), "utf8"));
  await vscode.window.showTextDocument(vscode.Uri.file(file));
  return file;
}

/** Open Generated Scala Source: .stから生成されたScalaのソースを開きます */
async function openGeneratedSource(uri) {
  const file = (uri ?? vscode.window.activeTextEditor?.document.uri)?.fsPath;
  if (!file || !file.endsWith(".st")) return;
  const generated = project.generatedSource(file);
  if (!generated || !fs.existsSync(generated)) {
    vscode.window.showWarningMessage(
      `Scalactive: 生成コードが見つかりません. sbtでコンパイルしてください${generated ? ` (${generated})` : ""}`,
    );
    return;
  }
  await vscode.window.showTextDocument(vscode.Uri.file(generated), { preview: true });
  return generated;
}

const notified = new Set();

/** .stを開いたときに target/scalactive-lsp.json がなければ、sbtのビルドごとに1回だけ案内します */
async function checkConfig(context, doc) {
  if (doc.languageId !== "scalactive" || doc.uri.scheme !== "file") return;
  if (project.findConfig(doc.uri.fsPath)) return;
  const root = project.sbtRoot(doc.uri.fsPath) ?? path.dirname(doc.uri.fsPath);
  if (notified.has(root)) return;
  notified.add(root);
  const run = "sbt compile を実行";
  const answer = await vscode.window.showWarningMessage(
    `Scalactive: target/${project.FILE_NAME} がありません. sbtでコンパイルするまで、.stのScalaの補完・型エラーは使えません.`,
    ...(project.sbtRoot(doc.uri.fsPath) ? [run] : []),
  );
  if (answer === run) await runSbtCompile(context, root);
}

/** sbt compile をタスクとして実行し、成功したら言語サーバーを再起動します */
async function runSbtCompile(context, root) {
  const task = new vscode.Task(
    { type: "shell", task: "scalactive-sbt-compile" },
    vscode.TaskScope.Workspace,
    "sbt compile",
    "scalactive",
    new vscode.ShellExecution("sbt -batch compile", { cwd: root }),
  );
  const execution = await vscode.tasks.executeTask(task);
  const done = vscode.tasks.onDidEndTaskProcess(async (e) => {
    if (e.execution !== execution) return;
    done.dispose();
    if (e.exitCode === 0) {
      notified.delete(root);
      await stop();
      await start(context);
      vscode.window.showInformationMessage("Scalactive: sbt compile が終わりました. 言語サーバーを再起動しました.");
    } else {
      vscode.window.showErrorMessage(`Scalactive: sbt compile が失敗しました(終了コード ${e.exitCode}).`);
    }
  });
}

/** @param {vscode.ExtensionContext} context */
async function activate(context) {
  context.subscriptions.push(
    vscode.commands.registerCommand("scalactive.newComponent", (uri, name, template) => newComponent(context, uri, name, template)),
    vscode.commands.registerCommand("scalactive.openGeneratedSource", (uri) => openGeneratedSource(uri)),
    vscode.workspace.onDidOpenTextDocument((doc) => checkConfig(context, doc)),
    vscode.commands.registerCommand("scalactive.restartLanguageServer", async () => {
      await stop();
      await start(context);
    }),
    vscode.workspace.onDidChangeConfiguration(async (e) => {
      if (e.affectsConfiguration("scalactive.languageServer")) {
        await stop();
        await start(context);
      }
    }),
  );
  await start(context);
  vscode.workspace.textDocuments.forEach((doc) => checkConfig(context, doc));
  // 結合テストから言語サーバーの状態を確認するため
  return { client: () => client, project };
}

function deactivate() {
  return stop();
}

module.exports = { activate, deactivate };
