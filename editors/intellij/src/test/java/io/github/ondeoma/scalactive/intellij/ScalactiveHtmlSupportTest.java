package io.github.ondeoma.scalactive.intellij;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.List;

/**
 * .stをHTMLの方言として解析し、IntelliJのHTML・CSSの機能が使えることを確認します.
 */
public class ScalactiveHtmlSupportTest extends BasePlatformTestCase {

    public void testParsedAsHtml() {
        PsiFile f = myFixture.configureByText("A.st", "<script lang=\"scala\">val a = 1</script>\n<template><div class=\"x\"><p>hi</p></div></template>\n<style scoped>.x { color: red; }</style>\n");
        assertTrue(f instanceof XmlFile);
        assertEquals(List.of("script", "template", "style"), rootTags(f));
    }

    private static List<String> rootTags(PsiFile f) {
        return PsiTreeUtil.getChildrenOfTypeAsList(((XmlFile) f).getDocument(), XmlTag.class).stream().map(XmlTag::getName).toList();
    }

    public void testScriptIsNotJavaScript() {
        PsiFile f = myFixture.configureByText("A.st", "<script lang=\"scala\">\ncase class Props(title: String)\ndef f(s: State): Unit = s.count := 1\n</script>\n<template><p></p></template>\n");
        // Scalaのコードが、JavaScriptやHTMLのタグとして解析されていない
        PsiElement at = f.findElementAt(f.getText().indexOf("case class"));
        String langs = PsiTreeUtil.collectParents(at, PsiElement.class, true, e -> e instanceof PsiFile).stream()
            .map(e -> e.getLanguage().getID()).distinct().toList().toString();
        assertFalse("script was parsed as " + langs, langs.contains("JavaScript") || langs.contains("ECMA"));
        assertEquals(List.of("script", "template"), rootTags(f));
    }

    public void testHtmlTagCompletion() {
        myFixture.configureByText("A.st", "<template>\n  <butt<caret>\n</template>\n");
        myFixture.completeBasic();
        List<String> items = myFixture.getLookupElementStrings();
        assertTrue("items: " + items, items == null || items.contains("button"));
        if (items == null) assertTrue(myFixture.getEditor().getDocument().getText().contains("<button"));
    }

    public void testHtmlAttributeCompletion() {
        myFixture.configureByText("A.st", "<template>\n  <a hre<caret>></a>\n</template>\n");
        myFixture.completeBasic();
        List<String> items = myFixture.getLookupElementStrings();
        if (items != null) assertTrue("items: " + items, items.contains("href"));
        else assertTrue(myFixture.getEditor().getDocument().getText().contains("href"));
    }

    public void testCloseTagIsInserted() {
        myFixture.configureByText("A.st", "<template>\n  <div<caret>\n</template>\n");
        myFixture.type(">");
        assertTrue(myFixture.getEditor().getDocument().getText(), myFixture.getEditor().getDocument().getText().contains("<div></div>"));
    }

    /**
     * テンプレートの中でEmmet(Tabキーでの展開)が使えること.
     * (ヘッドレスのテストでは、通常のHTMLファイルでも展開そのものが動かないため、Emmetが適用できることと略記の読み取りまでを確認します)
     */
    public void testEmmetIsApplicable() {
        myFixture.configureByText("A.st", "<template>\n  <div>\n    ul>li.item*2<caret>\n  </div>\n</template>\n");
        var callback = new com.intellij.codeInsight.template.CustomTemplateCallback(myFixture.getEditor(), myFixture.getFile());
        var emmet = new com.intellij.codeInsight.template.emmet.ZenCodingTemplate();
        assertTrue(emmet.isApplicable(callback, myFixture.getCaretOffset(), false));
        assertEquals("ul>li.item*2", emmet.computeTemplateKey(callback));
    }

    private String commentLine(String text) {
        myFixture.configureByText("A.st", text);
        myFixture.performEditorAction(com.intellij.openapi.actionSystem.IdeActions.ACTION_COMMENT_LINE);
        return myFixture.getEditor().getDocument().getText();
    }

    public void testCommentInScript() {
        String text = commentLine("<script lang=\"scala\">\nval a<caret> = 1\n</script>\n<template><p></p></template>\n");
        assertTrue(text, text.contains("//val a = 1"));
    }

    public void testCommentInTemplate() {
        String text = commentLine("<template>\n  <p>hi<caret></p>\n</template>\n");
        assertTrue(text, text.contains("<!--") && text.contains("<p>hi</p>-->"));
    }

    public void testCommentInStyle() {
        String text = commentLine("<template><p></p></template>\n<style scoped>\n.a { color: red; }<caret>\n</style>\n");
        assertTrue(text, text.contains("/*.a { color: red; }*/"));
    }

    public void testCssCompletionInStyle() {
        myFixture.configureByText("A.st", "<template><p class=\"x\"></p></template>\n<style scoped>\n.x { backgr<caret> }\n</style>\n");
        myFixture.completeBasic();
        List<String> items = myFixture.getLookupElementStrings();
        assertNotNull(items);
        assertTrue("items: " + items, items.contains("background-color"));
    }

}
