// Scalactive言語サーバー(補完・型エラー・ホバー・定義ジャンプ)を起動するVS Code拡張.
// 言語サーバーはJavaで動くjar(server/scalactive-language-server.jar)で、拡張に同梱しています.
const vscode = require("vscode");
const path = require("node:path");
const fs = require("node:fs");
const { LanguageClient, State } = require("vscode-languageclient/node");

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

/** @param {vscode.ExtensionContext} context */
async function activate(context) {
  context.subscriptions.push(
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
  // 結合テストから言語サーバーの状態を確認するため
  return { client: () => client };
}

function deactivate() {
  return stop();
}

module.exports = { activate, deactivate };
