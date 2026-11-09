package io.github.ondeoma.scalactive.intellij;

import com.intellij.html.embedding.HtmlEmbeddedContentProvider;
import com.intellij.html.embedding.HtmlEmbeddedContentSupport;
import com.intellij.html.embedding.HtmlEmbedmentInfo;
import com.intellij.lexer.BaseHtmlLexer;
import com.intellij.lexer.HtmlLexer;
import com.intellij.lexer.HtmlScriptStyleEmbeddedContentProvider;
import com.intellij.lexer.LexerBase;
import com.intellij.lexer.LexerPosition;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * .stの字句解析. HTMLの字句解析と同じですが、
 * <ul>
 *   <li>&lt;script&gt;の中身(Scala)をJavaScriptとして扱わず、そのままのテキストにします.
 *       (&lt;style&gt;は通常のHTMLと同じくCSSとして扱われます)</li>
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

    /** &lt;script ...&gt; の開始タグ */
    private static final Pattern SCRIPT_START_TAG = Pattern.compile("<script\\b[^>]*>", Pattern.CASE_INSENSITIVE);

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
        java.util.regex.Matcher m = SCRIPT_START_TAG.matcher(text);
        int from = 0;
        while (from < text.length() && m.find(from)) {
            int contentStart = m.end();
            int end = scriptEnd(text, contentStart, text.length());
            for (int k = contentStart; k < end; k++) {
                if (text.charAt(k) == '<' && startsWithIgnoreCase(text, k, Math.min(end + 8, text.length()), "</script")) {
                    if (sb == null) sb = new StringBuilder(text);
                    sb.setCharAt(k, ' ');
                }
            }
            from = Math.max(end, contentStart);
            if (from == contentStart && end == contentStart) from = contentStart + 1;
        }
        return sb == null ? text : sb;
    }

    /**
     * scriptの中身の終わり(&lt;/script の位置). Scalaの文字列・コメント・文字リテラルの中は飛ばします.
     * コードの中に見つからなければ(閉じていない文字列など)単純に探します. どちらも無ければend.
     * (コンパイラーの SfcParser / StCompiler.codeOnly と同じ考え方)
     */
    static int scriptEnd(CharSequence s, int from, int end) {
        int i = from;
        while (i < end) {
            char c = s.charAt(i);
            if (c == '<' && startsWithIgnoreCase(s, i, end, "</script")) {
                return i;
            } else if (c == '/' && startsWith(s, i, end, "//")) {
                while (i < end && s.charAt(i) != '\n') i++;
            } else if (c == '/' && startsWith(s, i, end, "/*")) {
                int depth = 0;
                while (i < end) {
                    if (startsWith(s, i, end, "/*")) { depth++; i += 2; }
                    else if (startsWith(s, i, end, "*/")) { depth--; i += 2; if (depth == 0) break; }
                    else i++;
                }
            } else if (c == '"' && startsWith(s, i, end, "\"\"\"")) {
                int j = indexOf(s, "\"\"\"", i + 3, end);
                i = j < 0 ? end : j + 3;
                while (i < end && s.charAt(i) == '"') i++;
            } else if (c == '"') {
                int j = i + 1;
                while (j < end && s.charAt(j) != '"' && s.charAt(j) != '\n') j += s.charAt(j) == '\\' ? 2 : 1;
                i = Math.min(j + 1, end);
            } else if (c == '\'' && i + 2 < end && (s.charAt(i + 2) == '\'' || s.charAt(i + 1) == '\\')) {
                int j = indexOf(s, "'", i + 2, end);
                i = j < 0 ? end : j + 1;
            } else {
                i++;
            }
        }
        // コードの中に無い: 単純に探す
        for (int k = from; k < end; k++) {
            if (s.charAt(k) == '<' && startsWithIgnoreCase(s, k, end, "</script")) return k;
        }
        return end;
    }

    private static boolean startsWith(CharSequence s, int at, int end, String p) {
        if (at + p.length() > end) return false;
        for (int k = 0; k < p.length(); k++) if (s.charAt(at + k) != p.charAt(k)) return false;
        return true;
    }

    private static boolean startsWithIgnoreCase(CharSequence s, int at, int end, String p) {
        if (at + p.length() > end) return false;
        for (int k = 0; k < p.length(); k++) if (Character.toLowerCase(s.charAt(at + k)) != p.charAt(k)) return false;
        return true;
    }

    private static int indexOf(CharSequence s, String p, int from, int end) {
        for (int k = from; k + p.length() <= end; k++) if (startsWith(s, k, end, p)) return k;
        return -1;
    }

}
