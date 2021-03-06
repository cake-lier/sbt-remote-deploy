import sbt._

object Dependencies {
  lazy val typesafeConfig = "com.typesafe" % "config" % "1.4.2"
  lazy val ssh = "com.decodified" %% "scala-ssh" % "0.11.1"
  lazy val cats = "org.typelevel" %% "cats-core" % "2.8.0"
  lazy val scalactic = "org.scalactic" %% "scalactic" % "3.2.12"
  // lazy val sshj = "com.hierynomus" % "sshj" % "0.33.0"
}
