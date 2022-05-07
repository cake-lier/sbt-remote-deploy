package io.github.cakelier

import com.decodified.scalassh.*
import com.decodified.scalassh.HostKeyVerifiers.DontVerify
import com.decodified.scalassh.PasswordProducer.fromString
import com.decodified.scalassh.PublicKeyLogin.DefaultKeyLocations
import com.typesafe.config.{ConfigException, ConfigFactory, ConfigParseOptions}
import sbt.{file, singleFileFinder, AutoPlugin, InputKey, SettingKey, TaskKey, ThisBuild}
import sbt.Def.*
import sbt.Keys.*
import sbt.util.Logger

import java.io.File
import java.nio.file.Paths
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

object RemoteDeployPlugin extends AutoPlugin {

  object autoImport {
    val deployConfigurationsFiles: SettingKey[Seq[String]] = settingKey[Seq[String]](
      "Deploy configuration file paths located in project root directory"
    )
    val deployConfigurations: SettingKey[Seq[RemoteConfiguration]] = settingKey[Seq[RemoteConfiguration]](
      "Additional deploy configurations coming from the sbt configuration file"
    )
    val deployArtifacts: TaskKey[Seq[(File, String)]] = taskKey[Seq[(File, String)]](
      "Artifacts that will be deployed to the remote locations"
    )
    val remoteDeployAfterHook: SettingKey[Seq[SshClient => Unit]] =
      settingKey[Seq[SshClient => Unit]](
        "Hook for executing commands on remote locations after deploy"
      )
    val remoteDeploy: InputKey[Unit] = inputKey[Unit](
      "Deploy to the specified remote location. Usage: `remoteDeploy remoteName1 remoteName2`"
    )
  }

  import autoImport.*

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    deployConfigurationsFiles := Seq.empty,
    deployConfigurations := Seq.empty,
    deployArtifacts := Seq.empty,
    remoteDeployAfterHook := Seq.empty,
    remoteDeploy := {
      val log = streams.value.log
      val args = spaceDelimited("<first configuration name> <second configuration name> ...").parsed
      val configs = deployConfigurationsFiles
        .value
        .map(p => {
          val path = file(((ThisBuild / baseDirectory).value / p).getPaths().head)
          try {
            ConfigFactory.parseFile(path, ConfigParseOptions.defaults.setAllowMissing(false)).resolve()
          } catch {
            case e: ConfigException =>
              log.error(s"Failed to load the configuration at $path: the exception was '$e''")
              ConfigFactory.empty()
          }
        })
        .foldLeft(List.empty[RemoteConfiguration])((l, c) =>
          l ++ (
            for {
              servers <- Try(c.getObjectList("servers").asScala).getOrElse(Seq.empty)
              serverConfigurations = servers.toConfig
              configurationName = serverConfigurations.getString("configurationName")
              remoteHost = serverConfigurations.getString("remoteHost")
              remotePort = Try(serverConfigurations.getInt("remotePort")).toOption
              remoteUser = serverConfigurations.getString("remoteUser")
              remotePassword = Try(serverConfigurations.getString("remotePassword")).toOption
              privateKeyFile = Try(serverConfigurations.getString("privateKeyFile")).toOption.map(Paths.get(_))
              privateKeyPassphrase = Try(serverConfigurations.getString("privateKeyPassphrase")).toOption
            } yield RemoteConfiguration(
              configurationName,
              RemoteLocation(remoteHost, remotePort, remoteUser, remotePassword),
              privateKeyPassphrase,
              privateKeyFile
            )
          )
        ) ++ deployConfigurations.value
      log.debug(s"${configs.size} have been loaded.")
      val artifacts = deployArtifacts.value
      val hooks = remoteDeployAfterHook.value
      if (configs.nonEmpty) {
        log.debug(configs.mkString(", \n"))
        log.debug(s"Deploy is being started for server(s): ${args.mkString(", ")}.")
        args.foreach { n =>
          log.debug(s"Deploying to remote named $n.")
          configs.map(c => c.configurationName -> c).toMap.get(n) match {
            case Some(c) =>
              log.debug(s"Configuration for remote $n found.")
              deploy(c, artifacts, hooks, log)
            case None =>
              log.error(s"No configuration for remote $n found, skipping deployment.")
          }
        }
      }
      log.debug("The deployment has completed.")
    }
  )

  private def deploy(
    configuration: RemoteConfiguration,
    artifacts: Seq[(File, String)],
    afterHooks: Seq[SshClient => Unit],
    log: Logger
  ): Unit = {
    SSH(
      configuration.remoteLocation.host,
      HostConfig(
        login = configuration
          .privateKeyFile
          .map(_.toString)
          .map(k =>
            configuration
              .remoteLocation
              .password
              .map(w => PublicKeyLogin(configuration.remoteLocation.user, w, k +: DefaultKeyLocations))
              .getOrElse(PublicKeyLogin(configuration.remoteLocation.user, k +: DefaultKeyLocations: _*))
          )
          .getOrElse(
            PasswordLogin(configuration.remoteLocation.user, configuration.remoteLocation.password.getOrElse[String](""))
          ),
        port = configuration.remoteLocation.port.getOrElse(22),
        hostKeyVerifier = DontVerify
      )
    ) { client =>
      log.debug(s"Connection with remote ${configuration.configurationName} established, copying artifacts.")
      val transfers = artifacts.map { case (localFile, remotePath) =>
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
      if (transfers.forall(_.isSuccess)) {
        log.debug("All artifacts were copied correctly, executing after-deployment hooks.")
        afterHooks.foreach(_.apply(client))
      }
    }
  }
}
