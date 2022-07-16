package io.github.cakelier

import java.nio.file.Path

trait RemoteConfiguration {

  val host: String

  val port: Int

  val user: String

  val password: Option[String]

  val privateKeyFile: Option[Path]

  val privateKeyPassphrase: Option[String]
}

object RemoteConfiguration {

  trait Factory {

    def host(host: String): Factory

    def port(port: Int): Factory

    def user(user: String): Factory

    def password(password: Option[String]): Factory

    def privateKeyFile(privateKeyFile: Option[Path]): Factory

    def privateKeyPassphrase(privateKeyPassphrase: Option[String]): Factory

    def create: Option[RemoteConfiguration]
  }

  object Factory {

    private final case class FactoryImpl(
      host: Option[String],
      port: Int,
      user: Option[String],
      password: Option[String],
      privateKeyFile: Option[Path],
      privateKeyPassphrase: Option[String]
    ) extends Factory {

      override def host(host: String): Factory = copy(host = Some(host))

      override def port(port: Int): Factory = copy(port = port)

      override def user(user: String): Factory = copy(user = Some(user))

      override def password(password: Option[String]): Factory = copy(password = password)

      override def privateKeyFile(privateKeyFile: Option[Path]): Factory = copy(privateKeyFile = privateKeyFile)

      override def privateKeyPassphrase(privateKeyPassphrase: Option[String]): Factory =
        copy(privateKeyPassphrase = privateKeyPassphrase)

      override def create: Option[RemoteConfiguration] = {
        for {
          h <- host
          u <- user
        } yield RemoteConfigurationImpl(h, port, u, password, privateKeyFile, privateKeyPassphrase)
      }
    }

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

  private final case class RemoteConfigurationImpl(
    host: String,
    port: Int,
    user: String,
    password: Option[String],
    privateKeyFile: Option[Path],
    privateKeyPassphrase: Option[String]
  ) extends RemoteConfiguration

  def apply(): RemoteConfiguration.Factory = RemoteConfiguration.Factory()
}
