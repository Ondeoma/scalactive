sys.props.get("plugin.version") match {
  case Some(v) => addSbtPlugin("io.github.ondeoma" % "sbt-scalactive" % v)
  case None => sys.error("plugin.version is not set. Run this test via `sbt sbtScalactive/scripted`.")
}
