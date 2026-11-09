package io.github.ondeoma.scalactive.intellij;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.lsp.api.LspServerSupportProvider;
import org.jetbrains.annotations.NotNull;

/**
 * .stファイルを開いたときに、Scalactiveの言語サーバー(補完・型エラー・ホバー・定義ジャンプ)を起動します.
 */
public final class ScalactiveLspServerSupportProvider implements LspServerSupportProvider {

    @Override
    public void fileOpened(@NotNull Project project, @NotNull VirtualFile file, @NotNull LspServerStarter serverStarter) {
        if (ScalactiveLspServerDescriptor.isStFile(file)) {
            serverStarter.ensureServerStarted(new ScalactiveLspServerDescriptor(project));
            ScalactiveSbtCompile.checkConfig(project, file);
        }
    }

}
