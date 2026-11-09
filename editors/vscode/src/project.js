// sbtプラグインが書き出す target/scalactive-lsp.json(言語サーバーが使うクラスパスなど)の、拡張で使う部分.
// (言語サーバーの ProjectConfig、IntelliJプラグインの ScalactiveProjectConfig と同じ探し方をします)
const path = require("node:path");
const fs = require("node:fs");

const FILE_NAME = "scalactive-lsp.json";

function load(file) {
  try {
    const o = JSON.parse(fs.readFileSync(file, "utf8"));
    return {
      file,
      sourceRoots: (o.sourceRoots ?? []).map((r) => path.resolve(r)),
      generatedDirectory: path.resolve(o.generatedDirectory),
    };
  } catch {
    return undefined;
  }
}

const isUnder = (file, dir) => {
  const rel = path.relative(dir, file);
  return rel !== "" && !rel.startsWith("..") && !path.isAbsolute(rel);
};

/** stFileを含むソースのルート(複数あれば最も深いもの) */
function sourceRootOf(config, stFile) {
  return config.sourceRoots.filter((r) => isUnder(stFile, r)).sort((a, b) => b.length - a.length)[0];
}

/** .stの親ディレクトリを遡り、そのファイルを含むプロジェクトの target/scalactive-lsp.json を探します */
function findConfig(stFile) {
  const abs = path.resolve(stFile);
  for (let dir = path.dirname(abs); ; dir = path.dirname(dir)) {
    const f = path.join(dir, "target", FILE_NAME);
    if (fs.existsSync(f)) {
      const c = load(f);
      if (c && sourceRootOf(c, abs)) return c;
    }
    if (path.dirname(dir) === dir) return undefined;
  }
}

/** .stから生成されるScalaのソース(sbtでコンパイルしていなければ存在しません) */
function generatedSource(stFile) {
  const abs = path.resolve(stFile);
  const c = findConfig(abs);
  if (!c) return undefined;
  const rel = path.relative(sourceRootOf(c, abs), abs).replace(/\.st$/, ".scala");
  return path.join(c.generatedDirectory, rel);
}

/** stFileを含むsbtのビルドのルート(build.sbtのある、最も外側のディレクトリ) */
function sbtRoot(stFile) {
  let found;
  for (let dir = path.dirname(path.resolve(stFile)); ; dir = path.dirname(dir)) {
    if (fs.existsSync(path.join(dir, "build.sbt"))) found = dir;
    if (path.dirname(dir) === dir) return found;
  }
}

module.exports = { FILE_NAME, findConfig, generatedSource, sbtRoot };
