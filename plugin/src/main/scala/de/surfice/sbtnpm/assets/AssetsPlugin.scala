//     Project: sbt-node
//      Module:
// Description:
package de.surfice.sbtnpm.assets

import sbt.{Def, _}
import Keys._
import de.surfice.sbtnpm.NpmPlugin
import org.scalajs.sbtplugin.{ScalaJSPluginInternal, Stage}

object AssetsPlugin extends AutoPlugin {
  override def requires: Plugins = NpmPlugin

  object autoImport {
    val NodeAssets: Configuration = config("nodeAssets") describedAs("Configuration for sbt-node assets pipeline")

    val nodeAssetsStage: TaskKey[File] =
      taskKey[File]("Runs the node assest pipeline for the current scope (fastOptJS or fullOptJS)")
  }

  import autoImport._
  import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    resourceDirectories in (NodeAssets,fastOptJS) := Seq( sourceDirectory.value / "main" / "public" ),
    resourceDirectories in (NodeAssets,fullOptJS) := Seq( sourceDirectory.value / "main" / "public" ),

    crossTarget in (NodeAssets,fastOptJS) := (crossTarget in fastOptJS).value / "fastopt",
    crossTarget in (NodeAssets,fullOptJS) := (crossTarget in fastOptJS).value / "fullopt",

    crossTarget in (Compile,fastOptJS) := (crossTarget in (NodeAssets,fastOptJS)).value,
    crossTarget in (Compile,fullOptJS) := (crossTarget in (NodeAssets,fullOptJS)).value
  ) ++
    perScalaJSStageSettings(Stage.FullOpt) ++
    perScalaJSStageSettings(Stage.FastOpt)


  private def perScalaJSStageSettings(stage: Stage): Seq[Def.Setting[_]] = {

    val stageTask = ScalaJSPluginInternal.stageKeys(stage)

    Seq(
      defineNodeAssetsStage(stageTask)
    )
  }

  def defineNodeAssetsStage(scoped: Scoped) =
    nodeAssetsStage in scoped := {
      val targetDir = (crossTarget in (NodeAssets,scoped)).value
      val resourceDirs = (resourceDirectories in (NodeAssets,scoped)).value

      for(dir <- resourceDirs)
        IO.copyDirectory(dir,targetDir)

      targetDir
    }

}
