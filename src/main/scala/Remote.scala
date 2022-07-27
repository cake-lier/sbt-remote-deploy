package io.github.cakelier

import java.io.OutputStream

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import net.schmizz.sshj.connection.channel.direct.Session

trait Remote {

  def run(cmd: String): Future[Result]

  def runPipe(cmd: String)(stdout: OutputStream, stderr: OutputStream): Future[Result]
}

object Remote {

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

  def apply(session: Session): Remote = new SSHRemote(session)
}
