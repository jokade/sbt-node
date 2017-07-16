//     Project: sbt-node
//      Module:
// Description:
package de.surfice.sbtnpm.utils
import sbinary.{Input, Output}
import sbt._

import scala.annotation.tailrec

case class FileWithLastrun(file: File, lastrun: Long) {
  def needsUpdate: Boolean = !file.exists() || file.lastModified > lastrun
  def needsUpdate(reference: sbt.File): Boolean =
    needsUpdate || reference.lastModified() > lastrun
  def needsUpdateComparedToConfig(baseDir: File): Boolean =
    FileWithLastrun.configNewerThanTimestamp(
      FileWithLastrun.findProjectRoot(baseDir),
      lastrun) || needsUpdate
}
object FileWithLastrun {
  def apply(file: sbt.File): FileWithLastrun = apply(file,new java.util.Date().getTime)

  private var _configClassesDir: File = _

  private def configNewerThanTimestamp(projectRoot: File, lastrun: Long): Boolean = {
    if(_configClassesDir == null) this.synchronized {
      _configClassesDir = projectRoot / "project" / "target" / "config-classes"
    }
    _configClassesDir.lastModified > lastrun
  }

  // TODO: handle detection of project root directories without build.sbt
  @tailrec
  private def findProjectRoot(baseDir: File): File =
    if(baseDir.isDirectory && (baseDir / "build.sbt").canRead) {
      baseDir
    }
    else if(baseDir.getParentFile == baseDir)
      throw new RuntimeException("Could not locate project root")
    else
      findProjectRoot(baseDir.getParentFile)


  implicit object format extends sbinary.Format[FileWithLastrun] {
    import sbt.Cache._
    override def reads(in: Input): FileWithLastrun = FileWithLastrun(
      new sbt.File(StringFormat.reads(in)),
      LongFormat.reads(in)
    )
    override def writes(out: Output, value: FileWithLastrun): Unit = {
      StringFormat.writes(out,value.file.getCanonicalPath)
      LongFormat.writes(out,value.lastrun)
    }
  }
}
