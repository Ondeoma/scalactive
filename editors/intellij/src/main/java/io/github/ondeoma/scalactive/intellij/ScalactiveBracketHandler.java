package io.github.ondeoma.scalactive.intellij;

import com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * &lt;script&gt;・&lt;style&gt;の中の括弧と引用符の自動補完.
 * <ul>
 *   <li>( [ { を入力すると閉じ括弧を、" を入力すると閉じる " を補う(キャレットの後ろが空白・行末・閉じ括弧などのとき)</li>
 *   <li>補った閉じ括弧・引用符の上で同じ文字を入力すると、上書きせずに通り過ぎる</li>
 *   <li>空の括弧・引用符の組の間でBackspaceを押すと、閉じるほうも消す</li>
 * </ul>
 * テンプレートの中では、Scalaの部分({{ }} と、ディレクティブ・:attr・@event・#slot の属性値)だけで同じことをします
 * (属性値の中の " は、属性を ' で囲んだときだけ補います).
 */
public final class ScalactiveBracketHandler {

    private ScalactiveBracketHandler() {
    }

    private static final String OPEN = "([{";
    private static final String CLOSE = ")]}";

    /** キャレットの位置: 補完しない / script・style / テンプレートのScala(引用符も補うか) */
    enum Where { NONE, RAW, SCALA, SCALA_NO_QUOTE }

    static Where where(PsiFile file, Editor editor) {
        if (file == null || !"st".equals(file.getVirtualFile() == null ? null : file.getVirtualFile().getExtension())) return Where.NONE;
        if (editor.getSelectionModel().hasSelection()) return Where.NONE;
        CharSequence text = editor.getDocument().getCharsSequence();
        int offset = editor.getCaretModel().getOffset();
        if (ScalactiveLineIndentProvider.inRawBlock(text, offset)) return Where.RAW;
        return templateContext(text, offset);
    }

    static boolean applies(PsiFile file, Editor editor) {
        return where(file, editor) != Where.NONE;
    }

    private static boolean isScalaAttribute(String name) {
        return name.startsWith(":") || name.startsWith("@") || name.startsWith("#") || name.startsWith("v-");
    }

    /**
     * テンプレートの先頭からoffsetまでを走査して、offsetが {{ }} の中か、Scalaを書く属性値の中かを調べます.
     * (属性値の中の &gt; や {{ }} の中の &lt; を正しく読み飛ばすため、タグ単位で状態を持って走査します)
     */
    static Where templateContext(CharSequence text, int offset) {
        String t = text.toString();
        int open = t.indexOf("<template");
        if (open < 0 || open >= offset) return Where.NONE;
        int i = t.indexOf('>', open);
        if (i < 0 || i >= offset) return Where.NONE;
        i++;
        final int TEXT = 0, TAG = 1, VALUE = 2, INTERP = 3, COMMENT = 4;
        int state = TEXT;
        char quote = 0;
        StringBuilder attr = new StringBuilder();
        String valueOf = "";
        boolean afterEq = false;
        while (i < offset) {
            char c = t.charAt(i);
            switch (state) {
                case TEXT -> {
                    if (t.startsWith("<!--", i)) { state = COMMENT; i += 4; continue; }
                    if (t.startsWith("{{", i)) { state = INTERP; i += 2; continue; }
                    if (c == '<' && i + 1 < t.length() && (Character.isLetter(t.charAt(i + 1)) || t.charAt(i + 1) == '/')) {
                        state = TAG;
                        attr.setLength(0);
                        afterEq = false;
                        // タグ名を読み飛ばす
                        i++;
                        while (i < offset && !Character.isWhitespace(t.charAt(i)) && t.charAt(i) != '>' && t.charAt(i) != '/') i++;
                        continue;
                    }
                }
                case TAG -> {
                    if (c == '>') state = TEXT;
                    else if (c == '=') afterEq = true;
                    else if ((c == '"' || c == '\'') && afterEq) {
                        state = VALUE;
                        quote = c;
                        valueOf = attr.toString();
                    } else if (Character.isWhitespace(c)) {
                        if (!afterEq && attr.length() > 0 && i + 1 < t.length() && t.charAt(i + 1) != '=' && !Character.isWhitespace(t.charAt(i + 1))) {
                            attr.setLength(0);
                        }
                    } else if (c != '/') {
                        if (afterEq) {
                            // 引用符のない属性値
                            afterEq = false;
                            attr.setLength(0);
                        }
                        attr.append(c);
                    }
                }
                case VALUE -> {
                    if (c == quote) {
                        state = TAG;
                        attr.setLength(0);
                        afterEq = false;
                    }
                }
                case INTERP -> {
                    if (t.startsWith("}}", i)) { state = TEXT; i += 2; continue; }
                }
                case COMMENT -> {
                    if (t.startsWith("-->", i)) { state = TEXT; i += 3; continue; }
                }
                default -> { }
            }
            i++;
        }
        if (state == INTERP) return Where.SCALA;
        if (state == VALUE && isScalaAttribute(valueOf)) return quote == '\'' ? Where.SCALA : Where.SCALA_NO_QUOTE;
        return Where.NONE;
    }

    static char closerOf(char c) {
        int i = OPEN.indexOf(c);
        return i >= 0 ? CLOSE.charAt(i) : c;
    }

    /** 閉じ括弧を補ってよい位置か(後ろが空白・行末・閉じ括弧・区切り) */
    private static boolean canPair(CharSequence text, int offset, Where where) {
        if (offset >= text.length()) return true;
        char next = text.charAt(offset);
        // 属性値の中では、属性を閉じる引用符の直前でも補う
        if ((where == Where.SCALA || where == Where.SCALA_NO_QUOTE) && (next == '"' || next == '\'')) return true;
        return Character.isWhitespace(next) || CLOSE.indexOf(next) >= 0 || next == ',' || next == ';' || next == ':';
    }

    /** 行の先頭からoffsetまでに、閉じていない " があるか */
    private static boolean insideString(Document doc, int offset) {
        int start = doc.getLineStartOffset(doc.getLineNumber(offset));
        CharSequence text = doc.getCharsSequence();
        boolean in = false;
        for (int i = start; i < offset; i++) {
            char c = text.charAt(i);
            if (c == '\\' && in) i++;
            else if (c == '"') in = !in;
        }
        return in;
    }

    public static final class Typed extends TypedHandlerDelegate {
        @Override
        public @NotNull Result beforeCharTyped(char c, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file, @NotNull FileType fileType) {
            if (OPEN.indexOf(c) < 0 && CLOSE.indexOf(c) < 0 && c != '"') return Result.CONTINUE;
            Where where = where(file, editor);
            if (where == Where.NONE || (c == '"' && where == Where.SCALA_NO_QUOTE)) return Result.CONTINUE;
            Document doc = editor.getDocument();
            CharSequence text = doc.getCharsSequence();
            int offset = editor.getCaretModel().getOffset();
            char next = offset < text.length() ? text.charAt(offset) : 0;

            if (CLOSE.indexOf(c) >= 0 || c == '"') {
                // 閉じる文字の上で同じ文字を入力したら通り過ぎる
                if (next == c && (c != '"' || insideString(doc, offset))) {
                    editor.getCaretModel().moveToOffset(offset + 1);
                    return Result.STOP;
                }
                if (c != '"') return Result.CONTINUE;
            }
            // 文字列を閉じる " と、単語の直前の括弧は、1文字だけ入力する(ほかのハンドラにも補わせない)
            boolean pair = !(c == '"' && insideString(doc, offset)) && canPair(text, offset, where);
            doc.insertString(offset, pair ? String.valueOf(c) + closerOf(c) : String.valueOf(c));
            editor.getCaretModel().moveToOffset(offset + 1);
            return Result.STOP;
        }
    }

    public static final class Backspace extends BackspaceHandlerDelegate {
        private boolean deletePair;

        @Override
        public void beforeCharDeleted(char c, @NotNull PsiFile file, @NotNull Editor editor) {
            deletePair = false;
            if (OPEN.indexOf(c) < 0 && c != '"') return;
            Where where = where(file, editor);
            if (where == Where.NONE || (c == '"' && where == Where.SCALA_NO_QUOTE)) return;
            CharSequence text = editor.getDocument().getCharsSequence();
            int offset = editor.getCaretModel().getOffset();
            deletePair = offset < text.length() && text.charAt(offset) == closerOf(c);
        }

        @Override
        public boolean charDeleted(char c, @NotNull PsiFile file, @NotNull Editor editor) {
            if (!deletePair) return false;
            deletePair = false;
            int offset = editor.getCaretModel().getOffset();
            editor.getDocument().deleteString(offset, offset + 1);
            return true;
        }
    }

}
