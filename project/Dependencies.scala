import sbt._

object Dependencies {
  lazy val typesafeConfig = "com.typesafe" % "config" % "1.4.2"
  lazy val ssh = "com.decodified" %% "scala-ssh" % "0.11.1"
  lazy val logger = "ch.qos.logback" % "logback-classic" % "1.2.11"
  lazy val loggerAPI = "org.slf4j" % "slf4j-api" % "1.7.36"
}
