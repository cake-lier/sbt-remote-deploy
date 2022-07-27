package io.github.cakelier

import scala.util.Try

import net.schmizz.sshj.connection.channel.direct.Session

trait Result extends {

  val exitCode: Option[Int]

  val errorMessage: Option[String]
}

object Result {

  class ResultImpl(command: Session.Command) extends Result {

    override val exitCode: Option[Int] = Try(command.getExitStatus.intValue()).toOption

    override val errorMessage: Option[String] = Option(command.getExitErrorMessage)
  }

  def apply(command: Session.Command): Result = new ResultImpl(command)
}
