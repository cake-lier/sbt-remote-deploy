import Dependencies._

ThisBuild / version := "1.0.0"
ThisBuild / organization := "io.github.cake-lier"
ThisBuild / organizationName := "cake_lier"
ThisBuild / homepage := Some(
  url("https://github.com/cake-lier/sbt-remote-deploy")
)
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/cake-lier/sbt-remote-deploy"),
    "git@github.com:cake-lier/sbt-remote-deploy.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id = "cake_lier",
    name = "Matteo Castellucci",
    email = "matteo.castellucci@outlook.com",
    url = url("https://github.com/cake-lier")
  )
)
ThisBuild / description := "A sbt plugin for deploying a scala artifact remotely."
ThisBuild / licenses := List(
  "MIT" -> new URL("https://opensource.org/licenses/MIT")
)
ThisBuild / publishMavenStyle := true
ThisBuild / crossPaths := false
ThisBuild / publishTo := Some(if (isSnapshot.value) Opts.resolver.sonatypeSnapshots else Opts.resolver.sonatypeStaging)

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / scalaVersion := "2.12.14"

ThisBuild / idePackagePrefix := Some("io.github.cakelier")

lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-remote-deploy",
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false,
    libraryDependencies ++= Seq(
      typesafeConfig,
      ssh,
      cats
    )
  )
