package io.github.cakelier

import java.nio.file.Path

import cats.syntax.all._

import validation.ValidationError
import validation.ValidationError._

/** A configuration for accessing a remote location.
  *
  * This entity represents the configuration parameters which are needed for accessing a remote location, a remote server, through
  * SSH. The hostname and the username to be used are required, while the port is not needed, defaulting to the one used for SSH,
  * which is the port 22. Other data can be specified, such as: the password for the user, if a password-based authentication must
  * be performed, the [[java.nio.file.Path]] to the file containing the private key, if a public key-based authentication is
  * required. Moreover, a passphrase can be specified for the private key, if it has been encrypted. If both the password and the
  * [[java.nio.file.Path]] to the private key were specified, the last one takes precedence and the first one is ignored. If
  * neither is specified, an empty password will be supplied. Last, a fingerprint can be specified for verifying the identity of
  * the remote location to which connecting and fail if not corresponding. Instances of this trait must be constructed through its
  * companion object.
  */
trait RemoteConfiguration {

  /** Returns the hostname of the remote location. */
  val host: String

  /** Returns the port to which establishing a connection to the remote location. */
  val port: Int

  /** Returns the username of the user to use while establishing a connection to the remote location. */
  val user: String

  /** Returns a [[scala.Some]] containing the password to be used during the authentication process, if present, [[scala.None]] if
    * absent.
    */
  val password: Option[String]

  /** Returns a [[scala.Some]] containing the [[java.nio.file.Path]] to the file containing the private key to be used during the
    * authentication process, if present, [[scala.None]] if absent.
    */
  val privateKeyFile: Option[Path]

  /** Returns a [[scala.Some]] containing the passphrase to be used for decrypting the private key to be used during the
    * authentication process, if present, [[scala.None]] if absent.
    */
  val privateKeyPassphrase: Option[String]

  /** Returns a [[scala.Some]] containing the fingerprint to be used for identifying the remote host to which connecting, if
    * present, a [[scala.None]] if absent.
    */
  val fingerprint: Option[String]
}

