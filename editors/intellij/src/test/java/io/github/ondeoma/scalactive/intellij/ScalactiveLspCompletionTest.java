package io.github.ondeoma.scalactive.intellij;

import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.lsp.api.LspServer;
import com.intellij.platform.lsp.api.LspServerManager;
import com.intellij.platform.lsp.api.LspServerState;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * IntelliJのLSPクライアントを通した補完のテスト.
 * リポジトリのdocプロジェクトのサンプル(.st)を開き、IntelliJの補完で言語サーバーの候補が出ることを確認します.
 * 事前にリポジトリ直下で sbt languageServer/assembly doc/compile を実行してください.
 */
public class ScalactiveLspCompletionTest extends BasePlatformTestCase {

    private static final Path DOC = Paths.get("../../doc").toAbsolutePath().normalize();
    private static final Path SAMPLE = DOC.resolve(
            "src/main/scalactive/io/github/ondeoma/scalactive/doc/components/contents/st/StSample.st");

    public void testCompletionFromLanguageServer() throws Exception {
        assertTrue("run 'sbt doc/compile' first", Files.isRegularFile(DOC.resolve("target/scalactive-lsp.json")));
        // .stのあるディレクトリだけをコンテンツルートにする(docのpackage.jsonなどを他のプラグインに読ませないため)
        VirtualFile docDir = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(SAMPLE.getParent());
        VirtualFile sample = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(SAMPLE);
        assertNotNull(sample);
        PsiTestUtil.addContentRoot(getModule(), docDir);
        try {
            myFixture.configureFromExistingVirtualFile(sample);
            LspServerManager manager = LspServerManager.getInstance(getProject());
            manager.ensureServerStarted(ScalactiveLspServerSupportProvider.class, new ScalactiveLspServerDescriptor(getProject()));
            waitUntil("server running", () -> manager.getServersForProvider(ScalactiveLspServerSupportProvider.class).stream()
                    .anyMatch(s -> s.getState() == LspServerState.Running));

            String text = myFixture.getEditor().getDocument().getText();
            int offset = text.indexOf("{{ s.count }}") + "{{ s.".length();
            myFixture.getEditor().getCaretModel().moveToOffset(offset);
            // presentation compilerの初回読み込みを待つため、候補が出るまで繰り返す
            List<String>[] lookup = new List[1];
            waitUntil("completion", () -> {
                myFixture.completeBasic();
                lookup[0] = myFixture.getLookupElementStrings();
                return lookup[0] != null && lookup[0].stream().anyMatch(s -> s.startsWith("count"));
            });
            assertTrue(lookup[0].toString(), lookup[0].stream().anyMatch(s -> s.startsWith("cardOpen")));
        } finally {
            LspServerManager.getInstance(getProject()).stopServers(ScalactiveLspServerSupportProvider.class);
            PsiTestUtil.removeContentEntry(getModule(), docDir);
        }
    }

    private static void waitUntil(String what, java.util.function.BooleanSupplier condition) throws Exception {
        long end = System.currentTimeMillis() + 180_000;
        while (System.currentTimeMillis() < end) {
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            if (condition.getAsBoolean()) return;
            Thread.sleep(500);
        }
        fail("timed out waiting for " + what);
    }

}
