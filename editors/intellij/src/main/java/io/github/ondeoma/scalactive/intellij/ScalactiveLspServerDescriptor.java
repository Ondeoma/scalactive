package io.github.ondeoma.scalactive.intellij;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor;
import com.intellij.platform.lsp.api.customization.LspFormattingSupport;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Scalactiveの言語サーバーの起動方法.
 * 言語サーバーはプラグインに同梱したjar(server/scalactive-language-server.jar)で、IDEに同梱のJava(JBR)で動かします.
 */
public final class ScalactiveLspServerDescriptor extends ProjectWideLspServerDescriptor {

    static final String JAR = "server/scalactive-language-server.jar";

    public ScalactiveLspServerDescriptor(@NotNull Project project) {
        super(project, "Scalactive");
    }

    static boolean isStFile(@NotNull VirtualFile file) {
        return "st".equals(file.getExtension());
    }

    @Override
    public boolean isSupportedFile(@NotNull VirtualFile file) {
        return isStFile(file);
    }

    @Override
    public @NotNull String getLanguageId(@NotNull VirtualFile file) {
        return "scalactive";
    }

    /**
     * テンプレートのHTMLのタグ・属性・属性値の補完はIntelliJ自身が出すため、言語サーバーには出させない
     * (子コンポーネント・prop・ディレクティブ・:attr・@eventの補完は言語サーバーが出す)
     */
    @Override
    public Object createInitializationOptions() {
        return java.util.Map.of("htmlCompletion", false);
    }

    /** .stの「コードの整形」は言語サーバー(textDocument/formatting)に任せる */
    @Override
    public @NotNull LspFormattingSupport getLspFormattingSupport() {
        return new LspFormattingSupport() {
            @Override
            public boolean shouldFormatThisFileExclusivelyByServer(@NotNull VirtualFile file, boolean ideCanFormatThisFileItself, boolean serverExplicitlyWantsToFormatThisFile) {
                return isStFile(file);
            }
        };
    }

    /** プラグインに同梱した言語サーバーのjar */
    static Path serverJar() throws ExecutionException {
        IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId(ScalactiveBundleProvider.PLUGIN_ID));
        if (plugin == null) throw new ExecutionException("Scalactive plugin is not installed");
        Path jar = plugin.getPluginPath().resolve(JAR);
        if (!Files.isRegularFile(jar)) throw new ExecutionException("Scalactive language server not found: " + jar);
        return jar;
    }

    /** IDEを動かしているJava */
    static Path javaExecutable() {
        String exe = System.getProperty("os.name").toLowerCase().startsWith("windows") ? "java.exe" : "java";
        return Paths.get(System.getProperty("java.home"), "bin", exe);
    }

    @Override
    public @NotNull GeneralCommandLine createCommandLine() throws ExecutionException {
        GeneralCommandLine cl = new GeneralCommandLine(
                javaExecutable().toString(), "-Xss4m", "-Xmx2G", "-jar", serverJar().toString()
        );
        String base = getProject().getBasePath();
        if (base != null && Files.isDirectory(Paths.get(base))) cl.withWorkDirectory(base);
        return cl;
    }

}
