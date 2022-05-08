package io.github.cakelier

import java.nio.file.Paths

trait ConfigurationFactory {

  def host(host: String): Unit

  def port(port: Int): Unit

  def user(user: String): Unit

  def password(password: String): Unit

  def privateKeyFile(privateKeyFile: String): Unit

  def privateKeyPassphrase(privateKeyPassphrase: String): Unit

  def build: RemoteConfiguration

  def reset(): Unit
}

object ConfigurationFactory {

  private class ConfigurationFactoryImpl extends ConfigurationFactory {

    private var host: Option[String] = None

    private var port: Int = 22

    private var user: Option[String] = None

    private var password: Option[String] = None

    private var privateKeyFile: Option[String] = None

    private var privateKeyPassphrase: Option[String] = None

    override def host(host: String): Unit = this.host = Some(host)

    override def port(port: Int): Unit = this.port = port

    override def user(user: String): Unit = this.user = Some(user)

    override def password(password: String): Unit = this.password = Some(password)

    override def privateKeyFile(privateKeyFile: String): Unit = this.privateKeyFile = Some(privateKeyFile)

    override def privateKeyPassphrase(privateKeyPassphrase: String): Unit = this.privateKeyPassphrase = Some(privateKeyPassphrase)

    override def build: RemoteConfiguration = {
      if (host.isEmpty || user.isEmpty) {
        throw new NoSuchElementException(
          if (host.isEmpty)
            "Missing configuration host value"
          else
            "Missing configuration user value"
        )
      }
      RemoteConfiguration(host.get, port, user.get, password, privateKeyFile.map(Paths.get(_)), privateKeyPassphrase)
    }

    override def reset(): Unit = {
      host = None
      port = 22
      user = None
      password = None
      privateKeyFile = None
      privateKeyPassphrase = None
    }
  }

  def apply(): ConfigurationFactory = new ConfigurationFactoryImpl
}
