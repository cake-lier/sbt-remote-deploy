package io.github.cakelier

import java.nio.file.Path

import scala.jdk.CollectionConverters._
import scala.util._

import cats.syntax.all._
import com.decodified.scalassh.HostKeyVerifiers.DontVerify
import com.decodified.scalassh.PasswordProducer.fromString
import com.decodified.scalassh._
import com.typesafe.config.ConfigFactory
import sbt.Def.spaceDelimited
import sbt.Keys.{baseDirectory, streams}
import sbt._
import sbt.util.Logger

import validation.Validation._

object RemoteDeployPlugin extends AutoPlugin {

  object autoImport {
    val remoteDeployConfFiles: SettingKey[Seq[String]] = settingKey[Seq[String]](
      "Deploy configuration file paths located in project root directory"
    )
    val remoteDeployConf: SettingKey[Seq[(String, Option[RemoteConfiguration])]] =
      settingKey[Seq[(String, Option[RemoteConfiguration])]](
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

    @SuppressWarnings(Array("org.wartremover.warts.Var"))
    private var configuration: RemoteConfiguration.Factory = RemoteConfiguration()

    object has {
      def host(host: String): Unit = configuration = configuration.host(host)

      def port(port: Int): Unit = configuration = configuration.port(port)

      def user(user: String): Unit = configuration = configuration.user(user)

      def password(password: String): Unit = configuration = configuration.password(Some(password))

      def privateKeyFile(privateKeyFile: String): Unit = configuration =
        configuration.privateKeyFile(Some(Path.of(privateKeyFile)))

      def privateKeyPassphrase(privateKeyPassphrase: String): Unit =
        configuration = configuration.privateKeyPassphrase(Some(privateKeyPassphrase))
    }

    def remoteConfiguration(withName: String)(body: => Unit): (String, Option[RemoteConfiguration]) = {
      body
      val tuple = withName -> configuration.create
      configuration = RemoteConfiguration()
      tuple
    }
  }

  import autoImport._

  @SuppressWarnings(Array("org.wartremover.warts.Any", "org.wartremover.warts.Nothing"))
  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    remoteDeployConfFiles := Seq.empty[String],
    remoteDeployConf := Seq.empty[(String, Option[RemoteConfiguration])],
    remoteDeployArtifacts := Seq.empty[(File, String)],
    remoteDeployAfterHooks := Seq.empty[SshClient => Unit],
    remoteDeploy := {
      val args = spaceDelimited("<first configuration name> <second configuration name> ...").parsed
      val configs =
        remoteDeployConfFiles
          .value
          .flatMap(r => {
            val path = ((ThisBuild / baseDirectory).value / r).get().headOption
            if (path.isEmpty) {
              streams.value.log.warn(s"The configuration at $r was not found, check if the given path is correct.")
            }
            path.toList
          })
          .map(p => {
            val configurationFile = ConfigFactory.parseFile(p).resolve()
            if (configurationFile.isEmpty) {
              streams.value.log.warn(s"The configuration at $p was found empty, check if its format is correct.")
            }
            configurationFile
          })
          .zipWithIndex
          .foldLeft(Map.empty[String, Option[RemoteConfiguration]])((m, c) =>
            m ++
              Try(c._1.getConfig("remotes"))
                .map(r =>
                  r.root
                    .keySet
                    .asScala
                    .toSeq
                    .map(n => (n, validateConfiguration(Try(r.getConfig(n)), n, streams.value.log)))
                )
                .toOption
                .getOrElse {
                  streams.value.log.warn(s"Unable to parse configuration file #${c._2}, check if its format is correct.")
                  Seq.empty[(String, Option[RemoteConfiguration])]
                }
          ) ++ remoteDeployConf.value
      streams.value.log.debug(s"${configs.size} configuration(s) loaded.")
      val artifacts = remoteDeployArtifacts.value
      val hooks = remoteDeployAfterHooks.value
      if (configs.nonEmpty) {
        streams.value.log.debug(s"Deploy is being started for server(s): ${configs.keys.mkString(", ")}.")
        args.foreach { n =>
          streams.value.log.debug(s"Deploying to remote named $n.")
          configs.get(n).flatten match {
            case Some(c) =>
              streams.value.log.debug(s"Configuration for remote $n found.")
              deploy(c, artifacts, hooks, streams.value.log)
            case None =>
              streams.value.log.error(s"No configuration for remote $n found, skipping deployment.")
          }
        }
      }
      streams.value.log.debug("The operation has completed.")
    }
  )

  @SuppressWarnings(Array("org.wartremover.warts.ToString"))
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
          .map[SshLogin](k =>
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
                Failure[Unit](t)
              }
            )
        }
        .transform(
          _ => {
            log.debug("All artifacts were copied correctly, executing after-deployment hooks.")
            afterHooks.map(f => Try(f(client))).toList.sequence.map(_ => ())
          },
          t => Try(log.error(s"While completing the deployment operation, the following exception happened: ${t.toString}"))
        )
    } match {
      case Failure(t) =>
        log.error(s"While executing the after-deployment hooks, the following exception happened: ${t.toString}")
      case Success(_) =>
        log.debug("All after-deployment hooks completed correctly.")
    }
  }
}
