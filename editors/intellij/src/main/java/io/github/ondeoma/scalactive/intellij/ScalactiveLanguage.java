package io.github.ondeoma.scalactive.intellij;

import com.intellij.lang.html.HTMLLanguage;

/**
 * .stの言語. HTMLの方言として扱い、IntelliJのHTMLの機能(Emmet・タグの補完・閉じタグの挿入・&lt;style&gt;のCSS)を使えるようにします.
 * Scalaの部分(script・{{ }}・ディレクティブの値)の補完などは言語サーバーが受け持ちます.
 */
public final class ScalactiveLanguage extends HTMLLanguage {

    public static final ScalactiveLanguage INSTANCE = new ScalactiveLanguage();

    private ScalactiveLanguage() {
        super(HTMLLanguage.INSTANCE, "Scalactive");
    }

}
