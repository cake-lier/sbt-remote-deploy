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
import Steps.connectToRemote
import validation.ValidationError

/** The SBT plugin, the main entrypoint to this project.
  *
  * This object represents the SBT plugin, containing its keys to be used in a build file, the DSL to specify a configuration
  * through code, and the steps taken by the plugin before executing its proper behavior, like parsing and validating the
  * configurations.
  */
object RemoteDeployPlugin extends AutoPlugin {

  /** The "autoImport" object, containing all things that must be auto-imported by the plugin when activated. */
  object autoImport {

    val remoteDeployConfFiles: SettingKey[Seq[String]] = settingKey[Seq[String]](
      "Deploy configuration file paths located in project root directory"
    )

    val remoteDeployConf: SettingKey[Seq[(String, Either[Seq[ValidationError], RemoteConfiguration])]] =
      settingKey[Seq[(String, Either[Seq[ValidationError], RemoteConfiguration])]](
        "Additional deploy configurations located in the sbt configuration file"
      )

    val remoteDeployArtifacts: TaskKey[Seq[(File, String)]] = taskKey[Seq[(File, String)]](
      "Artifacts that will be deployed to all remote locations"
    )

    val remoteDeployBeforeHook: SettingKey[Option[Remote => Unit]] =
      settingKey[Option[Remote => Unit]](
        "A hook consisting of commands to be executed on the remote locations before the deploy"
      )

    val remoteDeployAfterHook: SettingKey[Option[Remote => Unit]] =
      settingKey[Option[Remote => Unit]](
        "A hook consisting of commands to be executed on the remote locations after the deploy"
      )

    val remoteDeploy: InputKey[Unit] = inputKey[Unit](
      "Deploy to the specified remote location. Usage: `remoteDeploy remoteName1 remoteName2`"
    )

    @SuppressWarnings(Array("org.wartremover.warts.Var"))
    private var configuration: RemoteConfiguration.Factory = RemoteConfiguration()

    /** Contains all methods which are part of the DSL used for specifying a [[RemoteConfiguration]] via code.
      *
      * This object contains all methods which are part of a DSL which has been introduced in this plugin for defining a
      * [[RemoteConfiguration]] via code. This means that the DSL must allow to specify the hostname and the username of the
      * remote location to which connect, the port on which the remote location listens for SSH connections, the password or the
      * private key file and its possible passphrase to be used in the authentication phase.
      */
    object has {

      /** Allows to specify the hostname of the remote location which the [[RemoteConfiguration]] to create refers to.
        *
        * @param host
        *   the hostname of the remote location
        */
      def host(host: String): Unit = configuration = configuration.host(host)

      /** Allows to specify the port of the remote location which the [[RemoteConfiguration]] to create refers to.
        *
        * @param port
        *   the port behind which the remote location listens for SSH connections
        */
      def port(port: Int): Unit = configuration = configuration.port(port)

      /** Allows to specify the username used in the remote location which the [[RemoteConfiguration]] to create refers to.
        *
        * @param user
        *   the username used in the remote location
        */
      def user(user: String): Unit = configuration = configuration.user(user)

      /** The password used in the authentication phase of the SSH connection to the remote location which the
        * [[RemoteConfiguration]] to create refers to.
        *
        * @param password
        *   the password to be used in the authentication phase of the SSH connection
        */
      def password(password: String): Unit = configuration = configuration.password(Some(password))

      /** The path to the private key file used in the authentication phase of the SSH connection to the remote location which the
        * [[RemoteConfiguration]] to create refers to.
        *
        * @param privateKeyFile
        *   the path to the private key to be used in the authentication phase of the SSH connection
        */
      def privateKeyFile(privateKeyFile: String): Unit = configuration =
        configuration.privateKeyFile(Some(Path.of(privateKeyFile)))

