package io.github.ondeoma.scalactive.intellij;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.analysis.XmlUnboundNsPrefixInspection;
import com.intellij.codeInspection.htmlInspections.HtmlUnknownAttributeInspection;
import com.intellij.codeInspection.htmlInspections.HtmlUnknownBooleanAttributeInspection;
import com.intellij.codeInspection.htmlInspections.HtmlUnknownTagInspection;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * HTMLの検査が、.stの書き方(ディレクティブ・子コンポーネント)を警告しないこと、
 * 本当に間違ったHTMLは引き続き警告することを確認します.
 */
public class ScalactiveInspectionTest extends BasePlatformTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.enableInspections(new HtmlUnknownTagInspection(), new HtmlUnknownAttributeInspection(),
            new HtmlUnknownBooleanAttributeInspection(), new XmlUnboundNsPrefixInspection());
    }

    private List<String> problems(String name, String text) {
        myFixture.configureByText(name, text);
        return myFixture.doHighlighting().stream()
            .filter(h -> h.getSeverity().compareTo(HighlightSeverity.WEAK_WARNING) >= 0)
            .map(h -> h.getDescription() + " @" + text.substring(h.getStartOffset(), h.getEndOffset()))
            .toList();
    }

    /** ドキュメントサイトの.stファイルに警告・エラーが出ない */
    public void testDocFilesHaveNoProblems() throws IOException {
        Path dir = Path.of("../../doc/src/main/scalactive/io/github/ondeoma/scalactive/doc/components/contents/st");
        List<Path> files;
        try (Stream<Path> s = Files.list(dir)) {
            files = s.filter(p -> p.toString().endsWith(".st")).sorted().toList();
        }
        assertFalse(files.isEmpty());
        for (Path f : files) {
            String text = committed(f);
            assertEquals(f.getFileName().toString(), List.of(), problems(f.getFileName().toString(), text));
        }
    }

    /** コミット済みの内容(作業中の編集の影響を受けないように). gitが使えなければファイルの内容 */
    private static String committed(Path f) throws IOException {
        try {
            Process p = new ProcessBuilder("git", "show", "HEAD:./" + f.getFileName())
                .directory(f.getParent().toFile()).redirectErrorStream(false).start();
            byte[] out = p.getInputStream().readAllBytes();
            if (p.waitFor() == 0) return new String(out, StandardCharsets.UTF_8);
        } catch (IOException | InterruptedException ignored) {
        }
        return Files.readString(f, StandardCharsets.UTF_8);
    }

    public void testDirectivesAndComponents() {
        String text = String.join("\n",
            "<script lang=\"scala\">val a = 1</script>",
            "<template>",
            "  <div v-if=\"ok\" v-on:click.stop=\"f()\" v-bind:title=\"t\" :class=\"c\" @keydown.enter=\"g()\" ref=\"r\">",
            "    <input v-model.trim=\"s\">",
            "    <MyCard title=\"x\" :count=\"1\" @close=\"h()\" v-model:open=\"o\"><template #footer>f</template></MyCard>",
            "  </div>",
            "</template>",
            "<style scoped>.a { color: red; }</style>",
            "");
        assertEquals(List.of(), problems("A.st", text));
    }

    public void testRealHtmlMistakesAreStillReported() {
        String text = "<template>\n  <dvi foo=\"1\"></dvi>\n  <p unknownattr=\"1\"></p>\n</template>\n";
        List<String> problems = problems("A.st", text);
        assertTrue(problems.toString(), problems.stream().anyMatch(p -> p.endsWith("@dvi")));
        assertTrue(problems.toString(), problems.stream().anyMatch(p -> p.endsWith("@unknownattr")));
    }

}
