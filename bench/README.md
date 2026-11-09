# bench: 性能計測

Scalactiveの描画とリアクティブ値の性能を、ヘッドレスのChromeで計測します.
ライブラリを変更する前後で実行し、結果を比較して使います.

## 実行方法

```sh
sbt bench/fullLinkJS          # 計測用アプリをビルド(リポジトリ直下で)
cd bench && npm install       # 初回のみ(playwright-core)
node run.mjs                  # すべてのシナリオを計測(2分ほど)
```

インストール済みのGoogle Chromeを使います(playwright-coreはブラウザをダウンロードしません).
別のChromeを使う場合は `--chrome <実行ファイル>` を指定します.

結果は表で表示され、`bench/results/<日時>-<コミット>.json` に保存されます(gitの管理外).
変更前の結果と比べるには:

```sh
node run.mjs --out results/before.json   # 変更前
# ライブラリを変更して sbt bench/fullLinkJS
node run.mjs --compare results/before.json
```

主なオプション(`node run.mjs --help`):

| オプション | 内容 |
| --- | --- |
| `--impl scala,st` | 表の実装. `scala` はScala API(`RowsScala.scala`)、`st` は.st(`RowsSt.st`)、`lr` はRVListの代わりにReactiveListを使う版(`RowsLR.scala`). 既定は `scala,st` |
| `--scenarios a,b` | 実行するシナリオ |
| `--iterations N` / `--warmup N` | 計測回数(既定: シナリオごと、多くは10) / 計測前の空回し(既定: 3) |
| `--delay ms` | `ScalactiveConfig.reloadTimerLazyDuration`. 指定しなければライブラリの既定値(0: 現在の処理の直後に描き直す)のまま |
| `--cpu-throttle N` | CPUをN倍遅くする(遅い端末の想定) |
| `--fast` | fullLinkJSではなくfastLinkJSの出力を使う |
| `--profile dir` | シナリオごとのCPUプロファイル(`.cpuprofile`)をdirに保存する。`--fast`と合わせると関数名が読める。Chrome DevToolsのPerformanceパネルで開ける |

## 計測の内容

1,000行(10,000行)の表を `RVList` + `forC`(.stでは `v-for`)で描き、各行は
id、ラベル(行の値を監視するテキスト)、選択状態のclass(`Store.selected` を監視)、クリックイベントを持ちます.
行のデータは `Store`(コンポーネントの外のオブジェクト)にあります.

| シナリオ | 内容 |
| --- | --- |
| create-1k / create-10k | 空の表に1,000 / 10,000行を作る |
| replace-1k | 1,000行をすべて別の行に置き換える |
| update-10th | 10行に1行のラベルを書き換える |
| select | 1行を選択する |
| swap | 2行を入れ替える |
| remove | 1行を削除する |
| append-1k | 1,000行の後ろに1,000行を追加する |
| clear-1k | 1,000行をすべて削除する |
| rv-fanout | 1,000個の計算値(`mapC`)が監視するRVを100回更新する |
| rv-same-value | 同じく、同じ値を100回代入する |
| crv-chain | 1,000段つながった計算値の根元を100回更新する |
| rvlist-assign | リスト全体を監視される1,000件のRVListに1,000件を代入する |
| reactivelist-assign | 同じく、ReactiveListに代入する |
| rvlist-update / reactivelist-update | リスト全体を監視される1,000件のRVList / ReactiveListの1件を100回書き換える |

- **dom** シナリオ(表を使うもの)は、操作してから画面に反映されてレイアウトが終わるまでの時間です.
  反映は `#app` 以下のDOMの変化を `MutationObserver` で監視して判定します. 準備(前の行の作成など)は計測に含みません.
- **sync** シナリオ(`rv-`/`crv-`/`rvlist-`)は、DOMを使わない同期的な処理時間です.
- 各シナリオは新しいタブで実行します. 時間の単位はmsです.

表示される列:

- `median` `mean` `min` `max` `sd`: 計測時間. 比較には `median` を使います.
- `heap+MB`: シナリオの前後(GC後)のJSヒープの増分.
- `nodes`: 最後の計測直後の `#app` 以下のDOMノード数(テキスト・コメントを含む).
- `leak`: 表を空にした後も、ページを開いた直後より多く残っているDOMノード(`domNodes`)や監視(`selectedWatchers`、`rowsWatchers`)の数. 解除漏れの疑いがあります.

## 注意

- 同じ条件でも10〜30%ほどばらつきます. 比較するときは他のアプリを閉じ、差が小さいときは `--iterations` を増やして何度か実行してください.
- 値はマシンとChromeのバージョンに依存するので、異なる環境の結果同士は比べないでください.
- シナリオを追加するときは `src/main/scala/.../bench/Scenarios.scala` に書きます. 入力データは `Step` を作る時点で用意し、計測される `action` の中では作らないようにします.
