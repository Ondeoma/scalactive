package io.github.ondeoma.scalactive.intellij;

import com.intellij.lang.Language;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.codeStyle.lineIndent.LineIndentProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * &lt;script&gt;(Scala)・&lt;style&gt;(CSS)の中で改行したときのインデント.
 * IntelliJのHTMLの規則では、scriptやstyleの中身はタグより1段深くインデントされてしまうため、前の行を元に決めます.
 * <ul>
 *   <li>前の行と同じインデント</li>
 *   <li>前の行の括弧が閉じていなければ、その括弧の直後にそろえる(行末の括弧なら1段深く)</li>
 *   <li>前の行が { = =&gt; などで終わっていれば1段深く</li>
 *   <li>前の行で前の行より前の括弧が閉じたなら、その括弧を開いた行のインデントに戻す</li>
 *   <li>カーソルの後ろが } ) ] なら1段浅く</li>
 * </ul>
 * テンプレートの中は、これまでどおりIntelliJのHTMLの規則に任せます(nullを返す).
 */
public final class ScalactiveLineIndentProvider implements LineIndentProvider {

    private static final int STEP = 2;

    /** styleの中ではキャレットの位置の言語がCSSになるため、CSSも受け付ける(.st以外のファイルではnullを返す) */
    @Override
    public boolean isSuitableFor(@Nullable Language language) {
        return language != null && (language.isKindOf(ScalactiveLanguage.INSTANCE) || "CSS".equals(language.getID()));
    }

    static boolean isStDocument(@NotNull Document doc) {
        com.intellij.openapi.vfs.VirtualFile f = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getFile(doc);
        return f != null && "st".equals(f.getExtension());
    }

    @Override
    public @Nullable String getLineIndent(@NotNull Project project, @NotNull Editor editor, @Nullable Language language, int offset) {
        Document doc = editor.getDocument();
        CharSequence text = doc.getCharsSequence();
        if (!isStDocument(doc) || !inRawBlock(text, offset)) return null;
        int line = doc.getLineNumber(offset);
        int prev = line - 1;
        while (prev >= 0 && isBlank(lineText(doc, prev))) prev--;
        if (prev < 0) return null;
        String rest = text.subSequence(offset, doc.getLineEndOffset(line)).toString().trim();
        return spaces(indentFor(doc, prev, rest));
    }

    /** offsetが &lt;script&gt; か &lt;style&gt; の中身にあるか */
    static boolean inRawBlock(CharSequence text, int offset) {
        String before = text.subSequence(0, Math.min(offset, text.length())).toString();
        for (String tag : new String[]{"script", "style"}) {
            int open = before.lastIndexOf("<" + tag);
            if (open >= 0 && before.lastIndexOf("</" + tag) < open && before.indexOf('>', open) >= 0) return true;
        }
        return false;
    }

    private static int indentFor(Document doc, int prev, String rest) {
        String p = lineText(doc, prev);
        int base = indentOf(p);
        boolean closesNext = !rest.isEmpty() && "})]".indexOf(rest.charAt(0)) >= 0;
        if (p.trim().startsWith("<")) return base; // <script>・<style> の行の次

        int unmatched = lastUnmatchedOpener(p);
        if (unmatched >= 0) {
            boolean atEnd = p.substring(unmatched + 1).trim().isEmpty();
            // { のブロック({ x => など)は1段深く、( [ は括弧の直後にそろえる
            if (atEnd || p.charAt(unmatched) == '{') return closesNext ? base : base + STEP;
            return unmatched + 1;
        }
        // この行で、前の行から続く括弧が閉じた → その括弧を開いた行のインデントに戻す
        if (balance(p) < 0) {
            int depth = balance(p);
            for (int l = prev - 1; l >= 0 && depth < 0; l--) {
                String t = lineText(doc, l);
                if (t.trim().startsWith("<")) break;
                depth += balance(t);
                if (depth >= 0) {
                    base = indentOf(t);
                    break;
                }
            }
        }
        String trimmed = p.trim();
        if (trimmed.endsWith("{") || trimmed.endsWith("=") || trimmed.endsWith("=>") || trimmed.endsWith("->")) {
            return closesNext ? base : base + STEP;
        }
        return closesNext ? Math.max(base - STEP, 0) : base;
    }

    /** 行の中で閉じていない最後の ( [ { の位置(なければ -1). 文字列・// コメントの中は数えない */
    static int lastUnmatchedOpener(String line) {
        java.util.ArrayDeque<Integer> stack = new java.util.ArrayDeque<>();
        scan(line, (i, c) -> {
            if ("([{".indexOf(c) >= 0) stack.push(i);
            else if (!stack.isEmpty()) stack.pop();
        });
        return stack.isEmpty() ? -1 : stack.peek();
    }

    /** 開き括弧の数 - 閉じ括弧の数 */
    private static int balance(String line) {
        int[] n = {0};
        scan(line, (i, c) -> n[0] += "([{".indexOf(c) >= 0 ? 1 : -1);
        return n[0];
    }

    private interface BracketVisitor {
        void visit(int index, char c);
    }

    private static void scan(String line, BracketVisitor v) {
        char quote = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quote != 0) {
                if (c == '\\') i++;
                else if (c == quote) quote = 0;
            } else if (c == '"' || c == '\'') {
                // 'a' のような文字リテラルと、Scalaのシンボル・型パラメータ('a)を区別しきれないため、閉じる ' があるときだけ文字列扱い
                if (c == '"' || line.indexOf('\'', i + 1) >= 0) quote = c;
            } else if (c == '/' && i + 1 < line.length() && line.charAt(i + 1) == '/') {
                return;
            } else if ("([{)]}".indexOf(c) >= 0) {
                v.visit(i, c);
            }
        }
    }

    private static String lineText(Document doc, int line) {
        return doc.getCharsSequence().subSequence(doc.getLineStartOffset(line), doc.getLineEndOffset(line)).toString();
    }

    private static boolean isBlank(String s) {
        return s.trim().isEmpty();
    }

    private static int indentOf(String s) {
        int i = 0;
        while (i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\t')) i++;
        return i;
    }

    private static String spaces(int n) {
        return " ".repeat(Math.max(n, 0));
    }

}
