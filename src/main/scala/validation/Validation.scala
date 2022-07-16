package io.github.cakelier
package validation

import java.nio.file.Path

import scala.util.{Success, Try}

import cats.data._
import cats.syntax.all._
import com.typesafe.config.Config
import sbt.util.Logger

object Validation {

  def validateHost(c: Config, name: String): ValidatedNel[ValidationError, String] =
    Try(c.getString("host"))
      .toValidated
      .leftMap(_ => MissingHostValue(name))
      .leftWiden[ValidationError]
      .toValidatedNel

  def validatePort(c: Config, name: String): ValidatedNel[ValidationError, Option[Int]] =
    (if (c.root().containsKey("port")) Try(Some(c.getInt("port"))) else Success(None))
      .toValidated
      .leftMap(_ => WrongPortFormat(name))
      .leftWiden[ValidationError]
      .andThen[ValidationError, Option[Int]] {
        case Some(p) =>
          if (p > 0 && p < 65535) Some(p).valid[ValidationError] else WrongPortFormat(name).invalid[Option[Int]]
        case _ => None.valid[ValidationError]
      }
      .toValidatedNel

  def validateUser(c: Config, name: String): ValidatedNel[ValidationError, String] =
    Try(c.getString("user"))
      .toValidated
      .leftMap(_ => MissingUserValue(name))
      .leftWiden[ValidationError]
      .toValidatedNel

  def validatePassword(c: Config, name: String): ValidatedNel[ValidationError, Option[String]] =
    (if (c.root().containsKey("password")) Try(Some(c.getString("password"))) else Success(None))
      .toValidated
      .leftMap(_ => WrongStringOptionalFieldFormat(name, "password"))
      .leftWiden[ValidationError]
      .toValidatedNel

  def validatePrivateKeyFile(c: Config, name: String): ValidatedNel[ValidationError, Option[Path]] =
    (if (c.root().containsKey("privateKeyFile")) Try(Some(c.getString("privateKeyFile"))) else Success(None))
      .toValidated
      .leftMap(_ => WrongPrivateKeyFileFormat(name))
      .leftWiden[ValidationError]
      .map(_.map(Path.of(_)))
      .toValidatedNel

  def validatePrivateKeyPassphrase(c: Config, name: String): ValidatedNel[ValidationError, Option[String]] =
    (if (c.root().containsKey("privateKeyPassphrase")) Try(Some(c.getString("privateKeyPassphrase"))) else Success(None))
      .toValidated
      .leftMap(_ => WrongStringOptionalFieldFormat(name, "privateKeyPassphrase"))
      .leftWiden[ValidationError]
      .toValidatedNel

  @SuppressWarnings(Array("org.wartremover.warts.Any", "org.wartremover.warts.Nothing"))
  def validateConfiguration(configuration: Try[Config], name: String, log: Logger): Option[RemoteConfiguration] =
    configuration
      .toValidated
      .leftMap(_ => MissingRemoteKey(name))
      .leftWiden[ValidationError]
      .toValidatedNel
      .andThen((c: Config) =>
        validateHost(c, name)
          .product(validateUser(c, name))
          .andThen(t =>
            (
              validatePort(c, name),
              validatePassword(c, name),
              validatePrivateKeyFile(c, name),
              validatePrivateKeyPassphrase(c, name)
            )
              .mapN((p, w, f, pp) => (t._1, p, t._2, w, f, pp))
              .map(t =>
                RemoteConfiguration()
                  .host(t._1)
                  .port(t._2.getOrElse(22))
                  .user(t._3)
                  .password(t._4)
                  .privateKeyFile(t._5)
                  .privateKeyPassphrase(t._6)
                  .create
              )
          )
      )
      .toEither
      .fold(
        l => {
          l.map(_.message).toList.foreach(log.warn(_))
          None
        },
        identity
      )
}
