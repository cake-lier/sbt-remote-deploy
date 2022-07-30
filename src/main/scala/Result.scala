package io.github.cakelier

import scala.util.Try

import net.schmizz.sshj.connection.channel.direct.Session

/** The result of a command executed on a [[Remote]].
  *
  * This entity represents the result of the computation that is carried on by a command which execution is issued on a remote
  * location. Being this last one a command launched in a shell, its result is composed by an exit code and an error message, and
  * this last one is present only if the exit code means that the execution failed. Because the command is run remotely, its
  * execution can fail before it can return an exit code or an error message, due to a network partition. Instances of this trait
  * must be constructed through its companion object.
  */
trait Result extends {

  /** Returns the exit code which is part of the result, if the command execution did not fail before returning a result. */
  val exitCode: Option[Int]

  /** Returns the error message which is part of the result, if the command execution failed, but not before returning the error
    * message.
    */
  val errorMessage: Option[String]
}

/** Companion object to the [[Result]] trait, containing its factory method. */
private[cakelier] object Result {

  /* A case class implementation of the Result trait. */
  private final case class ResultImpl(exitCode: Option[Int], errorMessage: Option[String]) extends Result

  /** Creates a new instance of the [[Result]] trait, given the SSHJ command that has been launched and for which its result must
    * be returned.
    *
    * @param cmd
    *   the SSHJ command that has been launched and for which its result must be returned
    * @return
    *   the new [[Result]] trait instance
    */
  def apply(cmd: Session.Command): Result =
    ResultImpl(Try(cmd.getExitStatus.intValue()).toOption, Option(cmd.getExitErrorMessage))
}
