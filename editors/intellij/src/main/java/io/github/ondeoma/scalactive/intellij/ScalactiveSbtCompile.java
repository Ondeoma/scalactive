package io.github.ondeoma.scalactive.intellij;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.lsp.api.LspServerManager;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * target/scalactive-lsp.json がないとき(sbtでまだコンパイルしていないとき)の通知と、sbt compile の実行.
 * scalactive-lsp.json がないと、言語サーバーはScalaの補完・型エラーなどを出せません.
 */
public final class ScalactiveSbtCompile {

    private ScalactiveSbtCompile() {
    }

    private static final Key<Boolean> NOTIFIED = Key.create("scalactive.lspConfigMissingNotified");

    /** .stを開いたときに呼ばれる. scalactive-lsp.json がなければ、プロジェクトごとに1回だけ通知します */
    static void checkConfig(@NotNull Project project, @NotNull VirtualFile file) {
        if (file.getFileSystem().getNioPath(file) == null) return;
        Path path = file.toNioPath();
        if (ScalactiveProjectConfig.find(path).isPresent()) return;
        if (Boolean.TRUE.equals(project.getUserData(NOTIFIED))) return;
        project.putUserData(NOTIFIED, true);
        Path root = ScalactiveProjectConfig.sbtRoot(path);
        Notification n = NotificationGroupManager.getInstance().getNotificationGroup("Scalactive").createNotification(
            "Scalactive: target/" + ScalactiveProjectConfig.FILE_NAME + " がありません",
            "sbtでコンパイルするまで、.stのScalaの補完・型エラーは使えません(ScalactivePluginを有効にしたプロジェクトで sbt compile を実行してください).",
            NotificationType.WARNING);
        if (root != null) {
            n.addAction(NotificationAction.createSimpleExpiring("sbt compile を実行", () -> run(project, root)));
        }
        n.notify(project);
    }

    /** sbt compile をバックグラウンドで実行し、終わったら言語サーバーを再起動します */
    static void run(@NotNull Project project, @NotNull Path root) {
        new Task.Backgroundable(project, "sbt compile (Scalactive)", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("sbt compile in " + root);
                ProcessOutput out;
                try {
                    GeneralCommandLine cl = new GeneralCommandLine("sbt", "-batch", "compile")
                        .withWorkDirectory(root.toFile())
                        .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE);
                    out = new CapturingProcessHandler(cl).runProcessWithProgressIndicator(indicator);
                } catch (ExecutionException e) {
                    ScalactiveSbtCompile.notifyMessage(project, "sbtを起動できませんでした: " + e.getMessage(), NotificationType.ERROR);
                    return;
                }
                if (out.isCancelled()) return;
                if (out.getExitCode() == 0) {
                    project.putUserData(NOTIFIED, null);
                    LspServerManager.getInstance(project).stopAndRestartIfNeeded(ScalactiveLspServerSupportProvider.class);
                    ScalactiveSbtCompile.notifyMessage(project, "sbt compile が終わりました. 言語サーバーを再起動しました.", NotificationType.INFORMATION);
                } else {
                    String tail = String.join("\n", out.getStdoutLines().subList(Math.max(0, out.getStdoutLines().size() - 10), out.getStdoutLines().size()));
                    ScalactiveSbtCompile.notifyMessage(project, "sbt compile が失敗しました(終了コード " + out.getExitCode() + ").\n" + tail, NotificationType.ERROR);
                }
            }
        }.queue();
    }

    static void notifyMessage(Project project, String message, NotificationType type) {
        NotificationGroupManager.getInstance().getNotificationGroup("Scalactive").createNotification(message, type).notify(project);
    }

}
