//     Project: sbt-node
//      Module:
// Description:
package de.surfice.sbtnpm.sass

import de.surfice.sbtnpm.NpmPlugin
import de.surfice.sbtnpm.NpmPlugin.autoImport._
import de.surfice.sbtnpm.utils.{ExternalCommand, NodeCommand}
import org.scalajs.sbtplugin.{ScalaJSPluginInternal, Stage}
import sbt._
import Keys._
import Cache._

object SassPlugin extends AutoPlugin {

  override lazy val requires = NpmPlugin

  /**
   * @groupname tasks Tasks
   * @groupname settings Settings
   */
  object autoImport {
    /**
     * Version of the `node-sass` npm module.
     */
    val nodeSassVersion: SettingKey[String] =
      settingKey[String]("node-sass version")

    /**
     * Defines the node-sass command to be used
     */
    val nodeSassCmd: SettingKey[NodeCommand] =
      settingKey[NodeCommand]("node-sass command")

    val nodeSassTarget: SettingKey[File] =
      settingKey[File]("target directory for compiled sass files (may be scoped to fastOptJS and fullOptJS)")

    val nodeSassSourceDirectories: SettingKey[Seq[File]] =
      settingKey[Seq[File]]("list of source directories that contain files to be processed by sass (may be scoped to fastOptJS and fullOptJS)")

    val nodeSassInputs: TaskKey[Seq[Attributed[(File,String)]]] =
      taskKey("Contains all sass input files to be processed (may be scoped to fastOptJS and fullOptJS)")

    val nodeSass: TaskKey[Long] =
      taskKey[Long]("Runs the sass compiler")
  }

  import autoImport._

  override lazy val projectSettings: Seq[Def.Setting[_]] = Seq(
    nodeSassVersion := "~4.5.2",

    nodeSassCmd := NodeCommand(npmNodeModulesDir.value,"node-sass","node-sass"),

    defineSassSourceDirectories(Compile),

    defineSassTarget(Compile),

    defineSassInputs(Compile),

    defineSassTask(Compile,nodeSassTarget in Compile),

    npmDevDependencies += "node-sass" -> nodeSassVersion.value
  ) ++
    perScalaJSStageSettings(Stage.FullOpt) ++
    perScalaJSStageSettings(Stage.FastOpt)


  private def perScalaJSStageSettings(stage: Stage): Seq[Def.Setting[_]] = {

    val stageTask = ScalaJSPluginInternal.stageKeys(stage)

    Seq(
      defineSassSourceDirectories(stageTask),
      defineSassTarget(stageTask),
      defineSassInputs(stageTask),
      nodeSassTarget in stageTask := (crossTarget in (Compile,stageTask)).value,
      defineSassTask(stageTask,nodeSassTarget in stageTask)
    )
  }

  private def defineSassTask(scope: Any, targetDir: SettingKey[File]) = {
    val (task, inputs, targetDir) = scope match {
      case scoped: Scoped => (nodeSass in scoped, nodeSassInputs in (Compile,scoped), nodeSassTarget in (Compile,scoped))
      case config: Configuration => (nodeSass in config, nodeSassInputs in config, nodeSassTarget in config)
    }
    task := {
      val lastrun = task.previous
      npmInstall.value
      val cwd = targetDir.value
      val sass = nodeSassCmd.value
      val logger = streams.value.log
      var modified = false
      inputs.value.foreach{ f =>
        val (src,relDest) = f.data
        val dest = cwd / relDest
        if(lastrun.isEmpty || !dest.exists() || src.lastModified()>lastrun.get) {
          sass.run(src.getAbsolutePath, dest.getAbsolutePath)(cwd,logger)
          modified = true
        }
      }
      if(modified)
        new java.util.Date().getTime
      else
        lastrun.get
    }
  }

  private def defineSassInputs(scope: Any) = {
    val (task,sourceDirs) = scope match {
      case scoped: Scoped => (nodeSassInputs in scoped,nodeSassSourceDirectories in (Compile,scoped))
      case config: Configuration => (nodeSassInputs in config, nodeSassSourceDirectories in config)
    }
    task := {
      Attributed.blankSeq( sourceDirs.value flatMap { dir =>
        val fs = (dir ** "*").get.filter{ f =>
          val name = f.getName
          f.isFile && !name.startsWith("_") && ( name.endsWith(".scss") || name.endsWith(".sass") || name.endsWith(".css") )
        }
        fs.map{ f =>
          val path = f.relativeTo(dir).get.getPath

          (f, path.substring(0,path.lastIndexOf("."))+".css" )
        }
      })
    }
  }

  private def defineSassSourceDirectories(scope: Any) = scope match {
    case scoped: Scoped =>
      nodeSassSourceDirectories in (Compile,scoped) := (resourceDirectories in (Compile,scoped)).value
    case config: Configuration => nodeSassSourceDirectories in config := (resourceDirectories in config).value
  }

  private def defineSassTarget(scope: Any) = scope match {
    case scoped: Scoped =>
      nodeSassTarget in (Compile,scoped) := (crossTarget in (Compile,scoped)).value / "css"
    case config: Configuration =>
      nodeSassTarget in config := (crossTarget in config).value / "css"
  }

}
