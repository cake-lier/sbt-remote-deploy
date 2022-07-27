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
    afterHooks: Option[Remote => Unit],
    log: Logger
  ): Unit = {
    val client = new SSHClient
    try {
      client.loadKnownHosts()
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
        log.debug(s"Connection with remote ${configuration.host} established, copying artifacts.")
        copyArtifacts(client, artifacts, log) match {
          case Failure(_) =>
            log.error("The copy of the remaining files has been interrupted due to the exception thrown.")
          case Success(_) =>
            log.debug("All artifacts were copied correctly, executing after-deployment hooks.")
            val session = client.startSession
            afterHooks.foreach(_.apply(Remote(session)))
            session.close()
        }
      } finally {
        client.disconnect()
      }
    } catch {
      case t: Throwable => log.error(s"The following exception happened which interrupted the execution: $t")
    }
  }

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
