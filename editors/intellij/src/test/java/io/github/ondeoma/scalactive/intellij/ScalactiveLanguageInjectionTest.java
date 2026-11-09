package io.github.ondeoma.scalactive.intellij;

import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

/** scriptの中の // language=xxx に続く文字列への言語の注入 */
public class ScalactiveLanguageInjectionTest extends BasePlatformTestCase {

    private PsiFile injectedAt(PsiFile f, String marker) {
        int offset = f.getText().indexOf(marker);
        assertTrue("marker: " + marker, offset >= 0);
        PsiElement e = InjectedLanguageManager.getInstance(getProject()).findInjectedElementAt(f, offset);
        return e == null ? null : e.getContainingFile();
    }

    public void testCssWithStripMargin() {
        PsiFile f = myFixture.configureByText("A.st", """
            <script lang="scala">
            val css = // language=css
              ""\"|.box {
                 |  color: red;
                 |}
                 |""\".stripMargin
            </script>
            <template><p></p></template>
            """);
        PsiFile injected = injectedAt(f, "color");
        assertNotNull(injected);
        assertEquals("CSS", injected.getLanguage().getID());
        assertEquals(".box {\n  color: red;\n}\n", injected.getText());
    }

    public void testHtmlIsInjectedAsStWithoutInterpolations() {
        PsiFile f = myFixture.configureByText("A.st", """
            <script lang="scala">
            // language=html
            val html =
              s""\"<div class="x">${name}<span>$$1 $n</span></div>""\"
            </script>
            <template><p></p></template>
            """);
        PsiFile injected = injectedAt(f, "<span>");
        assertNotNull(injected);
        assertEquals(ScalactiveLanguage.INSTANCE, injected.getLanguage());
        assertEquals("<div class=\"x\"><span>$$1 </span></div>", injected.getText());
    }

    public void testAnyLanguageByIdIgnoringCase() {
        PsiFile f = myFixture.configureByText("A.st", """
            <script lang="scala">
            val js = // language=javascript
              ""\"const answer = 42""\"
            </script>
            <template><p></p></template>
            """);
        PsiFile injected = injectedAt(f, "answer");
        assertNotNull(injected);
        assertEquals("JavaScript", injected.getLanguage().getID());
    }

    public void testUnknownLanguageIsNotInjected() {
        PsiFile f = myFixture.configureByText("A.st", """
            <script lang="scala">
            val x = // language=nosuchlanguage
              ""\"abc""\"
            </script>
            <template><p></p></template>
            """);
        assertNull(injectedAt(f, "abc"));
    }

    public void testCommentNotFollowedByStringIsIgnored() {
        PsiFile f = myFixture.configureByText("A.st", """
            <script lang="scala">
            // language=css
            val n = 1
            val other = ""\".a { color: red; }""\"
            </script>
            <template><p></p></template>
            """);
        assertNull(injectedAt(f, "color"));
    }

    public void testCommentInsideStringIsIgnored() {
        PsiFile f = myFixture.configureByText("A.st", """
            <script lang="scala">
            val doc = ""\"// language=css""\"
            val other = ""\".a { color: red; }""\"
            </script>
            <template><p></p></template>
            """);
        assertNull(injectedAt(f, "color"));
    }

}
