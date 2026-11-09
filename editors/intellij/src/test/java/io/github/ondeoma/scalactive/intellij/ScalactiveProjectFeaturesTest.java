package io.github.ondeoma.scalactive.intellij;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * プロジェクトの.stを扱う機能(新規作成のひな形・名前の変更・scalactive-lsp.json・生成コード)のテスト.
 */
public class ScalactiveProjectFeaturesTest extends BasePlatformTestCase {

    private static final Path DOC = Paths.get("../../doc").toAbsolutePath().normalize();
    private static final Path SAMPLE = DOC.resolve(
            "src/main/scalactive/io/github/ondeoma/scalactive/doc/components/contents/st/StSample.st");

    public void testNewComponentTemplates() throws Exception {
        PsiDirectory dir = myFixture.getPsiManager().findDirectory(myFixture.getTempDirFixture().findOrCreateDir("app"));
        assertNotNull(dir);
        for (String name : new String[]{NewScalactiveComponentAction.BASIC, NewScalactiveComponentAction.PROPS, NewScalactiveComponentAction.STATEFUL}) {
            FileTemplate t = FileTemplateManager.getInstance(getProject()).getInternalTemplate(name);
            assertNotNull(name, t);
            assertEquals("st", t.getExtension());
            String text = t.getText();
            assertTrue(name, text.contains("<script lang=\"scala\">") && text.contains("<template>") && text.contains("<style scoped>"));
        }
        PsiFile created = com.intellij.openapi.command.WriteCommandAction.writeCommandAction(getProject()).compute(() ->
            (PsiFile) com.intellij.ide.fileTemplates.FileTemplateUtil.createFromTemplate(
                FileTemplateManager.getInstance(getProject()).getInternalTemplate(NewScalactiveComponentAction.PROPS), "UserCard", null, dir));
        assertEquals("UserCard.st", created.getName());
        assertSame(ScalactiveFileType.INSTANCE, created.getFileType());
        assertTrue(created.getText(), created.getText().contains("case class Props(title: String"));
    }

    public void testRenameUpdatesTags() {
        PsiFile card = myFixture.addFileToProject("app/Card.st", "<script lang=\"scala\">\ncase class Props(title: String)\n</script>\n<template><div>{{ props.title }}</div></template>\n");
        PsiFile use = myFixture.addFileToProject("app/Use.st", "<script lang=\"scala\">\n// <Card> はスクリプトの中なので変えない\n</script>\n<template>\n  <div>\n    <Card title=\"a\" />\n    <Card title=\"b\"></Card>\n    <CardList />\n  </div>\n</template>\n");
        PsiFile other = myFixture.addFileToProject("other/Use2.st", "<template><Card title=\"c\" /></template>\n");
        myFixture.renameElement(card, "Panel.st");
        String text = use.getText();
        assertTrue(text, text.contains("<Panel title=\"a\" />") && text.contains("<Panel title=\"b\"></Panel>"));
        assertTrue("似た名前のタグは変えない: " + text, text.contains("<CardList />"));
        assertTrue("スクリプトの中は変えない: " + text, text.contains("// <Card>"));
        assertTrue("別のパッケージは変えない", other.getText().contains("<Card title=\"c\" />"));
    }

    public void testProjectConfigAndGeneratedSource() {
        assertTrue("run 'sbt doc/compile' first", Files.isRegularFile(DOC.resolve("target/scalactive-lsp.json")));
        var config = ScalactiveProjectConfig.find(SAMPLE);
        assertTrue(config.isPresent());
        Path generated = config.get().generatedSource(SAMPLE);
        assertNotNull(generated);
        assertTrue(generated.toString(), generated.toString().endsWith("src_managed/main/scalactive/io/github/ondeoma/scalactive/doc/components/contents/st/StSample.scala"));
        assertTrue(generated.toString(), Files.isRegularFile(generated));
        assertEquals(generated, OpenGeneratedSourceAction.generatedSourceOf(SAMPLE));
        assertEquals(DOC.getParent(), ScalactiveProjectConfig.sbtRoot(SAMPLE));
    }

    public void testNoProjectConfigOutsideSbtProject() throws Exception {
        Path dir = Files.createTempDirectory("scalactive");
        Path st = Files.writeString(dir.resolve("A.st"), "<template><p></p></template>\n");
        assertTrue(ScalactiveProjectConfig.find(st).isEmpty());
        assertNull(OpenGeneratedSourceAction.generatedSourceOf(st));
        assertNull(ScalactiveProjectConfig.sbtRoot(st));
    }

}
