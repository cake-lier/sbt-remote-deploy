package io.github.cakelier

import java.nio.file.Path
import scala.util.matching.Regex

/** A configuration for accessing a remote location.
  *
  * This entity represents the configuration parameters which are needed for accessing a remote location, a remote server, through
  * SSH. The hostname and the username to be used are required, while the port is not needed, defaulting to the one used for SSH,
  * which is the port 22. Other data can be specified, such as: the password for the user, if a password-based authentication must
  * be performed, the [[java.nio.file.Path]] to the file containing the private key, if a public key-based authentication is
  * required. Moreover, a passphrase can be specified for the private key, if it has been encrypted. If both the password and the
  * [[java.nio.file.Path]] to the private key were specified, the last one takes precedence and the first one is ignored. If
  * neither is specified, an empty password will be supplied. Instances of this trait must be constructed through its companion
  * object.
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
}

/** Companion object to the [[RemoteConfiguration]] trait, containing its factory. */
private[cakelier] object RemoteConfiguration {

  /** Factory for creating new instances of [[RemoteConfiguration]].
    *
    * This entity represents an abstract factory for creating new [[RemoteConfiguration]] instances. It regulates how values can
    * be specified and what qualifies as a valid state for the given instance. Its [[RemoteConfiguration.Factory.create]] method
    * will return a [[scala.None]] in case the user tries to complete the creation of a [[RemoteConfiguration]] that is not in a
    * valid state. For this to be true, both the hostname and the username must be specified and the first must be a valid
    * hostname or IP address, while the second must be a string without spaces. The port must be a value in the port range, so
    * between 1 and 65535 included. The [[java.nio.file.Path]] must be to a file that exists and can be read. Instances of this
    * trait must be constructed through its companion object.
    *
    * @see
    *   [[https://en.wikipedia.org/wiki/Abstract_factory_pattern]]
    */
  trait Factory {

    /** Sets the hostname value for the [[RemoteConfiguration]] to be created. The provided hostname must be a valid hostname or a
      * valid IP address. If an invalid value is provided, nothing happens.
      *
      * @param host
      *   the hostname to be set
      * @return
      *   this [[Factory]] with the hostname set, so as to call all methods in a fluent manner
      */
    def host(host: String): Factory

    /** Sets the port value for the [[RemoteConfiguration]] to be created. The provided port value must be valid, so it must be
      * between 1 and 65535 included. If an invalid value is provided, nothing happens.
      *
      * @param port
      *   the port to be set
      * @return
      *   this [[Factory]] with the port set, so as to call all methods in a fluent manner
      */
    def port(port: Int): Factory

    /** Sets the user value for the [[RemoteConfiguration]] to be created. The provided user value must be valid, so it must be a
      * nonempty string without any whitespace. If an invalid value is provided, nothing happens.
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
      *   a [[scala.Option]] containing the [[java.nio.file.Path]] to the private key file to be set
      * @return
      *   this [[Factory]] with the [[java.nio.file.Path]] to the private key file set, so as to call all methods in a fluent
      *   manner
      */
    def privateKeyFile(privateKeyFile: Option[Path]): Factory

    /** Sets the passphrase value for the private key to be used in the [[RemoteConfiguration]] to be created.
      *
      * @param privateKeyPassphrase
      *   a [[scala.Option]] containing the passphrase for the private key to be set
      * @return
      *   this [[Factory]] with the passphrase for the private key set, so as to call all methods in a fluent manner
      */
    def privateKeyPassphrase(privateKeyPassphrase: Option[String]): Factory

    /** Creates a new instance of the [[RemoteConfiguration]] trait, if all configuration parameters were correctly specified and
      * the factory is in a valid state, otherwise it will return a [[scala.None]].
      *
      * @return
      *   a [[scala.Some]] containing a new instance of the [[RemoteConfiguration]] trait containing the supplied parameters, if
      *   those were valid, a [[scala.None]] otherwise
      */
    def create: Option[RemoteConfiguration]
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
      privateKeyPassphrase: Option[String]
    ) extends Factory {
      private val hostnameRegex: Regex =
        "^(?:(?:[a-zA-Z\\d]|[a-zA-Z\\d][a-zA-Z\\d\\-]*[a-zA-Z\\d])\\.)*(?:[A-Za-z\\d]|[A-Za-z\\d][A-Za-z\\d\\-]*[A-Za-z\\d])$".r
      private val ipRegex: Regex =
        "^(?:(?:\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])\\.){3}(?:\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])$".r

      override def host(host: String): Factory = host match {
        case hostnameRegex() | ipRegex() => copy(host = Some(host))
        case _                           => this
      }

      override def port(port: Int): Factory = port match {
        case v if v >= 1 && v <= 65535 => copy(port = port)
        case _                         => this
      }

      private val userRegex = "^\\S+$".r

      override def user(user: String): Factory = user match {
        case userRegex() => copy(user = Some(user))
        case _           => this
      }

      override def password(password: Option[String]): Factory = copy(password = password)

      override def privateKeyFile(privateKeyFile: Option[Path]): Factory =
        copy(privateKeyFile = privateKeyFile.filter(p => p.toFile.exists && p.toFile.canRead))

      override def privateKeyPassphrase(privateKeyPassphrase: Option[String]): Factory =
        copy(privateKeyPassphrase = privateKeyPassphrase)

      override def create: Option[RemoteConfiguration] = {
        for {
          h <- host
          u <- user
        } yield RemoteConfigurationImpl(h, port, u, password, privateKeyFile, privateKeyPassphrase)
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
        privateKeyPassphrase = None
      )
  }

  /* A case class implementation of the RemoteConfiguration trait. */
  private final case class RemoteConfigurationImpl(
    host: String,
    port: Int,
    user: String,
    password: Option[String],
    privateKeyFile: Option[Path],
    privateKeyPassphrase: Option[String]
  ) extends RemoteConfiguration

  /** Returns a new empty instance of the [[RemoteConfiguration.Factory]] trait, so as to create a new [[RemoteConfiguration]]
    * instance.
    */
  def apply(): RemoteConfiguration.Factory = RemoteConfiguration.Factory()
}
