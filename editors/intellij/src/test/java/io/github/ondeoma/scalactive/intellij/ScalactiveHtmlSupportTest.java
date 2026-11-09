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

    /** 言語サーバーが動いていないとき、IntelliJのHTMLの整形で.stを崩さない */
    public void testNoHtmlFormattingWithoutServer() {
        String text = "<script lang=\"scala\">\nval a = 1\n</script>\n<template>\n  <div>\n    <p><StField label=\"コンポーネントのv-model\" v-model=\"s.input\" /> → {{ s.input }} {{ s.input }} {{ s.input }} {{ s.input }}</p>\n  </div>\n</template>\n<style scoped>\ndiv{ margin: 8px; }\n</style>\n";
        myFixture.configureByText("A.st", text);
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(getProject(),
            () -> { com.intellij.psi.codeStyle.CodeStyleManager.getInstance(getProject()).reformat(myFixture.getFile()); });
        assertEquals(text, myFixture.getEditor().getDocument().getText());
    }

    /** 整形サービスを差し替えても、Enterでのインデント(前の行のインデントを引き継ぐ)は崩れない */
    public void testEnterIndentInTemplate() {
        myFixture.configureByText("A.st", "<template>\n  <div><caret></div>\n</template>\n");
        myFixture.type("\n");
        String text = myFixture.getEditor().getDocument().getText();
        assertTrue(text, text.contains("  <div>\n  </div>") || text.contains("  <div>\n    \n  </div>"));
    }

    private String enter(String text) {
        myFixture.configureByText("A.st", text);
        myFixture.type("\n");
        return myFixture.getEditor().getDocument().getText();
    }

    /** scriptの中の改行: 前の行のインデントを引き継ぐ(タグより1段深くしない) */
    public void testEnterInScriptKeepsIndent() {
        String text = enter("<script lang=\"scala\">\nval a = 10<caret>\n</script>\n<template><p></p></template>\n");
        assertTrue(text, text.contains("val a = 10\n\n</script>") || text.contains("val a = 10\n</script>"));
        myFixture.type("val b = 20");
        assertTrue(myFixture.getEditor().getDocument().getText(), myFixture.getEditor().getDocument().getText().contains("val a = 10\nval b = 20"));
    }

    public void testEnterInScriptBracketsAndBlocks() {
        // 閉じていない括弧の直後にそろえる
        enter("<script lang=\"scala\">\ncase class Props(title: String,<caret>\n</script>\n");
        myFixture.type("x: Int)");
        String text = myFixture.getEditor().getDocument().getText();
        assertTrue(text, text.contains("case class Props(title: String,\n                 x: Int)"));
        // 括弧が閉じたら、開いた行のインデントに戻す
        myFixture.type("\nval a = 1");
        text = myFixture.getEditor().getDocument().getText();
        assertTrue(text, text.contains("x: Int)\nval a = 1"));
        // { の後は1段深く
        enter("<script lang=\"scala\">\ndef f(s: State): Unit = {<caret>\n</script>\n");
        myFixture.type("g()");
        text = myFixture.getEditor().getDocument().getText();
        assertTrue(text, text.contains("= {\n  g()"));
    }

    public void testEnterInStyle() {
        enter("<template><p></p></template>\n<style scoped>\n.a {<caret>\n</style>\n");
        myFixture.type("color: red;\n}");
        String text = myFixture.getEditor().getDocument().getText();
        assertTrue(text, text.contains(".a {\n  color: red;\n}"));
    }

    public void testBracketPairsInScript() {
        myFixture.configureByText("A.st", "<script lang=\"scala\">\nval a = <caret>\n</script>\n");
        myFixture.type("f(");
        assertTrue(myFixture.getEditor().getDocument().getText(), myFixture.getEditor().getDocument().getText().contains("val a = f()\n"));
        myFixture.type("List[");
        myFixture.type("Int]");
        myFixture.type("(\"");
        String text = myFixture.getEditor().getDocument().getText();
        assertTrue(text, text.contains("val a = f(List[Int](\"\"))\n"));
        // 補った閉じる文字は上書きせずに通り過ぎる
        myFixture.type("x\"))");
        text = myFixture.getEditor().getDocument().getText();
        assertTrue(text, text.contains("val a = f(List[Int](\"x\"))\n"));
        // 単語の直前では補わない
        myFixture.configureByText("A.st", "<script lang=\"scala\">\nval a = <caret>foo\n</script>\n");
        myFixture.type("(");
        assertTrue(myFixture.getEditor().getDocument().getText(), myFixture.getEditor().getDocument().getText().contains("val a = (foo\n"));
    }

    public void testBackspaceDeletesEmptyPair() {
        myFixture.configureByText("A.st", "<script lang=\"scala\">\nval a = <caret>\n</script>\n");
        myFixture.type("{");
        myFixture.performEditorAction(com.intellij.openapi.actionSystem.IdeActions.ACTION_EDITOR_BACKSPACE);
        assertTrue(myFixture.getEditor().getDocument().getText(), myFixture.getEditor().getDocument().getText().contains("val a = \n"));
    }

    public void testBracketPairsInStyle() {
        myFixture.configureByText("A.st", "<template><p></p></template>\n<style scoped>\n.a <caret>\n</style>\n");
        myFixture.type("{");
        String text = myFixture.getEditor().getDocument().getText();
        assertTrue(text, text.contains(".a {}\n"));
    }

    public void testCssCompletionInStyle() {
        myFixture.configureByText("A.st", "<template><p class=\"x\"></p></template>\n<style scoped>\n.x { backgr<caret> }\n</style>\n");
        myFixture.completeBasic();
        List<String> items = myFixture.getLookupElementStrings();
        assertNotNull(items);
        assertTrue("items: " + items, items.contains("background-color"));
    }

}
