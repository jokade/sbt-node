addSbtPlugin("de.surfice" % "sbt-npm" % sys.props.getOrElse("plugin.version", sys.error("'plugin.version' environment variable is not set")))

