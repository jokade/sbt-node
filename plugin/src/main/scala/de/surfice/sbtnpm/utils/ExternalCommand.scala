//     Project: SBT NPM
//      Module:
// Description:
package de.surfice.sbtnpm.utils

import sbt._

/**
 * Helper for platform-independent handling of external commands.
 */
trait ExternalCommand {
  def run(args: String*)(workingDir: File, logger: Logger): Unit = {
    ExternalCommand.execute(cmdPath +: args, workingDir, logger)
  }

  def cmdPath: String
}


object ExternalCommand {
  def apply(cmdName: String): ExternalCommand = new Impl(cmdName)

  class Impl(val cmdPath: String) extends ExternalCommand

  def execute(cmdLine: Seq[String], cwd: sbt.File, logger: Logger): Unit = {
    val ret = Process(cmdLine) ! logger

    if(ret != 0)
      sys.error(s"Non-zero exit code from command ${cmdLine.head}")

  }

  object npm extends Impl("npm") {
    def install(npmTargetDir: File, logger: Logger): Unit = run("install")(npmTargetDir,logger)
  }
}
