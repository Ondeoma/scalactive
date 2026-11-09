package io.github.ondeoma.scalactive.intellij;

import com.intellij.formatting.FormattingContext;
import com.intellij.formatting.service.AbstractDocumentFormattingService;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.platform.lsp.api.LspServerManager;
import com.intellij.platform.lsp.api.LspServerState;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * .stをIntelliJのHTMLの整形で整形しないための整形サービス.
 * .stの整形は言語サーバーのもの(LspFormattingService)だけを使います. IntelliJのHTMLの整形は {{ }} を途中で改行するなど
 * .stを崩すため、言語サーバーが整形しないとき(起動中・停止中など)は、このサービスが何もせずに引き受けます.
 * (plugin.xmlで LspFormattingService の後、CoreFormattingService より前に登録しています)
 */
public final class ScalactiveFormattingGuard extends AbstractDocumentFormattingService {

    @Override
    public @NotNull Set<Feature> getFeatures() {
        return EnumSet.of(Feature.FORMAT_FRAGMENTS);
    }

    @Override
    public boolean canFormat(@NotNull PsiFile file) {
        return file.getLanguage() == ScalactiveLanguage.INSTANCE;
    }

    static boolean serverRunning(@NotNull Project project) {
        return LspServerManager.getInstance(project).getServersForProvider(ScalactiveLspServerSupportProvider.class).stream()
            .anyMatch(s -> s.getState() == LspServerState.Running);
    }

    @Override
    public void formatDocument(@NotNull Document document, @NotNull List<TextRange> formattingRanges,
                               @NotNull FormattingContext formattingContext, boolean canChangeWhiteSpaceOnly, boolean quickFormat) {
        Project project = formattingContext.getProject();
        if (!serverRunning(project)) {
            NotificationGroupManager.getInstance().getNotificationGroup("Scalactive")
                .createNotification(".stを整形できませんでした: Scalactiveの言語サーバーが起動していません(起動後にもう一度実行してください).",
                    NotificationType.WARNING)
                .notify(project);
        }
    }

}