/** Companion object to the [[RemoteConfiguration]] trait, containing its factory. */
private[cakelier] object RemoteConfiguration {

  /** Factory for creating new instances of [[RemoteConfiguration]].
    *
    * This entity represents an abstract factory for creating new [[RemoteConfiguration]] instances. It regulates how values can
    * be specified and what qualifies as a valid state for the given instance. Its [[RemoteConfiguration.Factory.create]] method
    * will return all encountered errors in case the user tries to complete the creation of a [[RemoteConfiguration]] that is not
    * in a valid state. For being in a valid state, both the hostname and the username must be specified and the first must be a
    * valid hostname or IP address, while the second must be a string without spaces. The port must be a value in the port range,
    * so between 1 and 65535 included. The [[java.nio.file.Path]] must be to a file that exists and can be read. Instances of this
    * trait must be constructed through its companion object.
    *
    * @see
    *   [[https://en.wikipedia.org/wiki/Abstract_factory_pattern]]
    */
  trait Factory {

    /** Sets the hostname value for the [[RemoteConfiguration]] to be created. The provided hostname must be a valid hostname or a
      * valid IP address.
      *
      * @param host
      *   the hostname to be set
      * @return
      *   this [[Factory]] with the hostname set, so as to call all methods in a fluent manner
      */
    def host(host: String): Factory

    /** Sets the port value for the [[RemoteConfiguration]] to be created. The provided port value must be valid, so it must be
      * between 1 and 65535 included.
      *
      * @param port
      *   the port to be set
      * @return
      *   this [[Factory]] with the port set, so as to call all methods in a fluent manner
      */
    def port(port: Int): Factory

    /** Sets the user value for the [[RemoteConfiguration]] to be created. The provided user value must be valid, so it must be a
      * nonempty string without any whitespace.
      *
      * @param user
      *   the user to be set
      * @return
      *   this [[Factory]] with the user set, so as to call all methods in a fluent manner
      */
    def user(user: String): Factory

    /** Sets the password value for the [[RemoteConfiguration]] to be created.
      *
      * @param password
      *   a [[scala.Option]] containing the password to be set, or a [[scala.None]] if no password is to be set
      * @return
      *   this [[Factory]] with the password set, so as to call all methods in a fluent manner
      */
    def password(password: Option[String]): Factory

    /** Sets the [[java.nio.file.Path]] to the file containing the private key to be used in the [[RemoteConfiguration]] to be
      * created. The provided path must be valid, so it must refer to a existing, readable file. If an invalid value is provided,
      * nothing happens.
      *
      * @param privateKeyFile
      *   a [[scala.Option]] containing the [[java.nio.file.Path]] to the private key file to be set, or a [[scala.None]] if no
      *   private key file is to be set
      * @return
      *   this [[Factory]] with the [[java.nio.file.Path]] to the private key file set, so as to call all methods in a fluent
      *   manner
      */
    def privateKeyFile(privateKeyFile: Option[Path]): Factory

    /** Sets the passphrase value for the private key to be used in the [[RemoteConfiguration]] to be created.
      *
      * @param privateKeyPassphrase
      *   a [[scala.Option]] containing the passphrase for the private key to be set, or a [[scala.None]] if no private key
      *   passphrase is to be set
      * @return
      *   this [[Factory]] with the passphrase for the private key set, so as to call all methods in a fluent manner
      */
    def privateKeyPassphrase(privateKeyPassphrase: Option[String]): Factory

    /** Sets the fingerprint value for the [[RemoteConfiguration]] to be created. The fingerprint can be specified in either an
      * MD5 colon-delimited format (16 hexadecimal octets, delimited by a colon), or in a Base64 encoded format for SHA-1 or
      * SHA-256 fingerprints. The MD5 format can be prefixed, but it is not mandatory, with the string "MD5:", while the SHA-1 and
      * SHA-256 formatted fingerprints must be prefixed respectively with "SHA1:" and "SHA256:" strings. The terminal "=" symbols
      * are optional.
      *
      * @param fingerprint
      *   a [[scala.Option]] containing the fingerprint to be set, or a [[scala.None]] if no fingerprint is to be set
      * @return
      *   this [[Factory]] with the fingerprint set, so as to call all methods in a fluent manner
      */
    def fingerprint(fingerprint: Option[String]): Factory

    /** Creates a new instance of the [[RemoteConfiguration]] trait, if all configuration parameters were correctly specified and
      * the factory is in a valid state, otherwise it will return all the encountered [[ValidationError]]s while creating the new
      * instance.
      *
      * @return
      *   a [[scala.Either]] containing a new instance of the [[RemoteConfiguration]] trait made from the supplied parameters, if
      *   those were valid, a [[scala.Seq]] with all encountered [[ValidationError]]s otherwise
      */
    def create: Either[Seq[ValidationError], RemoteConfiguration]
  }

  /** Companion object to its [[Factory]] trait, containing its factory method. */
  object Factory {

    /* A case class implementation of the Factory trait. */
    private final case class FactoryImpl(
      host: Option[String],
      port: Int,
      user: Option[String],
      password: Option[String],
      privateKeyFile: Option[Path],
      privateKeyPassphrase: Option[String],
      fingerprint: Option[String]
    ) extends Factory {

      override def host(host: String): Factory = copy(host = Some(host))

      override def port(port: Int): Factory = copy(port = port)

      override def user(user: String): Factory = copy(user = Some(user))

      override def password(password: Option[String]): Factory = copy(password = password)

      override def privateKeyFile(privateKeyFile: Option[Path]): Factory = copy(privateKeyFile = privateKeyFile)

      override def privateKeyPassphrase(privateKeyPassphrase: Option[String]): Factory =
        copy(privateKeyPassphrase = privateKeyPassphrase)

      override def fingerprint(fingerprint: Option[String]): Factory = copy(fingerprint = fingerprint)

      @SuppressWarnings(Array("org.wartremover.warts.Any", "org.wartremover.warts.Nothing"))
      override def create: Either[Seq[ValidationError], RemoteConfiguration] = {
        val hostnameRegex =
          "^(?:(?:[a-zA-Z\\d]|[a-zA-Z\\d][a-zA-Z\\d\\-]*[a-zA-Z\\d])\\.)*(?:[A-Za-z\\d]|[A-Za-z\\d][A-Za-z\\d\\-]*[A-Za-z\\d])$".r
        val ipRegex =
          "^(?:(?:\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])\\.){3}(?:\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])$".r
        val userRegex = "^\\S+$".r
        val md5FingerprintRegex = "^(?:MD5:)?[\\da-f]{2}(?::[\\da-f]{2}){15}$".r
        val shaFingerprintRegex = "^SHA(?:1|256):(?:[A-Za-z\\d+/]{4})*(?:[A-Za-z\\d+/]{2}(?:==)?|[A-Za-z\\d+/]{3}=?)?$".r
        (
          host
            .toValid(MissingFieldValue("host"))
            .leftWiden[ValidationError]
            .toValidatedNel
            .andThen((s: String) =>
              s match {
                case h @ (hostnameRegex() | ipRegex()) => h.validNel[ValidationError]
                case _                                 => InvalidHostValue.invalidNel[String]
              }
            ),
          port.validNel[ValidationError].andThen {
            case p if p >= 1 && p <= 65535 => p.validNel[ValidationError]
            case _                         => InvalidPortValue.invalidNel[Int]
          },
          user
            .toValid(MissingFieldValue("user"))
            .leftWiden[ValidationError]
            .toValidatedNel
            .andThen((s: String) =>
              s match {
                case u @ userRegex() => u.validNel[ValidationError]
                case _               => InvalidUserValue.invalidNel[String]
              }
            ),
          privateKeyFile.validNel[ValidationError].andThen {
            case f if f.forall(p => p.toFile.exists && p.toFile.canRead) => f.validNel[ValidationError]
            case _                                                       => InvalidPrivateKeyFileValue.invalidNel[Option[Path]]
          },
          fingerprint.validNel[ValidationError].andThen {
            case f if f.forall(v => v.matches(md5FingerprintRegex.regex) || v.matches(shaFingerprintRegex.regex)) =>
              f.validNel[ValidationError]
            case _ => InvalidFingerPrintValue.invalidNel[Option[String]]
          }
        )
          .mapN((h, p, u, k, f) => RemoteConfigurationImpl(h, p, u, password, k, privateKeyPassphrase, f))
          .toEither
          .leftMap(_.toList)
      }
    }

    /** Returns a new empty instance of the [[Factory]] trait. */
    def apply(): Factory =
      FactoryImpl(
        host = None,
        port = 22,
        user = None,
        password = None,
        privateKeyFile = None,
        privateKeyPassphrase = None,
        fingerprint = None
      )
  }

  /* A case class implementation of the RemoteConfiguration trait. */
  private final case class RemoteConfigurationImpl(
    host: String,
    port: Int,
    user: String,
    password: Option[String],
    privateKeyFile: Option[Path],
    privateKeyPassphrase: Option[String],
    fingerprint: Option[String]
  ) extends RemoteConfiguration

  /** Returns a new empty instance of the [[RemoteConfiguration.Factory]] trait, so as to create a new [[RemoteConfiguration]]
    * instance.
    */
  def apply(): RemoteConfiguration.Factory = RemoteConfiguration.Factory()
}
