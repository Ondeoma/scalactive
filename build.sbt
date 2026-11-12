ThisBuild / description := "A frontend framework for Scala.js"
ThisBuild / organization := "io.github.ondeoma"
ThisBuild / version := "0.4.3"
ThisBuild / scalaVersion := "3.3.7"
ThisBuild / homepage := Some(url("https://github.com/Ondeoma/scalactive"))
ThisBuild / licenses := List("MIT" -> url("https://opensource.org/licenses/MIT"))
ThisBuild / developers := List(
  Developer(
    "kagenaga-ondeoma",
    "Shogo Kagenaga",
    "kagenaga@ondeoma.com",
    url("https://github.com/kagenaga-ondeoma")
  )
)
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/Ondeoma/scalactive"),
    "scm:git:https://github.com/Ondeoma/scalactive.git",
    Some("scm:git:ssh://git@github.com:Ondeoma/scalactive.git")
  )
)
ThisBuild / publishMavenStyle := true
ThisBuild / versionScheme := Some("early-semver")

lazy val domV = "2.8.0"
lazy val scala212 = "2.12.20"
lazy val catsV = "2.13.0"
lazy val munitV = "1.0.4"

lazy val core = project.in(file("core"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "scalactive",
    scalaJSUseMainModuleInitializer := false,
    // scalaJSLinkerConfig ~= {
    //   _.withModuleKind(ModuleKind.ESModule)
    // },
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % domV,
      "org.typelevel" %%% "cats-core" % catsV,
      "org.scalameta" %%% "munit" % munitV % Test,
    ),
    // DOMを使うテストのため、jsdom上で実行します(事前にリポジトリ直下で npm install が必要).
    Test / jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv(),
    // テストがScalactiveConfig(描き直しの待ち時間など)を書き換えるため、スイートを並行に実行しない
    Test / parallelExecution := false,
    scalacOptions ++= Seq(
      "-Wconf:msg=Implicit parameters should be provided with a `using` clause:s"
    )
  )

// .st(単一ファイルコンポーネント)のパーサー/コード生成器. sbtプラグインから使うためScala 2.12.
lazy val compiler = project.in(file("compiler"))
  .settings(
    name := "scalactive-compiler",
    scalaVersion := scala212,
    libraryDependencies += "org.scalameta" %% "munit" % munitV % Test
  )

lazy val sbtScalactive = project.in(file("sbt-plugin"))
  .enablePlugins(SbtPlugin)
  .dependsOn(compiler)
  .settings(
    name := "sbt-scalactive",
    scalaVersion := scala212,
    // sbt scripted でsrc/sbt-test配下のテストプロジェクトを実行します.
    scriptedLaunchOpts ++= Seq("-Xmx1024M", "-Dplugin.version=" + version.value),
    scriptedBufferLog := false,
    scriptedDependencies := scriptedDependencies.dependsOn(compiler / publishLocal).value
  )

// .stの言語サーバー(LSP). compilerを直接使うためScala 2.12.
// Scala 3のpresentation compilerは、対象プロジェクトのScalaバージョンのものを実行時に取得し、別のクラスローダーで動かします.
lazy val languageServer = project.in(file("language-server"))
  .dependsOn(compiler)
  .settings(
    name := "scalactive-language-server",
    scalaVersion := scala212,
    // lsp4jのインターフェースのデフォルトメソッドへの転送メソッドを生成しない
    // (生成するとアノテーションが重複し、lsp4jが"Duplicate RPC method"で起動に失敗する)
    scalacOptions += "-Xmixin-force-forwarders:false",
    // エディタ拡張に同梱して配布します(sbt languageServer/assembly)
    publish / skip := true,
    libraryDependencies ++= Seq(
      "org.eclipse.lsp4j" % "org.eclipse.lsp4j" % "0.24.0",
      "org.scalameta" % "mtags-interfaces" % "1.6.2",
      "io.get-coursier" % "interface" % "1.0.18",
      // <script>の整形(scalafmt本体は、設定のバージョンのものを実行時に取得します)
      "org.scalameta" %% "scalafmt-dynamic" % "3.11.5",
      "org.scalameta" %% "munit" % munitV % Test
    ),
    // テストでは、coreのクラスパスで.stを型検査します
    Test / resourceGenerators += Def.task {
      val cp = (Test / resourceManaged).value / "core-classpath.txt"
      IO.write(cp, (core / Compile / fullClasspath).value.map(_.data.getAbsolutePath).mkString("\n"))
      val srcs = (Test / resourceManaged).value / "core-sources.txt"
      IO.write(srcs, (core / Compile / unmanagedSourceDirectories).value.map(_.getAbsolutePath).mkString("\n"))
      Seq(cp, srcs)
    }.taskValue,
    Test / fork := true,
    Compile / sourceGenerators += Def.task {
      val f = (Compile / sourceManaged).value / "BuildInfo.scala"
      IO.write(f,
        s"""package io.github.ondeoma.scalactive.ls
           |
           |object BuildInfo {
           |  val version: String = "${version.value}"
           |}
           |""".stripMargin)
      Seq(f)
    }.taskValue,
    assembly / assemblyJarName := "scalactive-language-server.jar",
    assembly / mainClass := Some("io.github.ondeoma.scalactive.ls.Main"),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "versions", _, "module-info.class") => MergeStrategy.discard
      case "module-info.class" => MergeStrategy.discard
      case PathList("META-INF", xs @ _*) if xs.lastOption.exists(n => n.endsWith(".SF") || n.endsWith(".DSA") || n.endsWith(".RSA")) =>
        MergeStrategy.discard
      // scalafmt-dynamicの依存(coursier・plexus)で重複する、動作に関係しない情報ファイル
      case "scala-collection-compat.properties" => MergeStrategy.first
      case PathList("META-INF", "sisu", _ @ _*) => MergeStrategy.discard
      case x => (assembly / assemblyMergeStrategy).value(x)
    }
  )

lazy val doc = project.in(file("doc"))
  .enablePlugins(ScalaJSPlugin, ScalactivePlugin)
  .dependsOn(core)
  .settings(
    name := "scalactive-doc",
    publish / skip := true,
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
    }
  )

// 性能計測用のアプリ(bench/README.md). fullLinkJSの出力をbench/run.mjsがヘッドレスChromeで実行します.
lazy val bench = project.in(file("bench"))
  .enablePlugins(ScalaJSPlugin, ScalactivePlugin)
  .dependsOn(core)
  .settings(
    name := "scalactive-bench",
    publish / skip := true,
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
    }
  )

lazy val root = (project in file("."))
  .aggregate(core, compiler, sbtScalactive, languageServer, doc, bench)
  .settings(
    name := "scalactive-root",
    publish / skip := true
  )
