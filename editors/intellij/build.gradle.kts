import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask

plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.20.0"
}

group = "io.github.ondeoma"
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // サポートする最小バージョン(sinceBuild)に対してコンパイルする
        intellijIdea(providers.gradleProperty("platformVersion"))
        bundledPlugin("org.jetbrains.plugins.textmate")
        testFramework(TestFrameworkType.Platform)
        // テストで、<style>のCSS・<script>がJavaScriptとして扱われないことを確かめる
        testBundledPlugins("com.intellij.css", "JavaScript")
    }
    testImplementation("junit:junit:4.13.2")
    // IntelliJ Platformのテストフレームワークが要求する(JUnit5 APIはJUnit4テストでも必要)
    testImplementation("org.opentest4j:opentest4j:1.3.0")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")
        // Marketplaceの「What's New」(change-notes.html)
        changeNotes = providers.fileContents(layout.projectDirectory.file("change-notes.html")).asText
        ideaVersion {
            sinceBuild = "252"
            untilBuild = provider { null }
        }
    }
    // JetBrains Marketplaceへの公開(./gradlew publishPlugin). トークンは環境変数 JETBRAINS_TOKEN で渡す
    publishing {
        token = providers.environmentVariable("JETBRAINS_TOKEN")
    }
    // プラグインの検証は手動で: ./gradlew verifyPlugin
    pluginVerification {
        ides {
            recommended()
        }
    }
}

tasks {
    // 新規作成のひな形は、VS Code拡張と同じもの(../vscode/templates)を使う
    processResources {
        from(layout.projectDirectory.dir("../vscode/templates")) {
            into("fileTemplates/internal")
            rename("^Component\\.st$", "Scalactive Component.st.ft")
            rename("^PropsComponent\\.st$", "Scalactive Props Component.st.ft")
            rename("^StatefulComponent\\.st$", "Scalactive Stateful Component.st.ft")
        }
        // .stのファイルのアイコンも、VS Code拡張と同じもの(../vscode/images)を使う. ダークテーマ用は st_dark.svg
        from(layout.projectDirectory.dir("../vscode/images")) {
            include("st-file.svg", "st-file-dark.svg")
            into("icons")
            rename("^st-file\\.svg$", "st.svg")
            rename("^st-file-dark\\.svg$", "st_dark.svg")
        }
    }

    // VS Code拡張(../vscode)の文法をそのままTextMateバンドルとして同梱する.
    // IntelliJのTextMateプラグインはVS Code拡張の形式(package.json)のバンドルを読めます.
    // (prepareSandbox / prepareTestSandbox などすべてのサンドボックスに配置する)
    withType<PrepareSandboxTask>().configureEach {
        from(layout.projectDirectory.dir("../vscode")) {
            include("package.json", "language-configuration.json", "syntaxes/**", "LICENSE")
            into(intellijPlatform.projectName.map { "$it/bundles/scalactive" })
        }
        from(layout.projectDirectory.dir("bundles/scala")) {
            into(intellijPlatform.projectName.map { "$it/bundles/scala" })
        }
        // 言語サーバー(事前にリポジトリ直下で sbt languageServer/assembly を実行)
        from(layout.projectDirectory.file("../../language-server/target/scala-2.12/scalactive-language-server.jar")) {
            into(intellijPlatform.projectName.map { "$it/server" })
        }
    }
}
