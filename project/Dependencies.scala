import sbt._

object Dependencies {
  lazy val typesafeConfig = "com.typesafe" % "config" % "1.4.3"
  lazy val cats = "org.typelevel" %% "cats-core" % "2.10.0"
  lazy val scalactic = "org.scalactic" %% "scalactic" % "3.2.18"
  lazy val sshj = "com.hierynomus" % "sshj" % "0.38.0"
}
