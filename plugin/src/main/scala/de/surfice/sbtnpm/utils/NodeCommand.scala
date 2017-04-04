//     Project: SBT NPM
//      Module:
// Description:
package de.surfice.sbtnpm.utils

import sbt._

case class NodeCommand(nodeModulesDir: File, pkg: String, cmdName: String) extends ExternalCommand {
  override lazy val cmdPath = (nodeModulesDir.getAbsoluteFile / pkg / "bin" / cmdName).toString
}

