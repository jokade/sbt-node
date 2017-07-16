//     Project: sbt-node
//      Module:
// Description:
package de.surfice.sbtnpm.liteserver

import sbt._
import Keys._
import de.surfice.sbtnpm.{NpmPlugin, utils}
import de.surfice.sbtnpm.utils.{FileWithLastrun, JsonNode, NodeCommand}
import org.scalajs.sbtplugin.{ScalaJSPluginInternal, Stage}

object LiteServerPlugin extends AutoPlugin {

  override lazy val requires = NpmPlugin


  /**
   * @groupname tasks Tasks
   * @groupname settings Settings
   */
  object autoImport {
    val liteServerVersion: SettingKey[String] =
      settingKey("Version of lite-server to be used")

    val liteServerConfigFile: SettingKey[File] =
      settingKey("Path to the node lite-server config file (scoped to fastOptJS or fullOptJS)")

    val liteServerBaseDir: SettingKey[File] =
      settingKey("Base directory from which files are served (scoped to fastOptJS or fullOptJS)")

    val liteServerIndexFile: SettingKey[File] =
      settingKey("Path to the index.html file (scoped to fastOptJS or fullOptJS")

    val liteServerRoutes: SettingKey[Iterable[(String,String)]] =
      settingKey("Entries to be put in the lite-server config 'routes' object (scoped to fastOptJS or fullOptJS)")

    val liteServerCmd: SettingKey[NodeCommand] =
      settingKey("command to run the node lite-server")

    val liteServerWriteConfigFile: TaskKey[FileWithLastrun] =
      taskKey("Create the lite-server config file for the current stage (fastOptJS or fullOptJS)")

    val liteServerStart: TaskKey[Unit] =
      taskKey("Start the node lite-server for the current stage (fastOptJS or fullOptJS")

    val liteServerStop: TaskKey[Unit] =
      taskKey("Stops the node lite-server for the current stage (fastOptJS or fullOptJS")

    val liteServerWriteIndexFile: TaskKey[File] =
      taskKey("Creates the index.html file for the current stage (fastOptJS or fullOptJS")
  }

  import autoImport._
  import NpmPlugin.autoImport._

  override lazy val projectSettings: Seq[Def.Setting[_]] = Seq(
    liteServerVersion := de.surfice.sbtnpm.Versions.liteServer,

    liteServerCmd := NodeCommand(npmNodeModulesDir.value,"lite-server","lite-server"),

    npmDevDependencies += "lite-server" -> liteServerVersion.value
  ) ++
    perScalaJSStageSettings(Stage.FullOpt) ++
    perScalaJSStageSettings(Stage.FastOpt)


  private def perScalaJSStageSettings(stage: Stage): Seq[Def.Setting[_]] = {

    val stageTask = ScalaJSPluginInternal.stageKeys(stage)

    Seq(
      defineLiteServerConfigFile(stageTask),
      defineLiteServerBaseDir(stageTask),
      defineLiteServerIndexFile(stageTask),
      defineLiteServerRoutes(stageTask),
      defineLiteServerWriteConfigFile(stageTask),
      defineLiteServerWriteIndexFile(stageTask),
      defineLiteServerStart(stageTask),
      defineLiteServerStop(stageTask)
    )
  }

  private def defineLiteServerConfigFile(scoped: Scoped) =
    liteServerConfigFile in scoped := utils.fileWithScalaJSStageSuffix( (crossTarget in (Compile,scoped)).value,"bs-config-",scoped,".json")

  private def defineLiteServerBaseDir(scoped: Scoped) =
    liteServerBaseDir in scoped := (crossTarget in (Compile,scoped)).value

  private def defineLiteServerIndexFile(scoped: Scoped) =
    liteServerIndexFile in scoped := {
      val baseDir = (resourceDirectory in (Compile,scoped)).value
      if(scoped.key.label == "fastOptJS")
        utils.fileWithScalaJSStageSuffix(baseDir,"index-",scoped,".html")
      else
        baseDir / "index.html"
    }

  private def defineLiteServerRoutes(scoped: Scoped) =
    liteServerRoutes in scoped := Seq("/node_modules/" -> npmNodeModulesDir.value.getAbsolutePath)

  private def defineLiteServerWriteConfigFile(scoped: Scoped) =
    liteServerWriteConfigFile in scoped := {
      val lastrun = (liteServerWriteConfigFile in scoped).previous
      val file = (liteServerConfigFile in scoped).value
      val baseDir = (liteServerBaseDir in scoped).value
      val indexFile = (liteServerWriteIndexFile in scoped).value
      val indexPath = indexFile.relativeTo(baseDir) match {
        case Some(p) => p.getPath()
        case None =>
          streams.value.log.error(s"index file $indexFile must be a child of base directory $baseDir")
          ""
      }


      if(lastrun.isEmpty || lastrun.get.needsUpdateComparedToConfig(baseDirectory.value)) {
        writeConfigFile(file,
          baseDir.getAbsolutePath,
          indexPath,
          (liteServerRoutes in scoped).value)
        FileWithLastrun(file)
      }
      else lastrun.get
    }

  private def defineLiteServerStart(scoped: Scoped) =
    liteServerStart in scoped := {
      npmInstall.value
      (liteServerWriteConfigFile in scoped).value
      val cmd = liteServerCmd.value
      val configFile = (liteServerConfigFile in scoped).value

      cmd.startAndStore("-c",configFile.getAbsolutePath)(scoped,streams.value.log)
    }

  private def defineLiteServerStop(scoped: Scoped) =
    liteServerStop in scoped := {
      liteServerCmd.value.destroy(scoped,streams.value.log)
    }

  private def defineLiteServerWriteIndexFile(scoped: Scoped) =
    liteServerWriteIndexFile in scoped := {
      val src = (liteServerIndexFile in scoped).value
      val basedir = (liteServerBaseDir in scoped).value
      val dest = basedir / src.getName
//      val dest = utils.fileWithScalaJSStageSuffix((liteServerBaseDir in scoped).value,"index-",scoped,".html")
      IO.copy(Seq((src,dest)),overwrite = true)
      dest
    }

//  private def defineLiteServer(stageTask: TaskKey[Attributed[File]]) =
//    liteServer in stageTask := {
//      npmInstall.value
//      (liteServerWriteConfigFile in stageTask).value
//      (stageTask in Compile).value
//
//      val cmd = liteServerCmd.value
//      val configFile = (liteServerConfigFile in stageTask).value
//      val cwd = npmTargetDir.value
//      cmd.run("-c",configFile.getAbsolutePath)(cwd,streams.value.log)
//      cmd.start("-c",configFile.getAbsolutePath)(streams.value.log,waitAndKillOnInput = true)
//
//
//    }

  private def writeConfigFile(file: File, baseDir: String, indexFile: String, routes: Iterable[(String,String)]) = {
    import JsonNode._
    val config = Obj(
      'server -> Obj(
        'baseDir -> baseDir,
        'index -> indexFile,
        'routes -> Obj(routes)
      )
    )
    IO.write(file,config.toJson)
  }

}
