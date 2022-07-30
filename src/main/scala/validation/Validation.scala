package io.github.cakelier
package validation

import java.nio.file.Path

import scala.util.{Success, Try}

import cats.data._
import cats.syntax.all._
import com.typesafe.config.Config

import validation.ValidationError._

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

    private type Validation[A] = ValidatedNel[ValidationError, A]

    private def validateRequiredStringField(config: Config, name: String, error: ValidationError): Validation[String] =
      Try(config.getString(name))
        .toValidated
        .leftMap(_ => error)
        .leftWiden[ValidationError]
        .toValidatedNel

    private def validateOptionalStringField(config: Config, name: String, error: ValidationError): Validation[Option[String]] =
      (if (config.root().containsKey(name)) Try(Some(config.getString(name))) else Success(None))
        .toValidated
        .leftMap(_ => error)
        .leftWiden[ValidationError]
        .toValidatedNel

    /* Validates the host field. */
    def validateHost(config: Config): Validation[String] =
      validateRequiredStringField(config, "host", MissingOrInvalidHostValue)

    /* Validates the port field. */
    def validatePort(config: Config): Validation[Option[Int]] =
      (if (config.root().containsKey("port")) Try(Some(config.getInt("port"))) else Success(None))
        .toValidated
        .leftMap(_ => InvalidPortValue)
        .leftWiden[ValidationError]
        .toValidatedNel

    /* Validates the user field. */
    def validateUser(config: Config): Validation[String] =
      validateRequiredStringField(config, "user", MissingOrInvalidUserValue)

    /* Validates the password field. */
    def validatePassword(config: Config): Validation[Option[String]] =
      validateOptionalStringField(config, "password", InvalidStringFieldValue("password"))

    /* Validates the private key file field. */
    def validatePrivateKeyFile(c: Config): Validation[Option[Path]] =
      validateOptionalStringField(c, "privateKeyFile", InvalidPrivateKeyFileValue).map(_.map(Path.of(_)))

    /* Validates the private key passphrase field. */
    def validatePrivateKeyPassphrase(config: Config): Validation[Option[String]] =
      validateOptionalStringField(config, "privateKeyPassphrase", InvalidStringFieldValue("privateKeyPassphrase"))
  }

  import validation.Validation.FieldValidators._

  /** Validates a [[Config]] instance containing a possible [[RemoteConfiguration]] combining all the fields validators defined
    * inside this object.
    *
    * @param configuration
    *   the [[Config]] instance to validate, if no exceptions were thrown while reading it, the corresponding exception otherwise
    * @param name
    *   the name of the configuration to validate
    * @return
    *   a [[scala.Option]] containing the [[RemoteConfiguration]] contained inside the given [[Config]] instance, if valid, a
    *   [[scala.None]] otherwise
    */
  @SuppressWarnings(Array("org.wartremover.warts.Any", "org.wartremover.warts.Nothing"))
  def validateConfiguration(
    configuration: Try[Config],
    name: String
  ): Either[Seq[ValidationError], RemoteConfiguration] =
    configuration
      .toValidated
      .leftMap(_ => InvalidRemoteKey(name))
      .leftWiden[ValidationError]
      .toValidatedNel
      .andThen((c: Config) =>
        validateHost(c)
          .product(validateUser(c))
          .andThen(t =>
            (
              validatePort(c),
              validatePassword(c),
              validatePrivateKeyFile(c),
              validatePrivateKeyPassphrase(c)
            )
              .mapN((p, w, f, pp) =>
                RemoteConfiguration()
                  .host(t._1)
                  .port(p.getOrElse(22))
                  .user(t._2)
                  .password(w)
                  .privateKeyFile(f)
                  .privateKeyPassphrase(pp)
                  .create
              )
          )
      )
      .andThen {
        case Left(s)  => NonEmptyList.fromListUnsafe(s.toList).invalid
        case Right(c) => c.validNel
      }
      .toEither
      .leftMap(_.toList)
}
