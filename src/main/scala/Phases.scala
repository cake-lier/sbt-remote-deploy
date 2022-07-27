package io.github.cakelier

import java.io.File

import scala.annotation.tailrec
import scala.util._

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import sbt.util.Logger

object Phases {

  @SuppressWarnings(Array("org.wartremover.warts.ToString"))
  def connectToRemote(
    configuration: RemoteConfiguration,
    artifacts: Seq[(File, String)],
    beforeHooks: Option[Remote => Unit],
    afterHooks: Option[Remote => Unit],
    log: Logger
  ): Unit = {
    val client = new SSHClient
    try {
      Try(client.loadKnownHosts()) match {
        case Failure(t) => log.debug(s"Could not load known hosts, operation failed with exception: $t")
        case Success(_) => log.debug("Known hosts successfully loaded")
      }
      client.addHostKeyVerifier(new PromiscuousVerifier())
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
        runHooks(client, beforeHooks)
        log.debug(s"Before-deployment hooks executed, copying artifacts.")
        copyArtifacts(client, artifacts, log) match {
          case Failure(_) =>
            log.error("The copy of the remaining files has been interrupted due to the exception thrown.")
          case Success(_) =>
            log.debug("All artifacts were copied correctly, executing after-deployment hooks.")
            runHooks(client, afterHooks)
        }
      } finally {
        client.disconnect()
      }
    } catch {
      case t: Throwable => log.error(s"The following exception happened which interrupted the execution: $t")
    }
  }

  def runHooks(client: SSHClient, hooks: Option[Remote => Unit]): Unit =
    hooks.foreach(f => {
      val session = client.startSession
      try {
        f(Remote(session))
      } finally {
        session.close()
      }
    })

  def copyArtifacts(client: SSHClient, artifacts: Seq[(File, String)], log: Logger): Try[Unit] = {
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
