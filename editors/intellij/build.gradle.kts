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
        ideaVersion {
            sinceBuild = "252"
            untilBuild = provider { null }
        }
    }
    // プラグインの検証は手動で: ./gradlew verifyPlugin
    pluginVerification {
        ides {
            recommended()
        }
    }
}

tasks {
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
