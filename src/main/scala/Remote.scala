package io.github.cakelier

import java.io.OutputStream

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import net.schmizz.sshj.connection.channel.direct.Session

/** A remote location, a server the user has access to through a SSH connection.
  *
  * This entity represents a remote location accessible through SSH, which allows to execute commands on it. The standard output
  * and the standard error of these commands can be either the same of the terminal in which the command was launched or other
  * ones, redirecting both standard error and output to [[java.io.OutputStream]]s defined by the user. Instances of this trait
  * must be constructed through its companion object.
  */
trait Remote {

  /** Runs the supplied command on this remote location and returns a [[scala.concurrent.Future]] containing the [[Result]] of the
    * execution.
    *
    * @param cmd
    *   the [[String]] representing the command to be executed
    * @return
    *   a [[scala.concurrent.Future]] containing the result of the command execution
    */
  def run(cmd: String): Future[Result]

  /** Runs the supplied command on this remote location and returns a [[scala.concurrent.Future]] containing the [[Result]] of the
    * execution, while also allowing user to redirect the standard output and the standard error to [[java.io.OutputStream]] of
    * their choice.
    *
    * @param cmd
    *   the [[String]] representing the command to be executed
    * @param stdout
    *   the [[java.io.OutputStream]] to which redirecting the standard output of the command
    * @param stderr
    *   the [[java.io.OutputStream]] to which redirecting the standard error of the command
    * @return
    *   a [[scala.concurrent.Future]] containing the result of the command execution
    */
  def runPipe(cmd: String)(stdout: OutputStream, stderr: OutputStream): Future[Result]
}

/** Companion object to the [[Remote]] trait, containing its factory method. */
private[cakelier] object Remote {

  /* An implementation of the Remote trait using the SSHJ library. */
  private class SSHRemote(session: Session) extends Remote {

    override def run(cmd: String): Future[Result] = runPipe(cmd)(Console.out, Console.err)

    @SuppressWarnings(Array("org.wartremover.warts.AnyVal"))
    override def runPipe(cmd: String)(stdout: OutputStream, stderr: OutputStream): Future[Result] = {
      val command = session.exec(cmd)
      Future
        .sequence(
          Seq(
            Future(command.getInputStream.transferTo(stdout)),
            Future(command.getErrorStream.transferTo(stderr)),
            Future(command.join())
          )
        )
        .map(_ => Result(command))
    }
  }

  /** Creates a new instance of the [[Remote]] trait, given the SSH session to be used for connecting to the remote itself.
    *
    * @param session
    *   the SSHJ session object to be used for connecting to the remote
    * @return
    *   the new [[Remote]] trait instance
    */
  def apply(session: Session): Remote = new SSHRemote(session)
}
