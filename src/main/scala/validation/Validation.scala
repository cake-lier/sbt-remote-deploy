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

    /* Type representing the generic result of validating a field which contains values of type A. */
    private type Validation[A] = ValidatedNel[ValidationError, A]

    /* Validates a required string field. */
    private def validateRequiredStringField(config: Config, name: String): Validation[String] =
      Try(config.getString(name))
        .toValidated
        .leftMap(_ => MissingOrInvalidStringFieldValue(name))
        .leftWiden[ValidationError]
        .toValidatedNel

    /* Validates an optional string field. */
    private def validateOptionalStringField(config: Config, name: String, error: ValidationError): Validation[Option[String]] =
      (if (config.root().containsKey(name)) Try(Some(config.getString(name))) else Success(None))
        .toValidated
        .leftMap(_ => error)
        .leftWiden[ValidationError]
        .toValidatedNel

    /* Validates the host field. */
    def validateHost(config: Config): Validation[String] =
      validateRequiredStringField(config, "host")

    /* Validates the port field. */
    def validatePort(config: Config): Validation[Option[Int]] =
      (if (config.root().containsKey("port")) Try(Some(config.getInt("port"))) else Success(None))
        .toValidated
        .leftMap(_ => InvalidPortValue)
        .leftWiden[ValidationError]
        .toValidatedNel

    /* Validates the user field. */
    def validateUser(config: Config): Validation[String] =
      validateRequiredStringField(config, "user")

    /* Validates the password field. */
    def validatePassword(config: Config): Validation[Option[String]] =
      validateOptionalStringField(config, "password", InvalidStringFieldValue("password"))

    /* Validates the private key file field. */
    def validatePrivateKeyFile(c: Config): Validation[Option[Path]] =
      validateOptionalStringField(c, "privateKeyFile", InvalidPrivateKeyFileValue).map(_.map(Path.of(_)))

    /* Validates the private key passphrase field. */
    def validatePrivateKeyPassphrase(config: Config): Validation[Option[String]] =
      validateOptionalStringField(config, "privateKeyPassphrase", InvalidStringFieldValue("privateKeyPassphrase"))

    def validateFingerprint(config: Config): Validation[Option[String]] =
      validateOptionalStringField(config, "fingerprint", InvalidFingerprintValue)

    def validateVerifyIdentity(config: Config): Validation[Option[Boolean]] =
      (if (config.root().containsKey("verifyIdentity")) Try(Some(config.getBoolean("verifyIdentity"))) else Success(None))
        .toValidated
        .leftMap(_ => InvalidVerifyIdentityValue)
        .leftWiden[ValidationError]
        .toValidatedNel
  }

  import validation.Validation.FieldValidators._

  /** Validates a [[Config]] instance containing a possible [[RemoteConfiguration]] combining all the fields validators defined
    * inside this object. This method will return all [[ValidationError]]s encountered during the [[Config]] parsing if it did not
    * contain a valid [[RemoteConfiguration]].
    *
    * @param configuration
    *   the [[Config]] instance to validate, if no exceptions were thrown while reading it, the corresponding exception otherwise
    * @param name
    *   the name of the configuration to validate
    * @return
    *   a [[scala.Either]] containing the [[RemoteConfiguration]] contained inside the given [[Config]] instance, if this was
    *   valid, a [[scala.Seq]] with all [[ValidationError]]s encountered while parsing the [[Config]] instance otherwise
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
              validatePrivateKeyPassphrase(c),
              validateFingerprint(c),
              validateVerifyIdentity(c)
            )
              .mapN((p, w, k, kp, f, i) =>
                RemoteConfiguration()
                  .host(t._1)
                  .port(p.getOrElse(22))
                  .user(t._2)
                  .password(w)
                  .privateKeyFile(k)
                  .privateKeyPassphrase(kp)
                  .fingerprint(f)
                  .verifyIdentity(i.getOrElse(true))
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
