//     Project: sbt-node
//      Module:
// Description:
package de.surfice.sbtnpm.systemjs

import sbt._
import Keys._
import Cache._
import de.surfice.sbtnpm.assets.AssetsPlugin
import de.surfice.sbtnpm.liteserver.LiteServerPlugin
import de.surfice.sbtnpm.{NpmPlugin, utils}
import de.surfice.sbtnpm.utils.FileWithLastrun
import org.scalajs.sbtplugin.{ScalaJSPluginInternal, Stage}

object SystemJSPlugin extends AutoPlugin {


  override lazy val requires = NpmPlugin && AssetsPlugin && LiteServerPlugin

  /**
   * @groupname tasks Tasks
   * @groupname settings Settings
   */
  object autoImport {
    val systemJSFile: SettingKey[File] =
      settingKey("Path to the systemjs.config.js file (scoped to fastOptJS or fullOptJS)")

    val systemJSPaths: SettingKey[Seq[(String,String)]] =
      settingKey("Entries to be put into the System.js config 'paths' object")

    val systemJSMappings: SettingKey[Seq[(String,String)]] =
      settingKey("Entries to be put into the System.js config 'map' object (the key 'app' is assigned automatically!)")

    val systemJSPackages: SettingKey[Seq[(String,SystemJSPackage)]] =
      settingKey("System.js package definitions (the package 'app' is defined automatically!)")

    val systemJS: TaskKey[FileWithLastrun] =
      taskKey("Writes the System.js config file for the current stage (fastOptJS, fullOptJS)")
  }

  import autoImport._
  import AssetsPlugin.autoImport._
  import LiteServerPlugin.autoImport._
  import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

  override lazy val projectSettings: Seq[Def.Setting[_]] = Seq(
  ) ++
    perScalaJSStageSettings(Stage.FullOpt) ++
    perScalaJSStageSettings(Stage.FastOpt) ++
  Seq(

    (fastOptJS in Compile) := (fastOptJS in Compile).dependsOn(systemJS in fastOptJS).value,
    (fullOptJS in Compile) := (fullOptJS in Compile).dependsOn(systemJS in fullOptJS).value
//    liteServerPrepare in fastOptJS := (liteServerPrepare in fastOptJS).dependsOn(systemJS in fastOptJS).value,
//    liteServerPrepare in fullOptJS := (liteServerPrepare in fullOptJS).dependsOn(systemJS in fullOptJS).value
  )


  private def perScalaJSStageSettings(stage: Stage): Seq[Def.Setting[_]] = {

    val stageTask = ScalaJSPluginInternal.stageKeys(stage)

    Seq(
      defineSystemJSFile(stageTask),
      defineSystemJSPaths(stageTask),
      defineSystemJSMappings(stageTask),
      defineSystemJSPackages(stageTask),
      defineSystemJSTask(stageTask)
    )
  }

  private def defineSystemJSFile(scope: Any) = scope match {
    case scoped: Scoped =>
      systemJSFile in scoped := utils.fileWithScalaJSStageSuffix((crossTarget in (NodeAssets,scoped)).value,"systemjs-",scoped,".config.js")
  }

  private def defineSystemJSPaths(scoped: Scoped) =
    systemJSPaths in scoped := Seq(
      "npm:" -> "node_modules/"
    )

  private def defineSystemJSMappings(scoped: Scoped) =
    systemJSMappings in scoped := Seq( "app" -> "./" )
//      systemJSMappings in scoped := Seq( "app" -> (crossTarget in (Compile,scoped)).value.relativeTo(baseDirectory.value).get.getPath )

  private def defineSystemJSPackages(scoped: Scoped) =
      systemJSPackages in scoped := Seq( "app" -> SystemJSPackage(
        main = Some("./"+(artifactPath in (Compile,scoped)).value.getName),
        format = Some("cjs"),
        defaultExtension = Some("js")
      ))

  private def defineSystemJSTask(scoped: Scoped) =
    systemJS in scoped := {
      val lastrun = (systemJS in scoped).previous
      val file = (systemJSFile in scoped).value

      if(lastrun.isEmpty || lastrun.get.needsUpdateComparedToConfig(baseDirectory.value)) {
        streams.value.log.info(s"Writing System.js configuration for ${scoped.key.label}")
        writeSystemJSFile(
          file = file,
          paths = (systemJSPaths in scoped).value,
          mappings = (systemJSMappings in scoped).value,
          packages = (systemJSPackages in scoped).value)
        FileWithLastrun(file)
      }
      else
        lastrun.get
    }

  private def writeSystemJSFile(file: File,
                                paths: Iterable[(String,String)],
                                mappings: Iterable[(String,String)],
                                packages: Iterable[(String,SystemJSPackage)]): Unit = {
    val js =
      s"""(function (global) {
         |  System.config({
         |    paths: {
         |${paths.map(kv => "      '"+kv._1+"': '"+kv._2+"'").mkString(",\n")}
         |    },
         |    map: {
         |${mappings.map(kv => "      '"+kv._1+"': '"+kv._2+"'").mkString(",\n")}
         |    },
         |    packages: {
         |${packages.map(kv => "      '"+kv._1+"': "+kv._2.toJS("        ")).mkString(",\n")}
         |    }
         |  });
         |})(this);
       """.stripMargin
    IO.write(file,js)
  }
}
