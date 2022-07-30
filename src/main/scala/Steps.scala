package io.github.cakelier

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import sbt.util.Logger

import java.io.File
import scala.annotation.tailrec
import scala.util._

/** Collection of methods for executing the steps composing this SBT plugin.
  *
  * This SBT plugin allows to connect to many different remote locations, or servers, and deploy one or more artifacts to them.
  * Before doing so, some functions can be invoked, as after the deployment. In these function the user can run commands on those
  * remote locations, for example for preparing the remote for the deployment or for cleaning up after. The connection happens
  * through a SSH connection, that needs to be established once for each remote.
  */
private[cakelier] object Steps {

  /** Establishes a connection with the remote location specified in the "configuration" parameter, then runs the commands on the
    * remote defined through the "beforeHook" parameter, deploys all artifacts defined through the "artifacts" parameter and then
    * runs the commands on the remote defined through the "afterHook" parameter.
    *
    * @param configuration
    *   the [[RemoteConfiguration]] of the remote location to contact
    * @param artifacts
    *   the pairs containing the local [[java.io.File]] to be copied and the path on the remote location to which copy the file
    * @param beforeHook
    *   the hook function containing the commands to be invoked on the remote location before the deployment of the artifacts
    * @param afterHook
    *   the hook function containing the commands to be invoked on the remote location before the deployment of the artifacts
    * @param log
    *   the [[sbt.Logger]] to be used for logging the state of the plugin
    */
  @SuppressWarnings(Array("org.wartremover.warts.ToString"))
  def connectToRemote(
    configuration: RemoteConfiguration,
    artifacts: Seq[(File, String)],
    beforeHook: Option[Remote => Unit],
    afterHook: Option[Remote => Unit],
    log: Logger
  ): Unit = {
    val client = new SSHClient
    try {
      if (configuration.verifyIdentity) {
        Try(client.loadKnownHosts()) match {
          case Failure(t) => log.debug(s"Could not load known hosts, operation failed with exception: $t")
          case Success(_) => log.debug("Known hosts successfully loaded")
        }
        configuration.fingerprint.foreach(client.addHostKeyVerifier)
      } else {
        client.addHostKeyVerifier(new PromiscuousVerifier())
      }
      client.connect(configuration.host, configuration.port)
      try {
        configuration
          .privateKeyFile
          .fold(
            client.authPassword(configuration.user, configuration.password.getOrElse(""))
          )(f =>
            configuration
              .privateKeyPassphrase
              .fold(
                client.authPublickey(configuration.user, f.toString)
              )(p => client.authPublickey(configuration.user, client.loadKeys(f.toString, p)))
          )
        log.debug(s"Connection with remote ${configuration.host} established, executing before-deployment hooks.")
        runHook(client, beforeHook)
        log.debug("Before-deployment hooks executed, copying artifacts.")
        copyArtifacts(client, artifacts, log) match {
          case Failure(_) =>
            log.error("The copy of the remaining files has been interrupted due to the exception thrown.")
          case Success(_) =>
            log.debug("All artifacts were copied correctly, executing after-deployment hooks.")
            runHook(client, afterHook)
        }
      } finally {
        client.disconnect()
      }
    } catch {
      case t: Throwable => log.error(s"The following exception happened which interrupted the execution: $t")
    }
  }

  /* Launches a hook containing commands to execute on the remote location. */
  private def runHook(client: SSHClient, hook: Option[Remote => Unit]): Unit =
    hook.foreach(f => {
      val session = client.startSession
      try {
        f(Remote(session))
      } finally {
        session.close()
      }
    })

  /* Copies the artifacts to the remote location. */
  private def copyArtifacts(client: SSHClient, artifacts: Seq[(File, String)], log: Logger): Try[Unit] = {
    @tailrec
    def copyArtifact(remaining: Seq[(File, String)]): Try[Unit] = remaining match {
      case (localFile, remotePath) :: t =>
        val localPath = localFile.getPath
        log.debug(s"Copying artifact from local path $localPath to remote path $remotePath.")
        Try(client.newSCPFileTransfer.upload(localPath, remotePath)) match {
          case Failure(t) =>
            log.error(s"Artifact at local path $localPath copy failed with exception: $t")
            Failure[Unit](t)
          case Success(_) =>
            log.debug(s"Artifact at path $localPath correctly copied, continuing...")
            copyArtifact(t)
        }
      case _ => Success(())
    }
    copyArtifact(artifacts)
  }
}
