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
 * (テンプレートの中は、IntelliJのHTMLの補完に任せます)
 */
public final class ScalactiveBracketHandler {

    private ScalactiveBracketHandler() {
    }

    private static final String OPEN = "([{";
    private static final String CLOSE = ")]}";

    static boolean applies(PsiFile file, Editor editor) {
        if (file == null || !"st".equals(file.getVirtualFile() == null ? null : file.getVirtualFile().getExtension())) return false;
        if (editor.getSelectionModel().hasSelection()) return false;
        return ScalactiveLineIndentProvider.inRawBlock(editor.getDocument().getCharsSequence(), editor.getCaretModel().getOffset());
    }

    static char closerOf(char c) {
        int i = OPEN.indexOf(c);
        return i >= 0 ? CLOSE.charAt(i) : c;
    }

    /** 閉じ括弧を補ってよい位置か(後ろが空白・行末・閉じ括弧・区切り) */
    private static boolean canPair(CharSequence text, int offset) {
        if (offset >= text.length()) return true;
        char next = text.charAt(offset);
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
            if (!applies(file, editor)) return Result.CONTINUE;
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
            boolean pair = !(c == '"' && insideString(doc, offset)) && canPair(text, offset);
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
            if ((OPEN.indexOf(c) < 0 && c != '"') || !applies(file, editor)) return;
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
