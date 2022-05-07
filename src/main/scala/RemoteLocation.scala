package io.github.cakelier

trait RemoteLocation {

  val host: String

  val user: String

  val password: Option[String]

  val port: Option[Int]
}

object RemoteLocation {

  private case class RemoteLocationImpl(host: String, user: String, password: Option[String], port: Option[Int])
    extends RemoteLocation {

    override def toString: String = s"$user${password.map(":" + _).getOrElse("")}@$host${port.map(":" + _).getOrElse("")}"
  }

  def apply(host: String, user: String, password: Option[String] = None, port: Option[Int] = None): RemoteLocation =
    RemoteLocationImpl(host, user, password, port)

  def apply(host: String, user: String, password: String): RemoteLocation =
    RemoteLocationImpl(host, user, Some(password), None)

  def apply(host: String, user: String, port: Int): RemoteLocation =
    RemoteLocationImpl(host, user, None, Some(port))

  def apply(host: String, user: String, password: String, port: Int): RemoteLocation =
    RemoteLocationImpl(host, user, Some(password), Some(port))
}
