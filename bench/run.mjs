// Scalactiveの性能計測を実行します.
// 'sbt bench/fullLinkJS'の出力をローカルのHTTPサーバーで配信し、ヘッドレスChromeで各シナリオを実行して、
// 結果を表示し bench/results/ にJSONで保存します. 使い方は bench/README.md を参照してください.
import { chromium } from "playwright-core";
import { execFileSync } from "node:child_process";
import fs from "node:fs";
import http from "node:http";
import path from "node:path";
import { parseArgs } from "node:util";

const here = path.dirname(new URL(import.meta.url).pathname);

const { values: args } = parseArgs({
  options: {
    impl: { type: "string", default: "scala,st" },
    scenarios: { type: "string" },
    iterations: { type: "string" },
    warmup: { type: "string", default: "3" },
    delay: { type: "string" },
    "cpu-throttle": { type: "string", default: "1" },
    out: { type: "string" },
    compare: { type: "string" },
    fast: { type: "boolean", default: false },
    headed: { type: "boolean", default: false },
    chrome: { type: "string" },
    help: { type: "boolean", short: "h", default: false },
  },
});

if (args.help) {
  console.log(`usage: node run.mjs [options]
  --impl scala,st        表の実装(既定: 両方)
  --scenarios a,b        実行するシナリオ(既定: すべて)
  --iterations N         計測回数(既定: シナリオごとの値)
  --warmup N             計測前の空回し(既定: 3)
  --delay ms             ScalactiveConfig.reloadTimerLazyDuration(既定: ライブラリの既定値)
  --cpu-throttle N       CPUをN倍遅くする(既定: 1)
  --out file.json        結果の保存先(既定: results/<日時>-<コミット>.json)
  --compare base.json    前の結果と中央値を比較する
  --fast                 fullLinkJSではなくfastLinkJSの出力を使う
  --headed               ブラウザを表示する
  --chrome path          Chromeの実行ファイル(既定: インストール済みのGoogle Chrome)`);
  process.exit(0);
}

// ---- 配信 ----

function linkDir() {
  const target = path.join(here, "target");
  const scalaDir = fs.readdirSync(target).filter((d) => d.startsWith("scala-3")).sort().pop();
  const dir = path.join(target, scalaDir ?? "", args.fast ? "scalactive-bench-fastopt" : "scalactive-bench-opt");
  if (!scalaDir || !fs.existsSync(path.join(dir, "main.js"))) {
    console.error(`${dir}/main.js がありません. 先に 'sbt bench/${args.fast ? "fastLinkJS" : "fullLinkJS"}' を実行してください.`);
    process.exit(1);
  }
  return dir;
}

function serve(dir) {
  const types = { ".html": "text/html", ".js": "text/javascript", ".map": "application/json" };
  const server = http.createServer((req, res) => {
    const url = new URL(req.url, "http://localhost");
    const file = url.pathname === "/" ? path.join(here, "index.html") : path.join(dir, path.normalize(url.pathname));
    if (!file.startsWith(dir) && file !== path.join(here, "index.html")) {
      res.writeHead(403).end();
      return;
    }
    fs.readFile(file, (err, data) => {
      if (err) res.writeHead(404).end();
      else res.writeHead(200, { "content-type": types[path.extname(file)] ?? "application/octet-stream" }).end(data);
    });
  });
  return new Promise((resolve) => server.listen(0, "127.0.0.1", () => resolve(server)));
}

// ---- 集計 ----

function stats(times) {
  const sorted = [...times].sort((a, b) => a - b);
  const mean = times.reduce((a, b) => a + b, 0) / times.length;
  const mid = Math.floor(sorted.length / 2);
  const median = sorted.length % 2 ? sorted[mid] : (sorted[mid - 1] + sorted[mid]) / 2;
  const sd = Math.sqrt(times.reduce((a, t) => a + (t - mean) ** 2, 0) / times.length);
  return { median, mean, min: sorted[0], max: sorted[sorted.length - 1], sd };
}

const fmt = (n) => (n >= 100 ? n.toFixed(0) : n >= 10 ? n.toFixed(1) : n.toFixed(2));
const mb = (bytes) => (bytes / 1024 / 1024).toFixed(1);

function gitInfo() {
  try {
    const commit = execFileSync("git", ["rev-parse", "--short", "HEAD"], { cwd: here }).toString().trim();
    const dirty = execFileSync("git", ["status", "--porcelain"], { cwd: here }).toString().trim() !== "";
    return { commit, dirty };
  } catch {
    return { commit: "unknown", dirty: false };
  }
}

function table(rows) {
  const widths = rows[0].map((_, i) => Math.max(...rows.map((r) => String(r[i]).length)));
  return rows
    .map((r) => r.map((c, i) => (i === 0 || i === 1 ? String(c).padEnd(widths[i]) : String(c).padStart(widths[i]))).join("  "))
    .join("\n");
}

// ---- 実行 ----