      /** The passphrase used to encrypt the private key file used in the authentication phase of the SSH connection to the remote
        * location which the [[RemoteConfiguration]] to create refers to.
        *
        * @param privateKeyPassphrase
        *   the passphrase used to encrypt the private key to be used in the authentication phase of the SSH connection
        */
      def privateKeyPassphrase(privateKeyPassphrase: String): Unit =
        configuration = configuration.privateKeyPassphrase(Some(privateKeyPassphrase))

      /** The fingerprint to be used for identifying the remote location which the [[RemoteConfiguration]] to create refers to.
        *
        * @param fingerprint
        *   the fingerprint to be used for identifying the remote location
        */
      def fingerprint(fingerprint: String): Unit =
        configuration = configuration.fingerprint(Some(fingerprint))

      /** The parameter "verify identity" used for connecting to the remote location only if the identity can be verified, remote
        * which the [[RemoteConfiguration]] to create refers to.
        *
        * @param verifyIdentity
        *   the parameter to be used for specifying to connect to the remote location only if the identity can be verified
        */
      def verifyIdentity(verifyIdentity: Boolean): Unit =
        configuration = configuration.verifyIdentity(verifyIdentity)
    }

    /** Allows to use the DSL for specifying a [[RemoteConfiguration]]. With its first parameter it allows to specify the name of
      * the [[RemoteConfiguration]] and with the second provides a place to use the DSL methods to specify all the parameters
      * which compose the [[RemoteConfiguration]].
      *
      * @param withName
      *   the name of the [[RemoteConfiguration]] to create
      * @param body
      *   the parameter to be used as a place to call the DSL methods
      * @return
      *   a new pair made of the name of a [[RemoteConfiguration]] and a [[scala.Either]] containing the [[RemoteConfiguration]]
      *   itself, if all the parameters were valid, a [[scala.Seq]] with all errors encountered while validating the possible
      *   [[RemoteConfiguration]] otherwise
      */
    def remoteConfiguration(withName: String)(body: => Unit): (String, Either[Seq[ValidationError], RemoteConfiguration]) = {
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
    remoteDeployConf := Seq.empty[(String, Either[Seq[ValidationError], RemoteConfiguration])],
    remoteDeployArtifacts := Seq.empty[(File, String)],
    remoteDeployBeforeHook := None,
    remoteDeployAfterHook := None,
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
          .foldLeft(Map.empty[String, Either[Seq[ValidationError], RemoteConfiguration]])((m, c) =>
            m ++
              Try(c._1.getConfig("remotes"))
                .map(r =>
                  r.root
                    .keySet
                    .asScala
                    .toSeq
                    .map(n => (n, validateConfiguration(Try(r.getConfig(n)), n)))
                )
                .toOption
                .getOrElse {
                  streams.value.log.warn(s"Unable to parse configuration file #${c._2}, check if its format is correct.")
                  Seq.empty[(String, Either[Seq[ValidationError], RemoteConfiguration])]
                }
          ) ++ remoteDeployConf.value
      streams.value.log.debug(s"${configs.size} configuration(s) loaded.")
      val artifacts = remoteDeployArtifacts.value
      val beforeHooks = remoteDeployBeforeHook.value
      val afterHooks = remoteDeployAfterHook.value
      if (configs.nonEmpty) {
        streams.value.log.debug(s"Deploy is being started for remote(s): ${configs.keys.mkString(", ")}.")
        args.foreach { n =>
          streams.value.log.debug(s"Deploying to remote named $n.")
          configs.get(n) match {
            case Some(c) =>
              streams.value.log.debug(s"Configuration for remote $n found.")
              c match {
                case Left(e) =>
                  streams.value.log.error(s"Configuration for remote $n was parsed with errors, the errors will be shown below:")
                  e.foreach(v => streams.value.log.error("\t" + v.message))
                case Right(r) => connectToRemote(r, artifacts, beforeHooks, afterHooks, streams.value.log)
              }
            case None =>
              streams.value.log.error(s"No configuration for remote $n found, skipping deployment.")
          }
        }
      }
      streams.value.log.debug("The operation has completed.")
    }
  )
}
