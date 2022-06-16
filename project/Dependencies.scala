import sbt._

object Dependencies {
  lazy val typesafeConfig = "com.typesafe" % "config" % "1.4.2"
  lazy val ssh = "com.decodified" %% "scala-ssh" % "0.11.1"
  lazy val cats = "org.typelevel" %% "cats-core" % "2.8.0"
}
