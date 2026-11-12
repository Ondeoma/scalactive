addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.21.0")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.11.2")
// 言語サーバーを実行可能なjarにまとめるため
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.3.1")

// docプロジェクトでsbt-scalactiveをソースから直接使うため、compiler/sbt-pluginのソースをメタビルドに含める
Compile / unmanagedSourceDirectories ++= Seq(
  baseDirectory.value.getParentFile / "compiler" / "src" / "main" / "scala",
  baseDirectory.value.getParentFile / "sbt-plugin" / "src" / "main" / "scala"
)

// sbt-pluginの<style lang="scss">の変換用(build.sbtのsassEmbeddedVと同じ版)
libraryDependencies += "de.larsgrefer.sass" % "sass-embedded-host" % "4.2.0"

// coreのテスト(StRuntime)をjsdom上で実行するため
libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0"
