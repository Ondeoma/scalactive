package io.github.ondeoma.scalactive.intellij;

import com.intellij.codeInspection.InspectionSuppressor;
import com.intellij.codeInspection.SuppressQuickFix;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * .stのテンプレートの書き方を、HTMLの検査が「未知のタグ・属性」と警告しないようにします.
 * <ul>
 *   <li>ディレクティブ(v-if、v-on:click など)、:attr、@event、#slot と ref</li>
 *   <li>子コンポーネントのタグ(大文字で始まるもの)と、その属性(prop)</li>
 *   <li>&lt;script lang&gt;、&lt;style scoped&gt; / &lt;style lang&gt;</li>
 * </ul>
 * (propの名前の間違いなどは、言語サーバーが検査します)
 */
public final class ScalactiveInspectionSuppressor implements InspectionSuppressor {

    private static final Set<String> TOOLS = Set.of(
        "HtmlUnknownTag", "HtmlUnknownAttribute", "HtmlUnknownBooleanAttribute", "XmlUnboundNsPrefix");

    static boolean isComponentTag(@Nullable XmlTag tag) {
        if (tag == null) return false;
        String name = tag.getName();
        return !name.isEmpty() && Character.isUpperCase(name.charAt(0));
    }

    static boolean isDirective(@NotNull String name) {
        return name.startsWith("v-") || name.startsWith(":") || name.startsWith("@") || name.startsWith("#") || name.equals("ref");
    }

    @Override
    public boolean isSuppressedFor(@NotNull PsiElement element, @NotNull String toolId) {
        if (!TOOLS.contains(toolId)) return false;
        XmlAttribute attr = PsiTreeUtil.getParentOfType(element, XmlAttribute.class, false);
        XmlTag tag = attr != null ? attr.getParent() : PsiTreeUtil.getParentOfType(element, XmlTag.class, false);
        if (isComponentTag(tag)) return true;
        if (attr == null || tag == null) return false;
        String name = attr.getName();
        if (isDirective(name)) return true;
        return switch (tag.getName()) {
            case "script" -> name.equals("lang");
            case "style" -> name.equals("lang") || name.equals("scoped");
            default -> false;
        };
    }

    @Override
    public SuppressQuickFix @NotNull [] getSuppressActions(@Nullable PsiElement element, @NotNull String toolId) {
        return SuppressQuickFix.EMPTY_ARRAY;
    }

}
