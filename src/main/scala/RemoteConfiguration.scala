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

  private case class RemoteConfigurationImpl(
    host: String,
    port: Int,
    user: String,
    password: Option[String],
    privateKeyFile: Option[Path],
    privateKeyPassphrase: Option[String]
  ) extends RemoteConfiguration

  def apply(
    host: String,
    port: Int,
    user: String,
    password: Option[String],
    privateKeyFile: Option[Path] = None,
    privateKeyPassphrase: Option[String] = None
  ): RemoteConfiguration =
    RemoteConfigurationImpl(host, port, user, password, privateKeyFile, privateKeyPassphrase)
}
