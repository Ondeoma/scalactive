package io.github.ondeoma.scalactive.intellij;

import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * script・styleの中で、対になった括弧の間({|} や (|))でEnterを押したときに、
 * 1段深くインデントした空の行を作り、閉じ括弧を次の行の元のインデントに置きます.
 * <pre>
 * if (ok) {|}   →   if (ok) {
 *                     |
 *                   }
 * </pre>
 * また、stripMarginの三重引用符の文字列の中でEnterを押したときは、次の行の同じ位置に余白の区切り | を補います.
 * (| の後ろのインデントも引き継ぎます)
 * <pre>
 * """|&lt;div&gt;     →   """|&lt;div&gt;
 *    |  &lt;p&gt;|           |  &lt;p&gt;
 *                       |  |
 * </pre>
 */
public final class ScalactiveEnterHandler implements EnterHandlerDelegate {

    private static final int STEP = 2;

    @Override
    public Result preprocessEnter(@NotNull PsiFile file, @NotNull Editor editor, @NotNull Ref<Integer> caretOffset,
                                  @NotNull Ref<Integer> caretAdvance, @NotNull DataContext dataContext, EditorActionHandler originalHandler) {
        if (ScalactiveBracketHandler.where(file, editor) != ScalactiveBracketHandler.Where.RAW) return Result.Continue;
        Document doc = editor.getDocument();
        CharSequence text = doc.getCharsSequence();
        int offset = editor.getCaretModel().getOffset();
        String margin = marginForNewLine(doc, offset);
        if (margin != null) {
            doc.insertString(offset, "\n" + margin);
            editor.getCaretModel().moveToOffset(offset + 1 + margin.length());
            return Result.Stop;
        }
        int before = offset;
        while (before > 0 && (text.charAt(before - 1) == ' ' || text.charAt(before - 1) == '\t')) before--;
        int after = offset;
        while (after < text.length() && (text.charAt(after) == ' ' || text.charAt(after) == '\t')) after++;
        if (before == 0 || after >= text.length()) return Result.Continue;
        char close = text.charAt(after);
        if (")]}".indexOf(close) < 0) return Result.Continue;
        // 行のキャレットより前に、閉じていない対応する開き括弧がある({|} のほか { x =>|} など)
        int lineStart = doc.getLineStartOffset(doc.getLineNumber(offset));
        int opener = ScalactiveLineIndentProvider.lastUnmatchedOpener(text.subSequence(lineStart, before).toString());
        if (opener < 0 || ScalactiveBracketHandler.closerOf(text.charAt(lineStart + opener)) != close) return Result.Continue;

        int indent = 0;
        while (lineStart + indent < text.length() && (text.charAt(lineStart + indent) == ' ' || text.charAt(lineStart + indent) == '\t')) indent++;
        String base = " ".repeat(indent);
        String inner = " ".repeat(indent + STEP);
        doc.replaceString(before, after, "\n" + inner + "\n" + base);
        editor.getCaretModel().moveToOffset(before + 1 + inner.length());
        return Result.Stop;
    }

    /**
     * offsetがstripMarginの三重引用符の文字列の中なら、改行の後に入れる「インデント + | + |の後ろのインデント」(そうでなければnull).
     * stripMarginの文字列とみなすのは、閉じた後に .stripMargin が続くか、今の行・開きの行に | の余白があるとき.
     */
    static String marginForNewLine(Document doc, int offset) {
        CharSequence text = doc.getCharsSequence();
        ScalaScan.TripleString str = null;
        for (ScalaScan.TripleString s : ScalaScan.tripleStrings(text)) {
            int contentEnd = s.closed() ? s.end() - 3 : s.end();
            if (offset >= s.start() + 3 && offset <= contentEnd) {
                str = s;
                break;
            }
        }
        if (str == null) return null;
        int line = doc.getLineNumber(offset);
        int lineStart = doc.getLineStartOffset(line);
        int openLine = doc.getLineNumber(str.start());
        int openLineStart = doc.getLineStartOffset(openLine);

        // | の列: 今の行の | → 文字列の中のほかの行の | → 開きの """| の | の順に探す
        int pipeColumn = -1;
        int afterPipe = 0;
        if (line != openLine) {
            int k = lineStart;
            while (k < offset && (text.charAt(k) == ' ' || text.charAt(k) == '\t')) k++;
            if (k < offset && text.charAt(k) == '|') {
                pipeColumn = k - lineStart;
                int j = k + 1;
                while (j < offset && (text.charAt(j) == ' ' || text.charAt(j) == '\t')) j++;
                afterPipe = j - k - 1;
            }
        }
        boolean openWithPipe = str.start() + 3 < text.length() && text.charAt(str.start() + 3) == '|';
        if (pipeColumn < 0) {
            int lastLine = doc.getLineNumber(Math.min(str.end(), text.length()));
            for (int l = openLine + 1; l <= lastLine && pipeColumn < 0; l++) {
                int k = doc.getLineStartOffset(l);
                int start = k;
                while (k < text.length() && (text.charAt(k) == ' ' || text.charAt(k) == '\t')) k++;
                if (k < str.end() && k < text.length() && text.charAt(k) == '|') pipeColumn = k - start;
            }
        }
        if (pipeColumn < 0 && openWithPipe) pipeColumn = str.start() + 3 - openLineStart;

        boolean stripMargin = str.closed() && followedByStripMargin(text, str.end());
        boolean hasPipe = pipeColumn >= 0 && (stripMargin || openWithPipe || line != openLine);
        if (!stripMargin && !hasPipe) return null;
        if (pipeColumn < 0) pipeColumn = str.start() + 3 - openLineStart;
        // 開きの行で """| の後ろのインデントも引き継ぐ
        if (line == openLine && openWithPipe) {
            int j = str.start() + 4;
            while (j < offset && (text.charAt(j) == ' ' || text.charAt(j) == '\t')) j++;
            afterPipe = j - str.start() - 4;
        }
        return " ".repeat(pipeColumn) + "|" + " ".repeat(afterPipe);
    }

    private static boolean followedByStripMargin(CharSequence text, int at) {
        int k = at;
        while (k < text.length() && Character.isWhitespace(text.charAt(k))) k++;
        return ScalaScan.startsWith(text, k, text.length(), ".stripMargin");
    }

}
