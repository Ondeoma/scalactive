// sbtで作った言語サーバーのjarを拡張に同梱するためにコピーします.
// 事前にリポジトリ直下で `sbt languageServer/assembly` を実行してください.
import { copyFileSync, existsSync, mkdirSync } from "node:fs";

const src = new URL("../../../language-server/target/scala-2.12/scalactive-language-server.jar", import.meta.url);
const dest = new URL("../server/scalactive-language-server.jar", import.meta.url);
if (!existsSync(src)) {
  console.error(`${src.pathname} not found. Run 'sbt languageServer/assembly' at the repository root first.`);
  process.exit(1);
}
mkdirSync(new URL("../server/", import.meta.url), { recursive: true });
copyFileSync(src, dest);
console.log(`copied ${dest.pathname}`);
