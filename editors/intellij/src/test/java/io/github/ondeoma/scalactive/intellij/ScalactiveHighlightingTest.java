package io.github.ondeoma.scalactive.intellij;

import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.psi.tree.IElementType;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jetbrains.plugins.textmate.TextMateFileType;
import org.jetbrains.plugins.textmate.TextMateService;
import org.jetbrains.plugins.textmate.language.TextMateLanguageDescriptor;
import org.jetbrains.plugins.textmate.language.syntax.lexer.TextMateElementType;
import org.jetbrains.plugins.textmate.language.syntax.lexer.TextMateScope;

import java.util.ArrayList;
import java.util.List;

/**
 * IntelliJ上で、同梱したバンドルによって.stファイルがハイライトされることを確認します.
 */
public class ScalactiveHighlightingTest extends BasePlatformTestCase {

    private static final String SAMPLE = String.join("\n",
        "<script lang=\"scala\">",
        "case class State(count: RV[Int] = RV(0))",
        "</script>",
        "<template>",
        "  <p v-if=\"ok\" @click.stop=\"s.count := 1\">{{ s.count }}</p>",
        "  <MyCard title=\"t\" />",
        "</template>",
        "");

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TextMateService.getInstance().reloadEnabledBundles();
        // バンドルの読み込みは非同期に終わることがあるため、.stの文法が登録されるまで待つ
        long end = System.currentTimeMillis() + 30_000;
        while (TextMateService.getInstance().getLanguageDescriptorByExtension("st") == null
                && System.currentTimeMillis() < end) {
            com.intellij.testFramework.PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            Thread.sleep(100);
        }
    }

    public void testBundlesAreProvided() {
        assertEquals(2, new ScalactiveBundleProvider().getBundles().size());
    }

    public void testStExtensionIsAssociated() {
        TextMateLanguageDescriptor d = TextMateService.getInstance().getLanguageDescriptorByExtension("st");
        assertNotNull("no TextMate grammar for .st", d);
        assertEquals("source.scalactive", d.getScopeName().toString());
    }

    public void testHighlighting() {
        myFixture.configureByText("Sample.st", SAMPLE);
        assertTrue(myFixture.getFile().getFileType() instanceof TextMateFileType);

        assertScope("case", "meta.embedded.block.scala");
        assertScope("case", "keyword");
        assertScope("v-if", "entity.other.attribute-name.directive.scalactive");
        assertScope(".stop", "entity.other.attribute-name.modifier.scalactive");
        assertScope(":=", "meta.embedded.inline.scala");
        assertScope("{{", "punctuation.definition.interpolation.begin.scalactive");
        assertScope("MyCard", "support.class.component.scalactive");
    }

    /** textの先頭位置のトークンのスコープ(親を含む)に、scopeで始まるものがあることを確認します. */
    private void assertScope(String text, String scope) {
        int offset = SAMPLE.indexOf(text);
        assertTrue("not found: " + text, offset >= 0);
        List<String> scopes = scopesAt(offset);
        assertTrue("'" + text + "' should have scope " + scope + " but was " + scopes,
            scopes.stream().anyMatch(s -> s.equals(scope) || s.startsWith(scope + ".")));
    }

    private List<String> scopesAt(int offset) {
        HighlighterIterator it = ((EditorEx) myFixture.getEditor()).getHighlighter().createIterator(offset);
        IElementType type = it.getTokenType();
        List<String> scopes = new ArrayList<>();
        if (type instanceof TextMateElementType t) {
            for (TextMateScope s = t.getScope(); s != null && !s.isEmpty(); s = s.getParent()) {
                if (s.getScopeName() != null) scopes.add(s.getScopeName().toString());
            }
        }
        return scopes;
    }

}
