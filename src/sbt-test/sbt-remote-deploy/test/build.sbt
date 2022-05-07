import _root_.io.github.cakelier._
import com.github.dockerjava.core.{DefaultDockerClientConfig, DockerClientImpl}
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient

import java.time.Duration

ThisBuild / version := "1.0.0"

ThisBuild / scalaVersion := "2.13.8"

Global / onChangedBuildSource := ReloadOnSourceChanges

val initTask = taskKey[Unit]("Initialization task for plugin test")

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
      val imageId = docker.buildImageCmd(baseDirectory.value.asFile).start().awaitImageId
      val containerId = docker.createContainerCmd(imageId).withPortSpecs("22:2022").exec().getId
      docker.startContainerCmd(containerId).exec()
    },
    deployConfigurations := Seq(
      RemoteConfiguration(
        "test",
        RemoteLocation("ec2-34-234-69-43.compute-1.amazonaws.com", None, "ubuntu"),
        None,
        Some(Path("/home/matteo/Desktop/bigdata.pem").asPath)
      )
    ),
    deployArtifacts := Seq(
      (Compile / packageBin).value.getParentFile / (assembly / assemblyJarName).value -> "/home/ubuntu/main.jar"
    ),
    remoteDeployAfterHook := Seq(sshClient => {
      println("Testing")
      val res = for {
        result <- sshClient.exec("ls *.jar")
      } yield result.stdOutAsString()
      println(res)
      /*assert(result.isDefined)
      println("received: " + result.get)
      assert(result.get == "Hello world!")*/
    })
  )
