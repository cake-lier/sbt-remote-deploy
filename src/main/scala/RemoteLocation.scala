package io.github.cakelier

trait RemoteLocation {

  val host: String

  val port: Option[Int]

  val user: String

  val password: Option[String]
}

object RemoteLocation {

  private case class RemoteLocationImpl(host: String, port: Option[Int], user: String, password: Option[String])
    extends RemoteLocation {

    override def toString: String = s"$user${password.map(":" + _).getOrElse("")}@$host${port.map(":" + _).getOrElse("")}"
  }

  def apply(
    host: String,
    port: Option[Int] = None,
    user: String,
    password: Option[String] = None
  ): RemoteLocation =
    RemoteLocationImpl(host, port, user, password)
}
