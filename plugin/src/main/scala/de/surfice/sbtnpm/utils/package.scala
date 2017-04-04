//     Project: sbt-node
//      Module:
// Description:
package de.surfice.sbtnpm

import sbt._
import Keys._
import Cache._

package object utils {
  def fileWithScalaJSStageSuffix(dir: File, filePrefix: String, stage: Scoped, fileSuffix: String): File =
    dir / (filePrefix + stage.key.toString.dropRight(2).toLowerCase() + fileSuffix)

//  def defineConfigDependentFileTask(taskKey: TaskKey[FileWithLastrun], writeFile: =>File, scope: Option[Any] = None) = {
//    val task = scope match {
//        case Some(scoped:Scoped) => taskKey in scoped
//        case None => taskKey
//    }
//    task := {
//      val lastrun = task.previous
//      if(lastrun.isEmpty || lastrun.get.needsUpdateComparedToConfig(baseDirectory.value)) {
//        FileWithLastrun( writeFile )
//      }
//      else lastrun.get
//    }}
}
