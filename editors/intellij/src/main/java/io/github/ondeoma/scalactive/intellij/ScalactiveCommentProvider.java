package io.github.ondeoma.scalactive.intellij;

import com.intellij.lang.Commenter;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageCommenters;
import com.intellij.lang.html.HTMLLanguage;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;

/**
 * コメントの切り替え(Cmd+/ など)を、ブロックごとの書き方にします.
 * &lt;script&gt; はScala(// と /* *&#47;)、&lt;style&gt; はCSS(/* *&#47;)、&lt;template&gt; はHTML(&lt;!-- --&gt;).
 */
public final class ScalactiveCommentProvider implements com.intellij.psi.templateLanguages.MultipleLangCommentProvider {

    private static final Commenter SCALA = new Commenter() {
        @Override public String getLineCommentPrefix() { return "//"; }
        @Override public String getBlockCommentPrefix() { return "/*"; }
        @Override public String getBlockCommentSuffix() { return "*/"; }
        @Override public String getCommentedBlockCommentPrefix() { return null; }
        @Override public String getCommentedBlockCommentSuffix() { return null; }
    };

    private static final Commenter CSS_FALLBACK = new Commenter() {
        @Override public String getLineCommentPrefix() { return null; }
        @Override public String getBlockCommentPrefix() { return "/*"; }
        @Override public String getBlockCommentSuffix() { return "*/"; }
        @Override public String getCommentedBlockCommentPrefix() { return null; }
        @Override public String getCommentedBlockCommentSuffix() { return null; }
    };

    @Override
    public boolean canProcess(PsiFile file, FileViewProvider viewProvider) {
        return file.getLanguage() == ScalactiveLanguage.INSTANCE;
    }

    @Override
    public @Nullable Commenter getLineCommenter(PsiFile file, Editor editor, Language lineStartLanguage, Language lineEndLanguage) {
        String block = blockAt(file, offsetOf(editor));
        if ("script".equals(block)) return SCALA;
        if ("style".equals(block)) {
            Language css = Language.findLanguageByID("CSS");
            Commenter c = css == null ? null : LanguageCommenters.INSTANCE.forLanguage(css);
            return c != null ? c : CSS_FALLBACK;
        }
        return LanguageCommenters.INSTANCE.forLanguage(HTMLLanguage.INSTANCE);
    }

    /** 選択範囲(なければキャレット)の先頭行の、最初の空白でない文字の位置 */
    private static int offsetOf(Editor editor) {
        Document doc = editor.getDocument();
        int offset = editor.getSelectionModel().hasSelection() ? editor.getSelectionModel().getSelectionStart() : editor.getCaretModel().getOffset();
        int line = doc.getLineNumber(Math.min(offset, doc.getTextLength()));
        int i = doc.getLineStartOffset(line);
        int end = doc.getLineEndOffset(line);
        CharSequence text = doc.getCharsSequence();
        while (i < end && Character.isWhitespace(text.charAt(i))) i++;
        return i;
    }

    /** offsetを含む最上位のブロック(script / template / style)の名前 */
    static @Nullable String blockAt(PsiFile file, int offset) {
        PsiElement e = file.findElementAt(offset);
        XmlTag top = null;
        for (XmlTag t = PsiTreeUtil.getParentOfType(e, XmlTag.class, false); t != null; t = PsiTreeUtil.getParentOfType(t, XmlTag.class, true)) {
            top = t;
            if (t.getParent() instanceof XmlDocument) break;
        }
        return top == null ? null : top.getName();
    }

}
