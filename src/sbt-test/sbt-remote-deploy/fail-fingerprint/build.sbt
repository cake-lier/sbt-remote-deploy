import com.github.dockerjava.api.model.{ExposedPort, HostConfig, Ports}
import com.github.dockerjava.api.model.Ports.Binding
import com.github.dockerjava.core.{DefaultDockerClientConfig, DockerClientImpl}
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient

import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.time.{Duration => JDuration}
import scala.concurrent.duration.Duration
import scala.concurrent.Await

ThisBuild / version := "1.0.0"

ThisBuild / scalaVersion := "2.13.16"

Global / onChangedBuildSource := ReloadOnSourceChanges

val initTask = taskKey[Unit]("Initialization task for plugin test")
val teardownTask = taskKey[Unit]("Teardown task for plugin test")

lazy val root = (project in file("."))
  .enablePlugins(RemoteDeployPlugin)
  .settings(
    name := "test",
    idePackagePrefix := Some("io.github.cakelier"),
    assembly / mainClass := Some("io.github.cakelier.Test"),
    assembly / assemblyJarName := "main.jar",
    initTask := {
      val config = DefaultDockerClientConfig.createDefaultConfigBuilder.build
      val docker = DockerClientImpl.getInstance(
        config,
        new ApacheDockerHttpClient.Builder()
          .dockerHost(config.getDockerHost)
          .sslConfig(config.getSSLConfig)
          .maxConnections(100)
          .connectionTimeout(JDuration.ofSeconds(30))
          .responseTimeout(JDuration.ofSeconds(45))
          .build()
      )
      println("Pulling image...")
      docker
        .pullImageCmd("matteocastellucci3/sbt-remote-deploy-fail-fingerprint")
        .start()
        .awaitCompletion()
      println("Creating container...")
      val portBindings = new Ports()
      portBindings.bind(ExposedPort.tcp(22), Binding.bindPort(2026))
      docker
        .createContainerCmd("matteocastellucci3/sbt-remote-deploy-fail-fingerprint")
        .withHostConfig(new HostConfig().withPortBindings(portBindings))
        .withName("sbt-remote-deploy-fail-fingerprint")
        .exec()
      println("Starting container...")
      docker.startContainerCmd("sbt-remote-deploy-fail-fingerprint").exec()
    },
    remoteDeployConf := Seq(
      remoteConfiguration(withName = "test") {
        has host "localhost"
        has port 2026
        has user "root"
        has password "example"
        has verifyIdentity true
      }
    ),
    remoteDeployArtifacts := Seq(
      (Compile / packageBin).value.getParentFile / (assembly / assemblyJarName).value -> "/root/main.jar"
    ),
    remoteDeployAfterHook := Some(remote => {
      val stdout = new ByteArrayOutputStream()
      val stderr = new ByteArrayOutputStream()
      val result =
        Await.result(remote.runPipe("/root/.local/share/coursier/bin/scala main.jar")(stdout, stderr), Duration.Inf)
      if (
        result.exitCode.isDefined
        && result.exitCode.get == 0
        && stdout.toString == "Hello world!\n"
      ) {
        Files.createFile(baseDirectory.value.toPath.resolve("SUCCESS"))
      }
    }),
    teardownTask := {
      val config = DefaultDockerClientConfig.createDefaultConfigBuilder.build
      val docker = DockerClientImpl.getInstance(
        config,
        new ApacheDockerHttpClient.Builder()
          .dockerHost(config.getDockerHost)
          .sslConfig(config.getSSLConfig)
          .maxConnections(100)
          .connectionTimeout(JDuration.ofSeconds(30))
          .responseTimeout(JDuration.ofSeconds(45))
          .build()
      )
      println("Stopping container...")
      docker.stopContainerCmd("sbt-remote-deploy-fail-fingerprint").exec()
      println("Deleting container...")
      docker.removeContainerCmd("sbt-remote-deploy-fail-fingerprint").exec()
      println("Deleting image...")
      docker.removeImageCmd("matteocastellucci3/sbt-remote-deploy-fail-fingerprint").exec()
    }
  )
