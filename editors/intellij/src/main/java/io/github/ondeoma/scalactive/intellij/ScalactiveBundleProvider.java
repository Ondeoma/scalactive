package io.github.ondeoma.scalactive.intellij;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.textmate.api.TextMateBundleProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * プラグインに同梱したTextMateバンドル(.stの文法と、埋め込み用のScalaの文法)をTextMateプラグインへ登録します.
 * バンドルはプラグインのインストールディレクトリ直下の bundles/ に配置されます(build.gradle.kts参照).
 */
public final class ScalactiveBundleProvider implements TextMateBundleProvider {

    static final String PLUGIN_ID = "io.github.ondeoma.scalactive-st";

    @Override
    public @NotNull List<PluginBundle> getBundles() {
        IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID));
        if (plugin == null) return List.of();
        Path dir = plugin.getPluginPath().resolve("bundles");
        List<PluginBundle> bundles = new ArrayList<>();
        add(bundles, "Scalactive", dir.resolve("scalactive"));
        add(bundles, "Scala (for Scalactive)", dir.resolve("scala"));
        return bundles;
    }

    private static void add(List<PluginBundle> bundles, String name, Path path) {
        if (Files.isDirectory(path)) bundles.add(new PluginBundle(name, path));
    }

}
