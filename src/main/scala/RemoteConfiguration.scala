package io.github.cakelier

import java.nio.file.Path

trait RemoteConfiguration {

  val configurationName: String

  val remoteLocation: RemoteLocation

  val privateKeyFile: Option[Path]

  val passphrase: Option[String]
}

object RemoteConfiguration {

  private case class RemoteConfigurationImpl(
    configurationName: String,
    remoteLocation: RemoteLocation,
    privateKeyFile: Option[Path],
    passphrase: Option[String]
  ) extends RemoteConfiguration

  def apply(
    configurationName: String,
    remoteLocation: RemoteLocation,
    privateKeyFile: Option[Path] = None,
    passphrase: Option[String] = None
  ): RemoteConfiguration =
    RemoteConfigurationImpl(configurationName, remoteLocation, privateKeyFile, passphrase)

  def apply(
    configurationName: String,
    remoteLocation: RemoteLocation,
    privateKeyFile: Path
  ): RemoteConfiguration = RemoteConfigurationImpl(configurationName, remoteLocation, Some(privateKeyFile), None)

  def apply(
    configurationName: String,
    remoteLocation: RemoteLocation,
    privateKeyFile: Path,
    passphrase: String
  ): RemoteConfiguration = RemoteConfigurationImpl(configurationName, remoteLocation, Some(privateKeyFile), Some(passphrase))
}
