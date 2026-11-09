package io.github.ondeoma.scalactive.intellij;

import com.intellij.html.embedding.HtmlEmbeddedContentProvider;
import com.intellij.html.embedding.HtmlEmbeddedContentSupport;
import com.intellij.html.embedding.HtmlEmbedmentInfo;
import com.intellij.lexer.BaseHtmlLexer;
import com.intellij.lexer.HtmlLexer;
import com.intellij.lexer.HtmlScriptStyleEmbeddedContentProvider;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * .stの字句解析. HTMLの字句解析と同じですが、&lt;script&gt;の中身(Scala)をJavaScriptとして扱わず、そのままのテキストにします.
 * (&lt;style&gt;は通常のHTMLと同じくCSSとして扱われます)
 */
public final class ScalactiveLexer extends HtmlLexer {

    public ScalactiveLexer() {
        super();
    }

    public ScalactiveLexer(boolean highlightMode) {
        super(highlightMode);
    }

    /** scriptの中身をそのままのテキストにする(styleの扱いは変えない) */
    private static final class ScriptAsRawText extends HtmlScriptStyleEmbeddedContentProvider {
        ScriptAsRawText(BaseHtmlLexer lexer) {
            super(lexer);
        }

        @Override
        protected HtmlEmbedmentInfo scriptEmbedmentInfo(String mimeType) {
            return HtmlEmbeddedContentProvider.RAW_TEXT_EMBEDMENT;
        }
    }

    private static final HtmlEmbeddedContentSupport SUPPORT = new HtmlEmbeddedContentSupport() {
        @Override
        public @NotNull List<HtmlEmbeddedContentProvider> createEmbeddedContentProviders(@NotNull BaseHtmlLexer lexer) {
            return List.of(new ScriptAsRawText(lexer));
        }
    };

    @Override
    protected @NotNull List<HtmlEmbeddedContentSupport> getEmbeddedContentSupportList() {
        List<HtmlEmbeddedContentSupport> list = new ArrayList<>(super.getEmbeddedContentSupportList());
        list.add(SUPPORT);
        return list;
    }

    @Override
    protected boolean acceptEmbeddedContentProvider(@NotNull HtmlEmbeddedContentProvider provider) {
        // 標準のscript/styleの扱いは、上のScriptAsRawTextに置き換える
        if (provider.getClass() == HtmlScriptStyleEmbeddedContentProvider.class) return false;
        return super.acceptEmbeddedContentProvider(provider);
    }

}
