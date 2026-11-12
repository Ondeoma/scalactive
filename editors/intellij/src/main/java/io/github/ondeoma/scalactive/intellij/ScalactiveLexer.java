package io.github.ondeoma.scalactive.intellij;

import com.intellij.html.embedding.HtmlEmbeddedContentProvider;
import com.intellij.lang.Language;
import com.intellij.html.embedding.HtmlEmbeddedContentSupport;
import com.intellij.html.embedding.HtmlEmbedmentInfo;
import com.intellij.lexer.BaseHtmlLexer;
import com.intellij.lexer.HtmlLexer;
import com.intellij.lexer.HtmlScriptStyleEmbeddedContentProvider;
import com.intellij.lexer.LexerBase;
import com.intellij.lexer.LexerPosition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * .stの字句解析. HTMLの字句解析と同じですが、
 * <ul>
 *   <li>&lt;script&gt;の中身(Scala)をJavaScriptとして扱わず、そのままのテキストにします.
 *       (&lt;style&gt;は通常のHTMLと同じくCSSとして扱われます. lang="scss" / lang="sass" は、
 *       Sassのプラグインがあれば、SCSS・Sassとして扱います)</li>
 *   <li>&lt;script&gt;の中身は、Scalaの文字列・コメントの中の &lt;/script&gt; では終わりません
 *       (コンパイラーのSfcParserと同じ). コードサンプルの文字列などで、外側の&lt;script&gt;が途中で閉じ、
 *       残りがHTML・CSSとして解析されて誤ったエラーが出ないようにするためです.</li>
 * </ul>
 */
public final class ScalactiveLexer extends LexerBase {

    /** HTMLの字句解析(scriptの中身をそのままのテキストにする) */
    private static final class HtmlPart extends HtmlLexer {
        HtmlPart(boolean highlightMode) {
            super(highlightMode);
        }

        @Override
        protected @NotNull List<HtmlEmbeddedContentSupport> getEmbeddedContentSupportList() {
            List<HtmlEmbeddedContentSupport> list = new ArrayList<>(super.getEmbeddedContentSupportList());
            list.add(SUPPORT);
            return list;
        }

        @Override
        protected boolean acceptEmbeddedContentProvider(@NotNull HtmlEmbeddedContentProvider provider) {
            // 標準のscript/styleの扱いは、下のScriptAsRawTextに置き換える
            if (provider.getClass() == HtmlScriptStyleEmbeddedContentProvider.class) return false;
            return super.acceptEmbeddedContentProvider(provider);
        }
    }

    /**
     * scriptの中身をそのままのテキストにする.
     * styleは、標準の type 属性に加えて lang 属性も見て、scss・sass をSCSS・Sassとして扱う(Sassのプラグインがない場合はCSS).
     */
    private static final class ScriptAsRawText extends HtmlScriptStyleEmbeddedContentProvider {
        ScriptAsRawText(BaseHtmlLexer lexer) {
            super(lexer);
        }

        @Override
        protected HtmlEmbedmentInfo scriptEmbedmentInfo(String mimeType) {
            return HtmlEmbeddedContentProvider.RAW_TEXT_EMBEDMENT;
        }

        @Override
        protected boolean isInterestedInAttribute(@NotNull CharSequence attributeName) {
            return super.isInterestedInAttribute(attributeName)
                || (StringUtil.equalsIgnoreCase(getTagName(), "style") && StringUtil.equalsIgnoreCase(attributeName, "lang"));
        }

        @Override
        protected @Nullable Language styleLanguage(@Nullable String styleLang) {
            String id = styleLang == null ? null : switch (StringUtil.toLowerCase(styleLang.trim())) {
                case "scss" -> "SCSS";
                case "sass" -> "SASS";
                default -> null;
            };
            Language lang = id == null ? null : Language.findLanguageByID(id);
            return lang != null ? lang : super.styleLanguage(styleLang);
        }
    }

    private static final HtmlEmbeddedContentSupport SUPPORT = new HtmlEmbeddedContentSupport() {
        @Override
        public @NotNull List<HtmlEmbeddedContentProvider> createEmbeddedContentProviders(@NotNull BaseHtmlLexer lexer) {
            return List.of(new ScriptAsRawText(lexer));
        }
    };

    private final HtmlPart html;
    private CharSequence original = "";

    public ScalactiveLexer() {
        this(false);
    }

    public ScalactiveLexer(boolean highlightMode) {
        html = new HtmlPart(highlightMode);
    }

    /**
     * 元のHTMLの字句解析には、scriptの中のScalaの文字列・コメントにある &lt;/script の &lt; を空白にしたテキストを渡します.
     * (長さは変えないので、トークンの位置はそのまま元のテキストの位置になります)
     */
    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        original = buffer;
        html.start(masked(buffer), startOffset, endOffset, initialState);
    }

    @Override
    public @NotNull CharSequence getBufferSequence() {
        return original;
    }

    @Override
    public int getBufferEnd() {
        return html.getBufferEnd();
    }

    @Override
    public int getState() {
        return html.getState();
    }

    @Override
    public @Nullable IElementType getTokenType() {
        return html.getTokenType();
    }

    @Override
    public int getTokenStart() {
        return html.getTokenStart();
    }

    @Override
    public int getTokenEnd() {
        return html.getTokenEnd();
    }

    @Override
    public void advance() {
        html.advance();
    }

    @Override
    public @NotNull LexerPosition getCurrentPosition() {
        return html.getCurrentPosition();
    }

    @Override
    public void restore(@NotNull LexerPosition position) {
        html.restore(position);
    }

    /** scriptの中身の、本当の終わりより前にある &lt;/script の &lt; を空白にしたテキスト(無ければ元のまま) */
    static CharSequence masked(CharSequence text) {
        StringBuilder sb = null;
        for (com.intellij.openapi.util.TextRange r : ScalaScan.scriptContents(text)) {
            int end = r.getEndOffset();
            for (int k = r.getStartOffset(); k < end; k++) {
                if (text.charAt(k) == '<' && ScalaScan.startsWithIgnoreCase(text, k, Math.min(end + 8, text.length()), "</script")) {
                    if (sb == null) sb = new StringBuilder(text);
                    sb.setCharAt(k, ' ');
                }
            }
        }
        return sb == null ? text : sb;
    }

}
