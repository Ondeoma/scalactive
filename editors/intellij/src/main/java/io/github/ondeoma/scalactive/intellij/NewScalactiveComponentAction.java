package io.github.ondeoma.scalactive.intellij;

import com.intellij.ide.actions.CreateFileFromTemplateAction;
import com.intellij.ide.actions.CreateFileFromTemplateDialog;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import org.jetbrains.annotations.NotNull;

/**
 * New → Scalactive Component. ファイル名がコンポーネント(object)の名前になるため、大文字で始まる名前にします.
 * ひな形は resources/fileTemplates/internal/ にあります.
 */
public final class NewScalactiveComponentAction extends CreateFileFromTemplateAction implements DumbAware {

    static final String BASIC = "Scalactive Component";
    static final String PROPS = "Scalactive Props Component";
    static final String STATEFUL = "Scalactive Stateful Component";

    public NewScalactiveComponentAction() {
        super("Scalactive Component", "Create a Scalactive single file component (.st)", ScalactiveFileType.ICON);
    }

    @Override
    protected void buildDialog(@NotNull Project project, @NotNull PsiDirectory directory, CreateFileFromTemplateDialog.@NotNull Builder builder) {
        builder.setTitle("New Scalactive Component")
            .addKind("Component", ScalactiveFileType.ICON, BASIC)
            .addKind("Component with Props", ScalactiveFileType.ICON, PROPS)
            .addKind("Stateful component", ScalactiveFileType.ICON, STATEFUL)
            .setValidator(new com.intellij.openapi.ui.InputValidatorEx() {
                @Override
                public String getErrorText(String name) {
                    String n = name.trim().replaceFirst("\\.st$", "");
                    if (n.isEmpty()) return null;
                    if (!n.matches("[A-Z][A-Za-z0-9_]*")) return "コンポーネントの名前は大文字で始まる英数字にしてください(例: UserCard)";
                    return null;
                }

                @Override
                public boolean checkInput(String name) {
                    return getErrorText(name) == null && !name.trim().isEmpty();
                }

                @Override
                public boolean canClose(String name) {
                    return checkInput(name);
                }
            });
    }

    @Override
    protected String getActionName(PsiDirectory directory, @NotNull String newName, String templateName) {
        return "Create Scalactive component " + newName;
    }

}
