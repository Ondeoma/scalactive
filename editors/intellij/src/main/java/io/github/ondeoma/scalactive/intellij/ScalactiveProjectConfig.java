package io.github.ondeoma.scalactive.intellij;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * sbtプラグインが書き出す target/scalactive-lsp.json(言語サーバーが使うクラスパスなど)の、プラグインで使う部分.
 * (言語サーバーの ProjectConfig と同じ探し方をします)
 */
public record ScalactiveProjectConfig(Path file, List<Path> sourceRoots, Path generatedDirectory) {

    public static final String FILE_NAME = "scalactive-lsp.json";

    /** .stの親ディレクトリを遡り、そのファイルを含むプロジェクトの target/scalactive-lsp.json を探します */
    public static Optional<ScalactiveProjectConfig> find(@NotNull Path stFile) {
        Path abs = stFile.toAbsolutePath().normalize();
        for (Path dir = abs.getParent(); dir != null; dir = dir.getParent()) {
            Path f = dir.resolve("target").resolve(FILE_NAME);
            if (Files.isRegularFile(f)) {
                Optional<ScalactiveProjectConfig> c = load(f);
                if (c.isPresent() && c.get().sourceRootOf(abs) != null) return c;
            }
        }
        return Optional.empty();
    }

    static Optional<ScalactiveProjectConfig> load(@NotNull Path file) {
        try {
            JsonObject o = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            List<Path> roots = new ArrayList<>();
            JsonArray a = o.getAsJsonArray("sourceRoots");
            if (a != null) a.forEach(e -> roots.add(Paths.get(e.getAsString()).toAbsolutePath().normalize()));
            Path generated = Paths.get(o.get("generatedDirectory").getAsString()).toAbsolutePath().normalize();
            return Optional.of(new ScalactiveProjectConfig(file, roots, generated));
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    /** stFileを含むソースのルート(パッケージの起点. 複数あれば最も深いもの) */
    @Nullable Path sourceRootOf(@NotNull Path stFile) {
        Path best = null;
        for (Path r : sourceRoots) {
            if (stFile.startsWith(r) && (best == null || r.getNameCount() > best.getNameCount())) best = r;
        }
        return best;
    }

    /** .stから生成されるScalaのソース(sbtでコンパイルしていなければ存在しません) */
    public @Nullable Path generatedSource(@NotNull Path stFile) {
        Path abs = stFile.toAbsolutePath().normalize();
        Path root = sourceRootOf(abs);
        if (root == null) return null;
        String rel = root.relativize(abs).toString();
        return generatedDirectory.resolve(rel.replaceFirst("\\.st$", ".scala"));
    }

    /** stFileを含むsbtのビルドのルート(build.sbtのある、最も外側のディレクトリ) */
    public static @Nullable Path sbtRoot(@NotNull Path stFile) {
        Path found = null;
        for (Path dir = stFile.toAbsolutePath().normalize().getParent(); dir != null; dir = dir.getParent()) {
            if (Files.isRegularFile(dir.resolve("build.sbt"))) found = dir;
        }
        return found;
    }

}
