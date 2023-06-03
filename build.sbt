import Dependencies._

ThisBuild / name := "sbt-remote-deploy"
ThisBuild / organization := "io.github.cake-lier"
ThisBuild / organizationName := "cake_lier"
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/cake-lier/sbt-remote-deploy"),
    "scm:git@github.com:cake-lier/sbt-remote-deploy.git"
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
ThisBuild / homepage := Some(
  url("https://github.com/cake-lier/sbt-remote-deploy")
)
ThisBuild / pomIncludeRepository := { _ => false }

ThisBuild / publishMavenStyle := true
ThisBuild / versionScheme := Some("semver-spec")
ThisBuild / crossPaths := false
ThisBuild / publishTo := sonatypePublishToBundle.value
ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / scalaVersion := "2.12.18"

ThisBuild / idePackagePrefix := Some("io.github.cakelier")

ThisBuild / scalafixDependencies ++= Seq(
  "com.github.liancheng" %% "organize-imports" % "0.6.0"
)

lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .enablePlugins(SiteScaladocPlugin)
  .settings(
    name := "sbt-remote-deploy",
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    version := "2.0.0",
    scriptedBufferLog := false,
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    libraryDependencies ++= Seq(
      typesafeConfig,
      cats,
      scalactic,
      sshj
    ),
    scalacOptions ++= Seq(
      "-Ywarn-unused",
      "-Ypartial-unification"
    ),
    wartremoverErrors ++= Warts.allBut(Wart.GlobalExecutionContext, Wart.ListUnapply),
    SiteScaladoc / siteSubdirName := "/"
  )
