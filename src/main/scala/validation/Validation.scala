package io.github.cakelier
package validation

import validation.ValidationError._

import cats.data._
import cats.syntax.all._
import com.typesafe.config.Config
import sbt.util.Logger

import java.nio.file.Path
import scala.util.{Success, Try}

/** Collection of methods for validating a [[Config]] object as extracted from a configuration file.
  *
  * This object represents a module of functions to be used for validating a [[com.typesafe.config.Config]] object as parsed from
  * a configuration files. It is available one validator for each field, which are in turn be used by a method for validating the
  * whole configuration. Each validator is responsible only for the validation of the presence and the type of the field, the
  * actual validation of the content of the field is demanded to the [[RemoteConfiguration.Factory]].
  */
private[cakelier] object Validation {

  /* The module containing all the field validators. */
  private object FieldValidators {

    /* Validates the host field. */
    def validateHost(c: Config, name: String): ValidatedNel[ValidationError, String] =
      Try(c.getString("host"))
        .toValidated
        .leftMap(_ => MissingHostValue(name))
        .leftWiden[ValidationError]
        .toValidatedNel

    /* Validates the port field. */
    def validatePort(c: Config, name: String): ValidatedNel[ValidationError, Option[Int]] =
      (if (c.root().containsKey("port")) Try(Some(c.getInt("port"))) else Success(None))
        .toValidated
        .leftMap(_ => WrongPortFormat(name))
        .leftWiden[ValidationError]
        .toValidatedNel

    /* Validates the user field. */
    def validateUser(c: Config, name: String): ValidatedNel[ValidationError, String] =
      Try(c.getString("user"))
        .toValidated
        .leftMap(_ => MissingUserValue(name))
        .leftWiden[ValidationError]
        .toValidatedNel

    /* Validates the password field. */
    def validatePassword(c: Config, name: String): ValidatedNel[ValidationError, Option[String]] =
      (if (c.root().containsKey("password")) Try(Some(c.getString("password"))) else Success(None))
        .toValidated
        .leftMap(_ => WrongStringFieldFormat(name, "password"))
        .leftWiden[ValidationError]
        .toValidatedNel

    /* Validates the private key file field. */
    def validatePrivateKeyFile(c: Config, name: String): ValidatedNel[ValidationError, Option[Path]] =
      (if (c.root().containsKey("privateKeyFile")) Try(Some(c.getString("privateKeyFile"))) else Success(None))
        .toValidated
        .leftMap(_ => WrongPrivateKeyFileFormat(name))
        .leftWiden[ValidationError]
        .map(_.map(Path.of(_)))
        .toValidatedNel

    /* Validates the private key passphrase field. */
    def validatePrivateKeyPassphrase(c: Config, name: String): ValidatedNel[ValidationError, Option[String]] =
      (if (c.root().containsKey("privateKeyPassphrase")) Try(Some(c.getString("privateKeyPassphrase"))) else Success(None))
        .toValidated
        .leftMap(_ => WrongStringFieldFormat(name, "privateKeyPassphrase"))
        .leftWiden[ValidationError]
        .toValidatedNel
  }

  import validation.Validation.FieldValidators._

  /** Validates a [[Config]] instance containing a possible [[RemoteConfiguration]] combining all the fields validators defined
    * inside this object.
    *
    * @param configuration
    *   the [[Config]] instance to validate, if no exceptions were thrown while reading it, the corresponding exception otherwise
    * @param name
    *   the name of the configuration to validate
    * @param log
    *   the [[sbt.Logger]] to log the errors encountered during the validation
    * @return
    *   a [[scala.Option]] containing the [[RemoteConfiguration]] contained inside the given [[Config]] instance, if valid, a
    *   [[scala.None]] otherwise
    */
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
