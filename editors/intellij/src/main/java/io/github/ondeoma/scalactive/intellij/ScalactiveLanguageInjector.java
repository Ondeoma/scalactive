package io.github.ondeoma.scalactive.intellij;

import com.intellij.lang.Language;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.xml.XmlText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * .stの&lt;script&gt;の中で、// language=xxx のコメントに続く三重引用符の文字列に、その言語を注入します.
 * (Scalaのファイルで IntelliJ が行う言語の注入と同じで、IntelliJ が扱える言語ならどれでも指定でき、
 * 色付けだけでなく補完・検査なども効きます)
 * html / st / scalactive は.stとして注入します(テンプレートの {{ }} などもScalaとして色付けされます).
 * 言語が見つからない場合(Scalaプラグインが無いときの scala など)は注入せず、TextMateの文法の色付けだけになります.
 */
public final class ScalactiveLanguageInjector implements MultiHostInjector {

    @Override
    public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {
        PsiFile file = context.getContainingFile();
        if (file == null || file.getLanguage() != ScalactiveLanguage.INSTANCE) return;
        if (!(context instanceof PsiLanguageInjectionHost host) || !host.isValidHost()) return;
        TextRange hostRange = context.getTextRange();
        CharSequence text = file.getViewProvider().getContents();
        for (ScalaScan.Injection injection : ScalaScan.languageInjections(text)) {
            List<TextRange> ranges = injection.ranges();
            if (ranges.isEmpty() || !hostRange.contains(ranges.get(0))) continue;
            Language language = findLanguage(injection.languageId());
            if (language == null) continue;
            registrar.startInjecting(language);
            for (TextRange r : ranges) {
                if (hostRange.contains(r)) registrar.addPlace(null, null, host, r.shiftLeft(hostRange.getStartOffset()));
            }
            registrar.doneInjecting();
        }
    }

    @Override
    public @NotNull List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
        return List.of(XmlText.class);
    }

    /** 言語のIDか表示名(大文字・小文字を区別しない)で探します */
    static @Nullable Language findLanguage(String id) {
        String lower = id.toLowerCase(Locale.ROOT);
        if (lower.equals("html") || lower.equals("st") || lower.equals("scalactive")) return ScalactiveLanguage.INSTANCE;
        Language exact = Language.findLanguageByID(id);
        if (exact != null && exact != Language.ANY) return exact;
        for (Language l : Language.getRegisteredLanguages()) {
            if (l == Language.ANY) continue;
            if (l.getID().equalsIgnoreCase(id) || l.getDisplayName().equalsIgnoreCase(id)) return l;
        }
        return null;
    }

}
