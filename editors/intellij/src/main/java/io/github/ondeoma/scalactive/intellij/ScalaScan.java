package io.github.ondeoma.scalactive.intellij;

import com.intellij.openapi.util.TextRange;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * .stの&lt;script&gt;の中(Scala)を、文字列・コメントを区別しながら読みます.
 * (コンパイラーの SfcParser / StCompiler.codeOnly と同じ考え方)
 */
final class ScalaScan {

    private ScalaScan() {
    }

    /** &lt;script ...&gt; の開始タグ */
    private static final Pattern SCRIPT_START_TAG = Pattern.compile("<script\\b[^>]*>", Pattern.CASE_INSENSITIVE);

    /** 三重引用符の文字列の範囲(開きの """ から閉じの """ まで. 閉じていなければendまで)と、閉じているか */
    record TripleString(int start, int end, boolean closed) {
        TextRange range() {
            return new TextRange(start, end);
        }
    }

    /** ファイル全体の、&lt;script&gt;の中身の範囲 */
    static List<TextRange> scriptContents(CharSequence text) {
        List<TextRange> result = new ArrayList<>();
        Matcher m = SCRIPT_START_TAG.matcher(text);
        int from = 0;
        while (from < text.length() && m.find(from)) {
            int contentStart = m.end();
            int end = scriptEnd(text, contentStart, text.length());
            result.add(new TextRange(contentStart, end));
            from = Math.max(end, contentStart + 1);
        }
        return result;
    }

    /** offsetが&lt;script&gt;の中身の中か(閉じタグ &lt;/script の直前も含む) */
    static boolean inScript(CharSequence text, int offset) {
        for (TextRange r : scriptContents(text)) {
            if (offset >= r.getStartOffset() && offset <= r.getEndOffset()) return true;
        }
        return false;
    }

    /** offsetが&lt;style&gt;の中身の中か(&lt;script&gt;の中の文字列にある &lt;style は数えない) */
    static boolean inStyle(CharSequence text, int offset) {
        List<TextRange> scripts = scriptContents(text);
        int from = 0;
        while (true) {
            int open = indexOfOutside(text, "<style", from, scripts);
            if (open < 0 || open >= offset) return false;
            int tagEnd = indexOf(text, ">", open, text.length());
            if (tagEnd < 0) return false;
            int close = indexOf(text, "</style", tagEnd + 1, text.length());
            int end = close < 0 ? text.length() : close;
            if (offset > tagEnd && offset <= end) return true;
            if (close < 0) return false;
            from = close + 1;
        }
    }

    /** &lt;script&gt;の中身の外にある、最初のneedleの位置(無ければ-1) */
    static int indexOfOutsideScript(CharSequence text, String needle) {
        return indexOfOutside(text, needle, 0, scriptContents(text));
    }

    /** &lt;script&gt;の中身の外にある、最後のneedleの位置(無ければ-1) */
    static int lastIndexOfOutsideScript(CharSequence text, String needle) {
        List<TextRange> scripts = scriptContents(text);
        for (int k = text.length() - needle.length(); k >= 0; k--) {
            if (startsWith(text, k, text.length(), needle) && !inAny(scripts, k)) return k;
        }
        return -1;
    }

    private static int indexOfOutside(CharSequence text, String needle, int from, List<TextRange> ranges) {
        for (int k = from; k + needle.length() <= text.length(); k++) {
            if (startsWith(text, k, text.length(), needle) && !inAny(ranges, k)) return k;
        }
        return -1;
    }

    private static boolean inAny(List<TextRange> ranges, int k) {
        for (TextRange r : ranges) if (k >= r.getStartOffset() && k < r.getEndOffset()) return true;
        return false;
    }

    /** ファイル全体の、&lt;script&gt;の中の三重引用符の文字列 */
    static List<TripleString> tripleStrings(CharSequence text) {
        List<TripleString> result = new ArrayList<>();
        for (TextRange r : scriptContents(text)) scan(text, r.getStartOffset(), r.getEndOffset(), result);
        return result;
    }

    /**
     * scriptの中身の終わり(&lt;/script の位置). Scalaの文字列・コメント・文字リテラルの中は飛ばします.
     * コードの中に見つからなければ(閉じていない文字列など)単純に探します. どちらも無ければend.
     */
    static int scriptEnd(CharSequence s, int from, int end) {
        int found = scan(s, from, end, null);
        if (found >= 0) return found;
        for (int k = from; k < end; k++) {
            if (s.charAt(k) == '<' && startsWithIgnoreCase(s, k, end, "</script")) return k;
        }
        return end;
    }

    /**
     * [from, end) をScalaとして読み、文字列・コメントの外にある最初の &lt;/script の位置を返します(無ければ-1).
     * strings が null でなければ、そこまでの三重引用符の文字列を加えます.
     */
    private static int scan(CharSequence s, int from, int end, List<TripleString> strings) {
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
                int close = j < 0 ? end : j + 3;
                while (close < end && s.charAt(close) == '"') close++;
                if (strings != null) strings.add(new TripleString(i, close, j >= 0));
                i = close;
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
        return -1;
    }

    static boolean startsWith(CharSequence s, int at, int end, String p) {
        if (at + p.length() > end) return false;
        for (int k = 0; k < p.length(); k++) if (s.charAt(at + k) != p.charAt(k)) return false;
        return true;
    }

    static boolean startsWithIgnoreCase(CharSequence s, int at, int end, String p) {
        if (at + p.length() > end) return false;
        for (int k = 0; k < p.length(); k++) if (Character.toLowerCase(s.charAt(at + k)) != p.charAt(k)) return false;
        return true;
    }

    private static int indexOf(CharSequence s, String p, int from, int end) {
        for (int k = from; k + p.length() <= end; k++) if (startsWith(s, k, end, p)) return k;
        return -1;
    }

}
