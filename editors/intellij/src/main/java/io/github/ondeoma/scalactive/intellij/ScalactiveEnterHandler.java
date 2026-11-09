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

}
