import com.github.dockerjava.api.model.{ExposedPort, HostConfig, Ports}
import com.github.dockerjava.api.model.Ports.Binding
import com.github.dockerjava.core.{DefaultDockerClientConfig, DockerClientImpl}
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient

import java.nio.file.Files
import java.time.Duration

ThisBuild / version := "1.0.0"

ThisBuild / scalaVersion := "2.13.8"

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
          .connectionTimeout(Duration.ofSeconds(30))
          .responseTimeout(Duration.ofSeconds(45))
          .build()
      )
      println("Pulling image...")
      docker
        .pullImageCmd("matteocastellucci3/sbt-remote-deploy-password")
        .start()
        .awaitCompletion()
      println("Creating container...")
      val portBindings = new Ports()
      portBindings.bind(ExposedPort.tcp(22), Binding.bindPort(2023))
      docker
        .createContainerCmd("matteocastellucci3/sbt-remote-deploy-password")
        .withHostConfig(new HostConfig().withPortBindings(portBindings))
        .withName("sbt-remote-deploy-password")
        .exec()
      println("Starting container...")
      docker.startContainerCmd("sbt-remote-deploy-password").exec()
    },
    remoteDeployConf := Seq(
      remoteConfiguration(withName = "test") {
        has host "localhost"
        has port 2023
        has user "root"
        has password "example"
      }
    ),
    remoteDeployArtifacts := Seq(
      (Compile / packageBin).value.getParentFile / (assembly / assemblyJarName).value -> "/root/main.jar"
    ),
    remoteDeployAfterHooks := Seq(sshClient => {
      val output = for {
        result <- sshClient.exec("/root/.local/share/coursier/bin/scala main.jar")
      } yield result
      if (
        output.isFailure
        || output.get.exitCode.isEmpty
        || output.get.exitCode.get != 0
        || output.get.stdOutAsString() != "Hello world!\n"
      ) {
        println(s"""
        Command did not return expected result, it returned instead:
        Exit code: ${output.toOption.flatMap(_.exitCode)}
        Stdout: ${output.toOption.map(_.stdOutAsString())}
        Stderr: ${output.toOption.map(_.stdErrAsString())}
        """.trim)
      } else {
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
          .connectionTimeout(Duration.ofSeconds(30))
          .responseTimeout(Duration.ofSeconds(45))
          .build()
      )
      println("Stopping container...")
      docker.stopContainerCmd("sbt-remote-deploy-password").exec()
      println("Deleting container...")
      docker.removeContainerCmd("sbt-remote-deploy-password").exec()
      println("Deleting image...")
      docker.removeImageCmd("matteocastellucci3/sbt-remote-deploy-password").exec()
    }
  )
