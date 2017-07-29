//     Project: sbt-node
//      Module:
// Description:
package de.surfice.sbtnpm.liteserver

import sbt._
import Keys._
import de.surfice.sbtnpm.assets.AssetsPlugin
import de.surfice.sbtnpm.{NpmPlugin, utils}
import de.surfice.sbtnpm.utils.{FileWithLastrun, JsonNode, NodeCommand}
import org.scalajs.sbtplugin.{ScalaJSPluginInternal, Stage}

object LiteServerPlugin extends AutoPlugin {


  override lazy val requires = NpmPlugin && AssetsPlugin


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
      settingKey("Path to the index.html file (scoped to fastOptJS or fullOptJS)")

    val liteServerRoutes: SettingKey[Iterable[(String,String)]] =
      settingKey("Entries to be put in the lite-server config 'routes' object (scoped to fastOptJS or fullOptJS)")

    val liteServerCmd: SettingKey[NodeCommand] =
      settingKey("command to run the node lite-server")

    val liteServerWriteConfigFile: TaskKey[FileWithLastrun] =
      taskKey("Create the lite-server config file for the current stage (fastOptJS or fullOptJS)")

    val liteServerPrepare: TaskKey[Unit] =
      taskKey[Unit]("Prepare config, compile, and run asset pipeline (scoped to fastOptJS and fullOptJS)")

    val liteServerStart: TaskKey[Unit] =
      taskKey("Start the node lite-server for the current stage (fastOptJS or fullOptJS")

    val liteServerStop: TaskKey[Unit] =
      taskKey("Stops the node lite-server for the current stage (fastOptJS or fullOptJS")


    val liteServerRun: TaskKey[Unit] =
      taskKey("Compiles the project and runs the node lite-server for the current stage (fastOptJS or fullOptJS")
  }

  import autoImport._
  import NpmPlugin.autoImport._
  import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
  import AssetsPlugin.autoImport._

  override lazy val projectSettings: Seq[Def.Setting[_]] = Seq(
    liteServerVersion := de.surfice.sbtnpm.Versions.liteServer,

    liteServerCmd := NodeCommand(npmNodeModulesDir.value,"lite-server","lite-server"),

    npmDevDependencies += "lite-server" -> liteServerVersion.value,

    (liteServerRun in fastOptJS) := {
      val _ = (liteServerPrepare in fastOptJS).value

      val cmd = npmCmd.value
      cmd.start("run-script","fastOptJS")(streams.value.log,waitAndKillOnInput = true)
    },
    (liteServerRun in fullOptJS) := {
      val _ = (liteServerPrepare in fullOptJS).value

      val cmd = npmCmd.value
      cmd.start("run-script","fullOptJS")(streams.value.log,waitAndKillOnInput = true)
    }
  ) ++
    perScalaJSStageSettings(Stage.FullOpt) ++
    perScalaJSStageSettings(Stage.FastOpt) ++
  Seq(
    liteServerPrepare in fastOptJS := (liteServerPrepare in fastOptJS)
      .dependsOn(npmInstall,fastOptJS in Compile,nodeAssetsStage in fastOptJS,liteServerWriteConfigFile in fastOptJS).value,
    liteServerPrepare in fullOptJS := (liteServerPrepare in fullOptJS)
      .dependsOn(npmInstall,fullOptJS in Compile,nodeAssetsStage in fullOptJS, liteServerWriteConfigFile in fullOptJS).value
  )


  private def perScalaJSStageSettings(stage: Stage): Seq[Def.Setting[_]] = {

    val stageTask = ScalaJSPluginInternal.stageKeys(stage)

    Seq(
      defineLiteServerConfigFile(stageTask),
      defineLiteServerBaseDir(stageTask),
      defineLiteServerIndexFile(stageTask),
      defineLiteServerRoutes(stageTask),
      defineLiteServerWriteConfigFile(stageTask),
      defineLiteServerPrepare(stageTask),
      defineLiteServerStart(stageTask),
      defineLiteServerStop(stageTask),
      addNpmScript(stageTask)
    )
  }

  private def defineLiteServerConfigFile(scoped: Scoped) =
    liteServerConfigFile in scoped := utils.fileWithScalaJSStageSuffix( (crossTarget in Compile).value,"bs-config-",scoped,".json")

  private def defineLiteServerBaseDir(scoped: Scoped) =
    liteServerBaseDir in scoped := (crossTarget in (NodeAssets,scoped)).value

  private def defineLiteServerIndexFile(scoped: Scoped) =
    liteServerIndexFile in scoped := {
      val baseDir = (liteServerBaseDir in scoped).value
      utils.fileWithScalaJSStageSuffix(baseDir,"index-",scoped,".html")
    }

  private def defineLiteServerRoutes(scoped: Scoped) =
    liteServerRoutes in scoped := Seq("/node_modules/" -> npmNodeModulesDir.value.getAbsolutePath)

  private def defineLiteServerPrepare(scoped: Scoped) =
    liteServerPrepare in scoped := {
      val log = streams.value.log
      val stageName = scoped.key.label
      log.info(s"preparing lite-server environment for $stageName")

      val srcIndexFile = (liteServerIndexFile in scoped).value
      val tgtDir = (crossTarget in (NodeAssets,scoped)).value
      val tgtIndexFile = tgtDir / "index.html"

      if(!tgtIndexFile.exists()) {
        if (!srcIndexFile.exists())
          log.warn(s"File $srcIndexFile defined by (liteServerIndexFile in $stageName) does not exist - lite-server configuration will probably fail")
        else
          IO.copyFile(srcIndexFile, tgtIndexFile)
      }
    }

  private def defineLiteServerWriteConfigFile(scoped: Scoped) =
    liteServerWriteConfigFile in scoped := {
      val lastrun = (liteServerWriteConfigFile in scoped).previous
      val file = (liteServerConfigFile in scoped).value
      val baseDir = (liteServerBaseDir in scoped).value

      if(lastrun.isEmpty || lastrun.get.needsUpdateComparedToConfig(baseDirectory.value)) {
        writeConfigFile(file,
          baseDir.getAbsolutePath,
          "./index.html",
          (liteServerRoutes in scoped).value)
        FileWithLastrun(file)
      }
      else lastrun.get
    }

  private def defineLiteServerStart(scoped: Scoped) =
    liteServerStart in scoped := {
      npmInstall.value
      val cmd = liteServerCmd.value
      val configFile = (liteServerConfigFile in scoped).value

      cmd.startAndStore("-c",configFile.getAbsolutePath)(scoped,streams.value.log)
    }

  private def defineLiteServerStop(scoped: Scoped) =
    liteServerStop in scoped := {
      liteServerCmd.value.destroy(scoped,streams.value.log)
    }


  private def addNpmScript(scoped: Scoped) =
    npmScripts += {
      val lsConfigFile = (liteServerConfigFile in scoped).value
      (scoped.key.label, "lite-server --config="+lsConfigFile.getAbsolutePath)
    }



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
