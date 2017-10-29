//     Project: sbt-node
//      Module:
// Description:
package de.surfice.sbtnpm.systemjs

import sbt._
import Keys._
import com.typesafe.config.Config
import de.surfice.sbtnpm.assets.AssetsPlugin
import de.surfice.sbtnpm.liteserver.LiteServerPlugin
import de.surfice.sbtnpm.{NpmPlugin, Versions, utils}
import de.surfice.sbtnpm.utils.{FileWithLastrun, JsonNode, JsonNodeGenerator}
import org.scalajs.sbtplugin.{ScalaJSPluginInternal, Stage}
import utils._

object SystemJSPlugin extends AutoPlugin {


  override lazy val requires = NpmPlugin && AssetsPlugin && LiteServerPlugin

  /**
   * @groupname tasks Tasks
   * @groupname settings Settings
   */
  object autoImport {
    val systemJSFile: SettingKey[File] =
      settingKey("Path to the systemjs.config.js file (scoped to fastOptJS or fullOptJS)")

    val systemJSConfig: TaskKey[SystemJSConfig] =
      taskKey("SystemJS configuration (scoped to fastOptJS or fullOptJS")

    val systemJS: TaskKey[FileWithLastrun] =
      taskKey("Writes the System.js config file for the current stage (fastOptJS, fullOptJS)")
  }

  import autoImport._
  import AssetsPlugin.autoImport._
  import de.surfice.sbtnpm.ConfigPlugin.autoImport._
  import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

  override lazy val projectSettings: Seq[Def.Setting[_]] = Seq(
  ) ++
    perScalaJSStageSettings(Stage.FullOpt) ++
    perScalaJSStageSettings(Stage.FastOpt) ++
  Seq(

    (fastOptJS in Compile) := (fastOptJS in Compile).dependsOn(systemJS in fastOptJS).value,
    (fullOptJS in Compile) := (fullOptJS in Compile).dependsOn(systemJS in fullOptJS).value
  )


  private def perScalaJSStageSettings(stage: Stage): Seq[Def.Setting[_]] = {

    val stageTask = ScalaJSPluginInternal.stageKeys(stage)

    Seq(
      defineSystemJSFile(stageTask),
      defineSystemJSConfig(stageTask),
      defineSystemJSTask(stageTask)
    )
  }

  private def defineSystemJSFile(scope: Any) = scope match {
    case scoped: Scoped =>
      systemJSFile in scoped := utils.fileWithScalaJSStageSuffix((crossTarget in (NodeAssets,scoped)).value,"systemjs-",scoped,".config.js")
  }

  private def defineSystemJSConfig(scoped: Scoped) =
    systemJSConfig in scoped := {
      val projectConfig = npmProjectConfig.value
      SystemJSConfig(
        paths = Seq("npm:" -> "node_modules/"),
        mappings = projectConfig.getStringMap("systemjs.map").toSeq,
        packages = Seq(
          "$app$" -> SystemJSPackage(
            main = Some("./" + (artifactPath in (Compile,scoped)).value.getName),
            format = Some("cjs"),
            defaultExtension = Some("js")
          )
        ) ++ loadLibraryConfigPackages(projectConfig),
        meta = loadLibraryConfigMeta(projectConfig)
      )
    }

  private def defineSystemJSTask(scoped: Scoped) =
    systemJS in scoped := {
      val lastrun = (systemJS in scoped).previous
      val file = (systemJSFile in scoped).value

      if(lastrun.isEmpty || lastrun.get.needsUpdateComparedToConfig(baseDirectory.value)) {
        streams.value.log.info(s"Writing System.js configuration for ${scoped.key.label}")
        writeSystemJSFile(
          file = file,
          config = (systemJSConfig in scoped).value)
        FileWithLastrun(file)
      }
      else
        lastrun.get
    }

  private def writeSystemJSFile(file: File,
                                config: SystemJSConfig): Unit = {
    import de.surfice.sbtnpm.utils.JsonNode._
    val configNode = Obj(
      'meta -> Obj(config.meta),
      'paths -> Obj(config.paths),
      'map -> Obj(config.mappings),
      'packages -> Obj(config.packages.map(p => p.copy(_2 = p._2.toJsonNode)))
    )

    val js =
      s"""(function (global) {
         |  System.config(
         |${configNode.toJson("  ")}
         |  );
         |})(this);
      """.stripMargin
    IO.write(file,js)
  }

  case class SystemJSConfig(paths: Seq[(String,String)],
                            mappings: Seq[(String,String)],
                            packages: Seq[(String,SystemJSPackage)],
                            meta: Seq[(String,Meta)]) {
    def withPaths(paths: Iterable[(String,String)]): SystemJSConfig = copy(paths = paths.toSeq)
    def addPaths(seq: (String,String)*): SystemJSConfig = copy(paths = paths ++ seq)
    def withMappings(mappings: Iterable[(String,String)]): SystemJSConfig = copy(mappings = mappings.toSeq)
    def addMappings(seq: (String,String)*): SystemJSConfig = copy(mappings = mappings ++ seq)
    def withPackages(packages: Iterable[(String,SystemJSPackage)]): SystemJSConfig = copy(packages = packages.toSeq)
    def addPackages(seq: (String,SystemJSPackage)*): SystemJSConfig = copy(packages = packages ++ seq )
    def withMeta(meta: Iterable[(String,Meta)]): SystemJSConfig = copy(meta = meta.toSeq)
    def addMeta(path: String, loader: Option[String] = None): SystemJSConfig = copy(meta = meta :+ path -> Meta(
      loader = loader
    ))
  }

  case class Meta(loader: Option[String] = None) extends JsonNodeGenerator {
    def toJsonNode: JsonNode = {
      import JsonNode._
      var node = Obj()
      if(loader.isDefined)
        node :+= 'loader -> loader.get
      node
    }
  }
  object Meta {
    def fromConfig(config: Config): Meta = Meta(
      loader = if(config.hasPath("loader")) Some(config.getString("loader")) else None
    )
  }

  case class SystemJSPackage(main: Option[String] = None,
                          format: Option[String] = None,
                          defaultExtension: Option[String] = None) extends JsonNodeGenerator {

    def toJsonNode: JsonNode = {
      import JsonNode._
      var node = Obj()
      if(main.isDefined)
        node :+= 'main -> main.get
      if(format.isDefined)
        node :+= 'format -> format.get
      if(defaultExtension.isDefined)
        node :+= 'defaultExtension -> defaultExtension.get
      node
    }
  }
  object SystemJSPackage {
    def fromConfig(config: Config): SystemJSPackage = SystemJSPackage(
      main = if(config.hasPath("main")) Some(config.getString("main")) else None,
      format = if(config.hasPath("format")) Some(config.getString("format")) else None,
      defaultExtension = if(config.hasPath("defaultExtension")) Some(config.getString("defaultExtension")) else None
    )
  }

  private def loadLibraryConfigMeta(config: Config): Seq[(String,Meta)] = config.getConfigMap("systemjs.meta")
    .map(p => p._1 -> Meta.fromConfig(p._2)).toSeq

  private def loadLibraryConfigPackages(config: Config): Seq[(String,SystemJSPackage)] = config.getConfigMap("systemjs.packages")
    .map(p => p._1 -> SystemJSPackage.fromConfig(p._2)).toSeq
}
