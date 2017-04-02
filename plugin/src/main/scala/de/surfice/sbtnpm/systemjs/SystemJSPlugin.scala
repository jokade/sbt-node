//     Project: sbt-node
//      Module:
// Description:
package de.surfice.sbtnpm.systemjs

import sbt._
import Keys._
import Cache._
import de.surfice.sbtnpm.NpmPlugin
import de.surfice.sbtnpm.utils.FileWithLastrun
import org.scalajs.sbtplugin.{ScalaJSPluginInternal, Stage}

object SystemJSPlugin extends AutoPlugin {

  override lazy val requires = NpmPlugin

  /**
   * @groupname tasks Tasks
   * @groupname settings Settings
   */
  object autoImport {
    val systemJSFile: SettingKey[File] =
      settingKey("Path to the systemjs.config.js file (scoped to fastOptJS or fullOptJS)")

    val systemJSMappings: SettingKey[Iterable[(String,String)]] =
      settingKey("Entries to be put into the System.js 'map' object (the key 'app' is assigned automatically!)")

    val systemJSPackages: SettingKey[Iterable[(String,SystemJSPackage)]] =
      settingKey("System.js package definitions (the package 'app' is defined automatically!)")

    val systemJS: TaskKey[Long] =
      taskKey("Writes the System.js config file for the current stage (fastOptJS, fullOptJS)")
  }

  import autoImport._

  override lazy val projectSettings: Seq[Def.Setting[_]] = Seq(
  ) ++
    perScalaJSStageSettings(Stage.FullOpt) ++
    perScalaJSStageSettings(Stage.FastOpt)


  private def perScalaJSStageSettings(stage: Stage): Seq[Def.Setting[_]] = {

    val stageTask = ScalaJSPluginInternal.stageKeys(stage)

    Seq(
      defineSystemJSFile(stageTask),
      defineSystemJSMappings(stageTask),
      defineSystemJSPackages(stageTask),
      defineSystemJSTask(stageTask)
    )
  }

  private def defineSystemJSFile(scope: Any) = scope match {
    case scoped: Scoped =>
      systemJSFile in scoped := (crossTarget in (Compile,scoped)).value / s"systemjs-${scoped.key.toString.toLowerCase}.config.js"
  }

  private def defineSystemJSMappings(scoped: Scoped) =
      systemJSMappings in scoped := Seq( "app" -> (crossTarget in (Compile,scoped)).value.relativeTo(baseDirectory.value).get.getPath )

  private def defineSystemJSPackages(scoped: Scoped) =
      systemJSPackages in scoped := Seq( "app" -> SystemJSPackage(
        main = Some("./"+(artifactPath in (Compile,scoped)).value.getName),
        format = Some("cjs"),
        defaultExtension = Some("js")
      ))

  private def defineSystemJSTask(scoped: Scoped) =
    systemJS in scoped := {
      val lastrun = (systemJS in scoped).previous
      val file = FileWithLastrun((systemJSFile in scoped).value,lastrun.getOrElse(0))
      if(lastrun.isEmpty || file.needsUpdateComparedToConfig(baseDirectory.value)) {
        writeSystemJSFile(
          file = file.file,
          mappings = (systemJSMappings in scoped).value,
          packages = (systemJSPackages in scoped).value)
        new java.util.Date().getTime
      }
      else
        lastrun.get
    }

  private def writeSystemJSFile(file: File,
                                mappings: Iterable[(String,String)],
                                packages: Iterable[(String,SystemJSPackage)]): Unit = {
    val js =
      s"""(function (global) {
         |  System.config({
         |    paths: {
         |${mappings.map(kv => "      '"+kv._1+"': '"+kv._2+"'").mkString(",\n")}
         |    },
         |    map: {
         |${packages.map(kv => "      '"+kv._1+"': "+kv._2.toJS("        ")).mkString(",\n")}
         |    }
         |  });
         |})(this);
       """.stripMargin
    IO.write(file,js)
  }
}
