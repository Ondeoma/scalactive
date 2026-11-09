package io.github.ondeoma.scalactive.intellij;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.refactoring.listeners.RefactoringElementListenerProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * .stのファイル名(=コンポーネントの名前)を変えたときに、同じディレクトリ(パッケージ)の.stのテンプレートにある
 * そのコンポーネントのタグ(&lt;Old ...&gt; と &lt;/Old&gt;)を新しい名前に書き換えます.
 * (.scalaから生成されたobjectを参照している箇所は書き換えません)
 */
public final class ScalactiveRenameListenerProvider implements RefactoringElementListenerProvider {

    static boolean isComponentFile(@Nullable PsiElement e) {
        return e instanceof PsiFile f && f.getName().endsWith(".st");
    }

    private static String componentName(String fileName) {
        return fileName.substring(0, fileName.length() - ".st".length());
    }

    @Override
    public @Nullable RefactoringElementListener getListener(PsiElement element) {
        if (!isComponentFile(element)) return null;
        PsiFile file = (PsiFile) element;
        String oldName = componentName(file.getName());
        PsiDirectory dir = file.getContainingDirectory();
        if (dir == null) return null;
        return new RefactoringElementListener() {
            @Override
            public void elementMoved(@NotNull PsiElement newElement) {
                // 別のパッケージへの移動では、タグの書き換えはしない(importが必要になるため)
            }

            @Override
            public void elementRenamed(@NotNull PsiElement newElement) {
                if (!isComponentFile(newElement)) return;
                String newName = componentName(((PsiFile) newElement).getName());
                if (newName.equals(oldName) || !newName.matches("[A-Z][A-Za-z0-9_]*")) return;
                com.intellij.openapi.application.WriteAction.run(() -> renameTags(dir, oldName, newName));
            }
        };
    }

    /** dirの.stのテンプレートにある &lt;oldName と &lt;/oldName を newName に書き換えます */
    static void renameTags(@NotNull PsiDirectory dir, @NotNull String oldName, @NotNull String newName) {
        PsiDocumentManager pdm = PsiDocumentManager.getInstance(dir.getProject());
        Pattern tag = Pattern.compile("(</?)" + Pattern.quote(oldName) + "(?![\\w.-])");
        for (PsiFile f : dir.getFiles()) {
            if (!f.getName().endsWith(".st")) continue;
            Document doc = pdm.getDocument(f);
            if (doc == null) continue;
            String text = doc.getText();
            int start = text.indexOf("<template");
            int end = text.lastIndexOf("</template>");
            if (start < 0 || end < start) continue;
            Matcher m = tag.matcher(text).region(start, end);
            List<int[]> ranges = new ArrayList<>();
            while (m.find()) ranges.add(new int[]{m.start() + m.group(1).length(), m.end()});
            // 後ろから置き換えて位置がずれないようにする
            for (int i = ranges.size() - 1; i >= 0; i--) doc.replaceString(ranges.get(i)[0], ranges.get(i)[1], newName);
            if (!ranges.isEmpty()) pdm.commitDocument(doc);
        }
    }

}
