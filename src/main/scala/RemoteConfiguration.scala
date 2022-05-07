package io.github.cakelier

import java.nio.file.Path

trait RemoteConfiguration {

  val configurationName: String

  val remoteLocation: RemoteLocation

  val passphrase: Option[String]

  val privateKeyFile: Option[Path]
}

object RemoteConfiguration {

  private case class RemoteConfigurationImpl(
    configurationName: String,
    remoteLocation: RemoteLocation,
    passphrase: Option[String],
    privateKeyFile: Option[Path]
  ) extends RemoteConfiguration

  def apply(
    configurationName: String,
    remoteLocation: RemoteLocation,
    passphrase: Option[String] = None,
    privateKeyFile: Option[Path] = None
  ): RemoteConfiguration = RemoteConfigurationImpl(configurationName, remoteLocation, passphrase, privateKeyFile)
}
