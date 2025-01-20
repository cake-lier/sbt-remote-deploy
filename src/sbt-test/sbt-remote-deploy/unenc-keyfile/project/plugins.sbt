addSbtPlugin("org.jetbrains.scala" % "sbt-ide-settings" % "1.1.2")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.3.1")
sys.props.get("plugin.version") match {
  case Some(x) => addSbtPlugin("io.github.cake-lier" % "sbt-remote-deploy" % x)
  case _ => sys.error("""|The system property 'plugin.version' is not defined.
                         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
}
