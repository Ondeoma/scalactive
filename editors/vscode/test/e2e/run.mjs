// VS Codeを起動し、拡張から言語サーバーを動かす結合テスト.
//
//   (リポジトリ直下で) sbt languageServer/assembly doc/compile
//   npm run test:e2e
//
// 環境変数 VSCODE_PATH でVS Codeの実行ファイルを指定できます(未指定ならmacOSの既定の場所、なければ安定版をダウンロード).
import { downloadAndUnzipVSCode, resolveCliArgsFromVSCodeExecutablePath, runTests } from "@vscode/test-electron";
import { spawnSync } from "node:child_process";
import { existsSync, mkdirSync } from "node:fs";
import { fileURLToPath } from "node:url";

const root = fileURLToPath(new URL("../../", import.meta.url));
const macApp = "/Applications/Visual Studio Code.app/Contents/MacOS/Code";
// 最近のVS CodeはmacOSの実行ファイル名が Electron から Code に変わっているため、存在する方を使う
const fixMac = (p) => (existsSync(p) ? p : p.replace(/\/Electron$/, "/Code"));
const vscodeExecutablePath = fixMac(
  process.env.VSCODE_PATH || (existsSync(macApp) ? macApp : await downloadAndUnzipVSCode("stable")),
);

// 依存する拡張(Scala Syntax)だけを入れた拡張ディレクトリを使う
const extensionsDir = `${root}.vscode-test/extensions`;
const userDataDir = `${root}.vscode-test/user-data`;
mkdirSync(extensionsDir, { recursive: true });
const [cli, ...cliArgs] = resolveCliArgsFromVSCodeExecutablePath(vscodeExecutablePath);
const install = spawnSync(cli, [...cliArgs, "--extensions-dir", extensionsDir, "--install-extension", "scala-lang.scala"], {
  encoding: "utf8",
  stdio: "inherit",
  shell: process.platform === "win32",
});
if (install.status !== 0) process.exit(install.status ?? 1);

try {
  await runTests({
    vscodeExecutablePath,
    extensionDevelopmentPath: root,
    extensionTestsPath: fileURLToPath(new URL("./suite.cjs", import.meta.url)),
    launchArgs: [
      fileURLToPath(new URL("../../../../doc", import.meta.url)),
      "--extensions-dir",
      extensionsDir,
      "--user-data-dir",
      userDataDir,
      "--disable-workspace-trust",
    ],
  });
} catch (e) {
  console.error(e);
  process.exit(1);
}
