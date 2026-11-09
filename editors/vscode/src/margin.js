// stripMarginの三重引用符の文字列の中で改行したときに、次の行の同じ位置に余白の区切り | を補います.
// (| の後ろのインデントも引き継ぎます. IntelliJ版の ScalactiveEnterHandler と同じ判定)
//
// <script>の中は埋め込みの言語(Scala)の設定が使われ、.stの onEnterRules が効かないため、
// 改行の入力(変更)を見て、その後で | を入れます.
const vscode = require("vscode");

/** <script ...> の開始タグ */
const SCRIPT_START_TAG = /<script\b[^>]*>/gi;

function startsWith(s, at, end, p) {
  return at + p.length <= end && s.startsWith(p, at);
}

function startsWithIgnoreCase(s, at, end, p) {
  return at + p.length <= end && s.substr(at, p.length).toLowerCase() === p;
}

/**
 * [from, end) をScalaとして読み(文字列・コメントを区別する)、文字列・コメントの外にある最初の </script の位置を返す(無ければ-1).
 * strings に三重引用符の文字列 {start, end, closed} を加える.
 */
function scan(s, from, end, strings) {
  let i = from;
  while (i < end) {
    const c = s[i];
    if (c === "<" && startsWithIgnoreCase(s, i, end, "</script")) {
      return i;
    } else if (startsWith(s, i, end, "//")) {
      while (i < end && s[i] !== "\n") i++;
    } else if (startsWith(s, i, end, "/*")) {
      let depth = 0;
      while (i < end) {
        if (startsWith(s, i, end, "/*")) { depth++; i += 2; }
        else if (startsWith(s, i, end, "*/")) { depth--; i += 2; if (depth === 0) break; }
        else i++;
      }
    } else if (startsWith(s, i, end, '"""')) {
      const j = s.indexOf('"""', i + 3);
      let close = j < 0 || j + 3 > end ? end : j + 3;
      while (close < end && s[close] === '"') close++;
      strings.push({ start: i, end: close, closed: j >= 0 && j + 3 <= end });
      i = close;
    } else if (c === '"') {
      let j = i + 1;
      while (j < end && s[j] !== '"' && s[j] !== "\n") j += s[j] === "\\" ? 2 : 1;
      i = Math.min(j + 1, end);
    } else if (c === "'" && i + 2 < end && (s[i + 2] === "'" || s[i + 1] === "\\")) {
      const j = s.indexOf("'", i + 2);
      i = j < 0 ? end : j + 1;
    } else {
      i++;
    }
  }
  return -1;
}

/** ファイル全体の、<script>の中の三重引用符の文字列 */
function tripleStrings(text) {
  const strings = [];
  SCRIPT_START_TAG.lastIndex = 0;
  let m;
  while ((m = SCRIPT_START_TAG.exec(text)) !== null) {
    const contentStart = m.index + m[0].length;
    const found = scan(text, contentStart, text.length, strings);
    const end = found >= 0 ? found : text.length;
    SCRIPT_START_TAG.lastIndex = Math.max(end, contentStart + 1);
  }
  return strings;
}

const lineStartOf = (text, offset) => text.lastIndexOf("\n", offset - 1) + 1;
const isBlank = (ch) => ch === " " || ch === "\t";

/**
 * offset(改行を入れた位置)がstripMarginの三重引用符の文字列の中なら、次の行に入れる「インデント + | + |の後ろのインデント」.
 * そうでなければundefined. stripMarginの文字列とみなすのは、閉じた後に .stripMargin が続くか、| の余白があるとき.
 */
function marginForNewLine(text, offset) {
  const str = tripleStrings(text).find((s) => offset >= s.start + 3 && offset <= (s.closed ? s.end - 3 : s.end));
  if (!str) return undefined;
  const lineStart = lineStartOf(text, offset);
  const openLineStart = lineStartOf(text, str.start);
  const onOpenLine = lineStart === openLineStart;

  // | の列: 今の行の | → 文字列の中のほかの行の | → 開きの """| の | の順に探す
  let pipeColumn = -1;
  let afterPipe = 0;
  if (!onOpenLine) {
    let k = lineStart;
    while (k < offset && isBlank(text[k])) k++;
    if (k < offset && text[k] === "|") {
      pipeColumn = k - lineStart;
      let j = k + 1;
      while (j < offset && isBlank(text[j])) j++;
      afterPipe = j - k - 1;
    }
  }
  const openWithPipe = text[str.start + 3] === "|";
  if (pipeColumn < 0) {
    let l = text.indexOf("\n", str.start);
    while (l >= 0 && l < str.end && pipeColumn < 0) {
      let k = l + 1;
      while (k < text.length && isBlank(text[k])) k++;
      if (k < str.end && text[k] === "|") pipeColumn = k - (l + 1);
      l = text.indexOf("\n", l + 1);
    }
  }
  if (pipeColumn < 0 && openWithPipe) pipeColumn = str.start + 3 - openLineStart;

  const stripMargin = str.closed && /^\s*\.stripMargin/.test(text.slice(str.end, str.end + 200));
  const hasPipe = pipeColumn >= 0 && (stripMargin || openWithPipe || !onOpenLine);
  if (!stripMargin && !hasPipe) return undefined;
  if (pipeColumn < 0) pipeColumn = str.start + 3 - openLineStart;
  if (onOpenLine && openWithPipe) {
    let j = str.start + 4;
    while (j < offset && isBlank(text[j])) j++;
    afterPipe = j - str.start - 4;
  }
  return " ".repeat(pipeColumn) + "|" + " ".repeat(afterPipe);
}

/** 改行の入力を見て、stripMarginの文字列の中なら | を補う */
function register(context) {
  context.subscriptions.push(
    vscode.workspace.onDidChangeTextDocument((e) => {
      if (e.document.languageId !== "scalactive" || e.reason !== undefined || e.contentChanges.length !== 1) return;
      const change = e.contentChanges[0];
      // Enterによる入力(改行 + 自動のインデント)だけを対象にする
      if (change.rangeLength !== 0 || !/^\r?\n[ \t]*$/.test(change.text)) return;
      const editor = vscode.window.activeTextEditor;
      if (!editor || editor.document !== e.document || editor.selections.length !== 1) return;
      const doc = e.document;
      const text = doc.getText();
      const margin = marginForNewLine(text, change.rangeOffset);
      if (margin === undefined) return;
      const newLineStart = change.rangeOffset + change.text.indexOf("\n") + 1;
      const indentEnd = change.rangeOffset + change.text.length;
      const range = new vscode.Range(doc.positionAt(newLineStart), doc.positionAt(indentEnd));
      editor.edit((b) => b.replace(range, margin), { undoStopBefore: false, undoStopAfter: false }).then((ok) => {
        if (!ok) return;
        const caret = doc.positionAt(newLineStart + margin.length);
        editor.selection = new vscode.Selection(caret, caret);
      });
    }),
  );
}

module.exports = { register, marginForNewLine, tripleStrings };
