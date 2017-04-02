//     Project: SBT NPM
//      Module:
// Description:
package de.surfice.sbtnpm.utils

import sbt._

case class NodeCommand(nodeTargetDir: File, pkg: String, cmdName: String) extends ExternalCommand {
  override lazy val cmdPath = (nodeTargetDir.getAbsoluteFile / "node_modules" / pkg / "bin" / cmdName).toString
}