async function main() {
  const dir = linkDir();
  const server = await serve(dir);
  const base = `http://127.0.0.1:${server.address().port}/`;
  const browser = await chromium.launch({
    headless: !args.headed,
    ...(args.chrome ? { executablePath: args.chrome } : { channel: "chrome" }),
    args: ["--js-flags=--expose-gc"],
  });
  const version = browser.version();
  const impls = args.impl.split(",").map((s) => s.trim()).filter(Boolean);
  const results = [];
  let effectiveDelay = null;
  const warmup = Number(args.warmup);
  const throttle = Number(args["cpu-throttle"]);

  try {
    for (const impl of impls) {
      const url = `${base}?impl=${impl}${args.delay !== undefined ? `&delay=${args.delay}` : ""}`;
      const listPage = await browser.newPage();
      await listPage.goto(url);
      await listPage.waitForFunction(() => window.scalactiveBench);
      let scenarios = await listPage.evaluate(() => window.scalactiveBench.scenarios());
      effectiveDelay = await listPage.evaluate(() => window.scalactiveBench.delay);
      await listPage.close();
      if (args.scenarios) {
        const wanted = args.scenarios.split(",").map((s) => s.trim());
        const unknown = wanted.filter((w) => !scenarios.some((s) => s.name === w));
        if (unknown.length) throw new Error(`unknown scenarios: ${unknown.join(", ")}`);
        scenarios = scenarios.filter((s) => wanted.includes(s.name));
      }

      for (const s of scenarios) {
        // シナリオごとに新しいページで実行し、前のシナリオの影響を受けないようにする
        const page = await browser.newPage();
        const cdp = await page.context().newCDPSession(page);
        await page.goto(url);
        await page.waitForFunction(() => window.scalactiveBench);
        if (throttle > 1) await cdp.send("Emulation.setCPUThrottlingRate", { rate: throttle });
        const baseline = await page.evaluate(() => window.scalactiveBench.metrics());
        await cdp.send("HeapProfiler.collectGarbage");
        const heapBefore = (await cdp.send("Runtime.getHeapUsage")).usedSize;
        const iterations = args.iterations ? Number(args.iterations) : s.iterations;
        process.stderr.write(`${impl} ${s.name} (${warmup}+${iterations}) ... `);
        const r = await page.evaluate(([n, w, i]) => window.scalactiveBench.run(n, w, i), [s.name, warmup, iterations]);
        await cdp.send("HeapProfiler.collectGarbage");
        const heapAfter = (await cdp.send("Runtime.getHeapUsage")).usedSize;
        const st = stats(r.times);
        process.stderr.write(`${fmt(st.median)} ms\n`);
        results.push({
          impl,
          name: s.name,
          description: s.description,
          kind: s.kind,
          times: r.times,
          ...st,
          after: r.after,
          baseline,
          afterReset: r.afterReset,
          heapBefore,
          heapAfter,
        });
        await page.close();
      }
    }
  } finally {
    await browser.close();
    server.close();
  }

  const meta = {
    date: new Date().toISOString(),
    ...gitInfo(),
    chrome: version,
    delay: effectiveDelay,
    cpuThrottle: throttle,
    warmup,
    build: args.fast ? "fastLinkJS" : "fullLinkJS",
  };

  // 表示
  console.log(
    `\nScalactive bench  ${meta.date}  commit ${meta.commit}${meta.dirty ? "(未コミットの変更あり)" : ""}  Chrome ${version}  ` +
      `delay ${meta.delay}ms  CPU x${throttle}  ${meta.build}\n`,
  );
  const compare = args.compare ? JSON.parse(fs.readFileSync(args.compare, "utf8")) : null;
  const header = ["impl", "scenario", "median", "mean", "min", "max", "sd", "heap+MB", "nodes", "leak"];
  if (compare) header.push("base", "diff");
  const rows = [header];
  for (const r of results) {
    // 表を空にした後も、ページを開いた直後より多く残っているDOMノード/監視は解除漏れの疑い
    const leaks = [];
    for (const k of ["domNodes", "selectedWatchers", "rowsWatchers"]) {
      const d = r.afterReset[k] - r.baseline[k];
      if (d > 0) leaks.push(`${k}+${d}`);
    }
    const row = [
      r.impl,
      r.name,
      fmt(r.median),
      fmt(r.mean),
      fmt(r.min),
      fmt(r.max),
      fmt(r.sd),
      mb(r.heapAfter - r.heapBefore),
      r.after.domNodes,
      leaks.join(" ") || "-",
    ];
    if (compare) {
      const b = compare.results.find((x) => x.impl === r.impl && x.name === r.name);
      if (b) {
        const diff = ((r.median - b.median) / b.median) * 100;
        row.push(fmt(b.median), `${diff >= 0 ? "+" : ""}${diff.toFixed(1)}%`);
      } else row.push("-", "-");
    }
    rows.push(row);
  }
  console.log(table(rows));
  console.log("\n時間はms(中央値など). heap+MB: シナリオ前後(GC後)のJSヒープの増分. nodes: 計測直後の#app以下のDOMノード数.");
  console.log("leak: 表を空にした後も、ページを開いた直後より多く残っているDOMノード/監視の数.");

  const out = args.out ?? path.join(here, "results", `${meta.date.replace(/[:.]/g, "-")}-${meta.commit}.json`);
  fs.mkdirSync(path.dirname(out), { recursive: true });
  fs.writeFileSync(out, JSON.stringify({ meta, results }, null, 2));
  console.log(`\n保存しました: ${path.relative(process.cwd(), out)}`);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
