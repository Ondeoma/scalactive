package io.github.ondeoma.scalactive.intellij;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * &lt;script&gt;の中の、複数行にわたる三重引用符の文字列を折りたためるようにします("""...""" と表示).
 * (HTMLの折りたたみに加えて登録し、.stのファイルでだけ働きます. HTMLのタグの折りたたみもそのまま使えます)
 */
public final class ScalactiveFoldingBuilder extends FoldingBuilderEx implements DumbAware {

    @Override
    public FoldingDescriptor @NotNull [] buildFoldRegions(@NotNull PsiElement root, @NotNull Document document, boolean quick) {
        // HTMLの折りたたみに加えて登録しているため、.st以外では何もしない
        if (!(root.getContainingFile() != null && root.getContainingFile().getLanguage() == ScalactiveLanguage.INSTANCE)) {
            return FoldingDescriptor.EMPTY_ARRAY;
        }
        CharSequence text = document.getCharsSequence();
        List<FoldingDescriptor> result = new ArrayList<>();
        for (ScalaScan.TripleString s : ScalaScan.tripleStrings(text)) {
            if (!s.closed()) continue;
            TextRange range = s.range();
            if (document.getLineNumber(range.getStartOffset()) == document.getLineNumber(range.getEndOffset())) continue;
            result.add(new FoldingDescriptor(root.getNode(), range, null, "\"\"\"...\"\"\""));
        }
        return result.toArray(FoldingDescriptor.EMPTY_ARRAY);
    }

    @Override
    public @Nullable String getPlaceholderText(@NotNull ASTNode node) {
        return "\"\"\"...\"\"\"";
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode node) {
        return false;
    }

}
