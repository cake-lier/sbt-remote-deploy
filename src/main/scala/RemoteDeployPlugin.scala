package io.github.cakelier

import cats.implicits._
import com.decodified.scalassh._
import com.decodified.scalassh.HostKeyVerifiers.DontVerify
import com.decodified.scalassh.PasswordProducer.fromString
import com.typesafe.config.{ConfigFactory, ConfigParseOptions}
import sbt.{file, singleFileFinder, AutoPlugin, File, InputKey, SettingKey, TaskKey, ThisBuild}
import sbt.Def._
import sbt.Keys._
import sbt.util.Logger

import java.nio.file.Paths
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

object RemoteDeployPlugin extends AutoPlugin {

  object autoImport {
    val remoteDeployConfFiles: SettingKey[Seq[String]] = settingKey[Seq[String]](
      "Deploy configuration file paths located in project root directory"
    )
    val remoteDeployConf: SettingKey[Seq[(String, RemoteConfiguration)]] = settingKey[Seq[(String, RemoteConfiguration)]](
      "Additional deploy configurations coming from the sbt configuration file"
    )
    val remoteDeployArtifacts: TaskKey[Seq[(File, String)]] = taskKey[Seq[(File, String)]](
      "Artifacts that will be deployed to the remote locations"
    )
    val remoteDeployAfterHooks: SettingKey[Seq[SshClient => Unit]] =
      settingKey[Seq[SshClient => Unit]](
        "Hook for executing commands on remote locations after deploy"
      )
    val remoteDeploy: InputKey[Unit] = inputKey[Unit](
      "Deploy to the specified remote location. Usage: `remoteDeploy remoteName1 remoteName2`"
    )

    val has: ConfigurationFactory = ConfigurationFactory()

    def remoteConfiguration(withName: String)(body: Unit): (String, RemoteConfiguration) = {
      val configuration = withName -> has.build
      has.reset()
      configuration
    }
  }

  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    remoteDeployConfFiles := Seq.empty,
    remoteDeployConf := Seq.empty,
    remoteDeployArtifacts := Seq.empty,
    remoteDeployAfterHooks := Seq.empty,
    remoteDeploy := {
      val args = spaceDelimited("<first configuration name> <second configuration name> ...").parsed
      val configs = remoteDeployConfFiles
        .value
        .map(p => {
          val path = file(((ThisBuild / baseDirectory).value / p).getPaths().head)
          val configurationFile = ConfigFactory.parseFile(path, ConfigParseOptions.defaults).resolve()
          if (configurationFile.isEmpty) {
            streams.value.log.warn(s"The configuration at $path was found empty, check if the given path is correct.")
          }
          configurationFile
        })
        .foldLeft(Map.empty[String, RemoteConfiguration])((m, c) =>
          m ++ (
            for {
              servers <- Set(c.getConfig("remotes"))
              configurationName <- servers.root.keySet.asScala
              remoteHost = servers.atKey(configurationName).getString("host")
              remotePort = Try(servers.atKey(configurationName).getInt("port")).toOption
              remoteUser = servers.atKey(configurationName).getString("user")
              remotePassword = Try(servers.atKey(configurationName).getString("password")).toOption
              privateKeyFile = Try(servers.atKey(configurationName).getString("privateKeyFile")).toOption.map(Paths.get(_))
              privateKeyPassphrase = Try(servers.atKey(configurationName).getString("privateKeyPassphrase")).toOption
            } yield configurationName -> RemoteConfiguration(
              remoteHost,
              remotePort.getOrElse(22),
              remoteUser,
              remotePassword,
              privateKeyFile,
              privateKeyPassphrase
            )
          )
        ) ++ remoteDeployConf.value
      streams.value.log.debug(s"${configs.size} configuration(s) loaded.")
      val artifacts = remoteDeployArtifacts.value
      val hooks = remoteDeployAfterHooks.value
      if (configs.nonEmpty) {
        streams.value.log.debug(configs.mkString(", \n"))
        streams.value.log.debug(s"Deploy is being started for server(s): ${args.mkString(", ")}.")
        args.foreach { n =>
          streams.value.log.debug(s"Deploying to remote named $n.")
          configs.get(n) match {
            case Some(c) =>
              streams.value.log.debug(s"Configuration for remote $n found.")
              deploy(c, artifacts, hooks, streams.value.log)
            case None =>
              streams.value.log.error(s"No configuration for remote $n found, skipping deployment.")
          }
        }
      }
      streams.value.log.debug("The deployment has completed.")
    }
  )

  private def deploy(
    configuration: RemoteConfiguration,
    artifacts: Seq[(File, String)],
    afterHooks: Seq[SshClient => Unit],
    log: Logger
  ): Unit = {
    SSH(
      configuration.host,
      HostConfig(
        login = configuration
          .privateKeyFile
          .map(_.toString)
          .map(k =>
            configuration
              .privateKeyPassphrase
              .map(p => PublicKeyLogin(configuration.user, p, List(k)))
              .getOrElse(PublicKeyLogin(configuration.user, k))
          )
          .getOrElse(
            PasswordLogin(configuration.user, configuration.password.getOrElse[String](""))
          ),
        port = configuration.port,
        hostKeyVerifier = DontVerify
      )
    ) { client =>
      log.debug(s"Connection with remote ${configuration.host} established, copying artifacts.")
      artifacts
        .toList
        .traverse { case (localFile, remotePath) =>
          val localPath = localFile.getPath
          log.debug(s"Copying artifact from local path $localPath to remote path $remotePath.")
          client
            .upload(localPath, remotePath)
            .transform(
              _ => {
                log.debug(s"Artifact at path $localPath correctly copied")
                Success(())
              },
              t => {
                log.error(s"Artifact at local path $localPath copy failed with exception: $t")
                Failure(t)
              }
            )
        }
        .foreach { _ =>
          log.debug("All artifacts were copied correctly, executing after-deployment hooks.")
          afterHooks.foreach(_.apply(client))
        }
    }
  }
}
