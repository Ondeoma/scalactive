ThisBuild / description := "A frontend framework for Scala.js"
ThisBuild / organization := "io.github.ondeoma"
ThisBuild / version := "0.1.0"
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
lazy val catsV = "2.13.0"

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
    ),
    scalacOptions ++= Seq(
      "-Wconf:msg=Implicit parameters should be provided with a `using` clause:s"
    )
  )

lazy val root = (project in file("."))
  .aggregate(core)
  .settings(
    publish / skip := true
  )
