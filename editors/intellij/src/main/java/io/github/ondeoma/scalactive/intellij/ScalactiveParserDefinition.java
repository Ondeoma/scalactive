package io.github.ondeoma.scalactive.intellij;

import com.intellij.lang.html.HTMLParserDefinition;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.html.HtmlFileImpl;
import com.intellij.psi.tree.IFileElementType;
import org.jetbrains.annotations.NotNull;

/** .stをHTMLとして解析します(字句解析・構文解析はHTMLのものをそのまま使う) */
public final class ScalactiveParserDefinition extends HTMLParserDefinition {

    static final IFileElementType FILE = new IFileElementType(ScalactiveLanguage.INSTANCE);

    @Override
    public @NotNull IFileElementType getFileNodeType() {
        return FILE;
    }

    @Override
    public @NotNull Lexer createLexer(Project project) {
        return new ScalactiveLexer();
    }

    @Override
    public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider) {
        return new HtmlFileImpl(viewProvider, FILE);
    }

}
