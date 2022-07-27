package io.github.cakelier

import java.io.File
import java.nio.file.Path

import scala.jdk.CollectionConverters._
import scala.util._

import com.typesafe.config.ConfigFactory
import sbt.Def.spaceDelimited
import sbt.Keys.{baseDirectory, streams}
import sbt._

import validation.Validation._
import Phases.connectToRemote

object RemoteDeployPlugin extends AutoPlugin {

  object autoImport {
    val remoteDeployConfFiles: SettingKey[Seq[String]] = settingKey[Seq[String]](
      "Deploy configuration file paths located in project root directory"
    )
    val remoteDeployConf: SettingKey[Seq[(String, Option[RemoteConfiguration])]] =
      settingKey[Seq[(String, Option[RemoteConfiguration])]](
        "Additional deploy configurations located in the sbt configuration file"
      )
    val remoteDeployArtifacts: TaskKey[Seq[(File, String)]] = taskKey[Seq[(File, String)]](
      "Artifacts that will be deployed to all remote locations"
    )
    val remoteDeployAfterHooks: SettingKey[Option[Remote => Unit]] =
      settingKey[Option[Remote => Unit]](
        "All hooks made of operations to be executed on the remote location after the deploy succeeded"
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
    remoteDeployAfterHooks := None,
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
        streams.value.log.debug(s"Deploy is being started for remote(s): ${configs.keys.mkString(", ")}.")
        args.foreach { n =>
          streams.value.log.debug(s"Deploying to remote named $n.")
          configs.get(n).flatten match {
            case Some(c) =>
              streams.value.log.debug(s"Configuration for remote $n found.")
              connectToRemote(c, artifacts, hooks, streams.value.log)
            case None =>
              streams.value.log.error(s"No configuration for remote $n found, skipping deployment.")
          }
        }
      }
      streams.value.log.debug("The operation has completed.")
    }
  )
}
