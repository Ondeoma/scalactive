package io.github.ondeoma.scalactive.intellij;

import com.intellij.ide.FileIconProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

/**
 * .stファイルのアイコン(プロジェクトビュー・エディターのタブなど).
 * .stはTextMateのファイルタイプとして開かれるため、ファイルタイプではなくFileIconProviderでアイコンを付けます.
 * ダークテーマでは icons/st_dark.svg が自動で使われます.
 */
public final class ScalactiveFileIconProvider implements FileIconProvider {

    static final Icon ICON = IconLoader.getIcon("/icons/st.svg", ScalactiveFileIconProvider.class);

    @Override
    public @Nullable Icon getIcon(@NotNull VirtualFile file, int flags, @Nullable Project project) {
        return "st".equals(file.getExtension()) ? ICON : null;
    }

}
