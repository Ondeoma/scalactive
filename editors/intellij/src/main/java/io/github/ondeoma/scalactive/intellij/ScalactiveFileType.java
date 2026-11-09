package io.github.ondeoma.scalactive.intellij;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/** .stのファイルタイプ. ダークテーマでは icons/st_dark.svg が自動で使われます */
public final class ScalactiveFileType extends LanguageFileType {

    public static final ScalactiveFileType INSTANCE = new ScalactiveFileType();

    static final Icon ICON = IconLoader.getIcon("/icons/st.svg", ScalactiveFileType.class);

    private ScalactiveFileType() {
        super(ScalactiveLanguage.INSTANCE);
    }

    @Override
    public @NotNull String getName() {
        return "Scalactive";
    }

    @Override
    public @NotNull String getDescription() {
        return "Scalactive single file component";
    }

    @Override
    public @NotNull String getDefaultExtension() {
        return "st";
    }

    @Override
    public Icon getIcon() {
        return ICON;
    }

}
