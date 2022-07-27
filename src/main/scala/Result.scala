package io.github.cakelier

import scala.util.Try

import net.schmizz.sshj.connection.channel.direct.Session

trait Result extends {

  val exitCode: Option[Int]

  val errorMessage: Option[String]
}

object Result {

  private final case class ResultImpl(exitCode: Option[Int], errorMessage: Option[String]) extends Result

  def apply(command: Session.Command): Result =
    ResultImpl(Try(command.getExitStatus.intValue()).toOption, Option(command.getExitErrorMessage))
}
