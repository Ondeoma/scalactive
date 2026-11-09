package io.github.ondeoma.scalactive.intellij;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Optional;

/**
 * .stから生成されたScalaのソース(target/scala-*&#47;src_managed/main/scalactive/...)を開きます.
 * コンパイルエラーの原因を生成コードで確かめるときに使います.
 */
public final class OpenGeneratedSourceAction extends AnAction implements DumbAware {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        VirtualFile f = e.getData(CommonDataKeys.VIRTUAL_FILE);
        e.getPresentation().setEnabledAndVisible(e.getProject() != null && f != null && "st".equals(f.getExtension()));
    }

    /** stFileの生成コード(sbtでコンパイルしていなければnull) */
    static @Nullable Path generatedSourceOf(@NotNull Path stFile) {
        Optional<ScalactiveProjectConfig> c = ScalactiveProjectConfig.find(stFile);
        return c.map(cfg -> cfg.generatedSource(stFile)).orElse(null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile f = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (project == null || f == null || f.getFileSystem().getNioPath(f) == null) return;
        Path generated = generatedSourceOf(f.toNioPath());
        VirtualFile g = generated == null ? null : LocalFileSystem.getInstance().refreshAndFindFileByNioFile(generated);
        if (g == null) {
            NotificationGroupManager.getInstance().getNotificationGroup("Scalactive").createNotification(
                "生成コードが見つかりません. sbtでコンパイルしてください" + (generated == null ? "" : " (" + generated + ")"),
                NotificationType.WARNING).notify(project);
            return;
        }
        new OpenFileDescriptor(project, g).navigate(true);
    }

}
