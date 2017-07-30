//     Project: sbt-node
//      Module:
// Description:
package de.surfice.sbtnpm.webpack

import de.surfice.sbtnpm.{NpmPlugin, Versions, utils}
import sbt._
import Keys._
import de.surfice.sbtnpm.NpmPlugin.NpmDependency
import de.surfice.sbtnpm.utils.{ExternalCommand, FileWithLastrun, JsonNode, NodeCommand}
import org.scalajs.sbtplugin.{ScalaJSPlugin, ScalaJSPluginInternal, Stage}

object WebpackPlugin extends AutoPlugin {
  type StageTask = TaskKey[Attributed[File]]
  type ResolveAlias = (String,String)

  override val requires = NpmPlugin

  object autoImport {

    val webpackCmd: SettingKey[NodeCommand] =
      settingKey[NodeCommand]("webpack command")

    val webpackConfig: SettingKey[Config] =
      settingKey[Config]("Webpack configuration scoped to fastOptJS or fullOptJS")

    val webpackWriteConfigFile: TaskKey[FileWithLastrun] =
      taskKey("Create the webpack config file for the specified stage (fastOptJS or fullOptJS)")

    val webpackWritePreambleFile: TaskKey[FileWithLastrun] =
      taskKey("Create the preamble file for the specified stage (fastOptJS or fullOptJS)")

    val webpack: TaskKey[File] =
      taskKey("Runs the webpack bundler for the specified stage (fastOptJS or fullOptJS")

    object WebpackRules {
      val css = WebpackRule(
        test = "/\\.css$/",
        use = Seq(
          WebpackLoader(name = "css-loader")
        )
      )
      val html = WebpackRule(
        test = "/\\.(html)$/",
        use = Seq(
          WebpackLoader(name = "html-loader")
        )
      )
    }
  }

  import autoImport._
  import NpmPlugin.autoImport._

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    webpackCmd := NodeCommand(npmNodeModulesDir.value,"webpack","webpack.js")

  ) ++
    perScalaJSStageSettings(Stage.FullOpt) ++
    perScalaJSStageSettings(Stage.FastOpt)


  private def perScalaJSStageSettings(stage: Stage): Seq[Def.Setting[_]] = {
    val stageTask = ScalaJSPluginInternal.stageKeys(stage)

    Seq(
      defineWebpackConfig(stageTask),
      defineWebpackWritePreambleFile(stageTask),
      defineWebpackWriteConfigFile(stageTask),
      defineWebpack(stageTask)
    )
  }


  private def defineWebpackConfig(scoped: StageTask) =
    webpackConfig in scoped := {
      val target = (crossTarget in (Compile,scoped)).value
      val mainEntry = (artifactPath in (Compile,scoped)).value
      val preambleFile = utils.fileWithScalaJSStageSuffix(target, "webpack-preamble-",scoped,".js")

      Config(
        configFile = utils.fileWithScalaJSStageSuffix(target, "webpack-",scoped,".config.js"),
        outputFile = utils.fileWithScalaJSStageSuffix(target, "bundle-",scoped,".js"),
        mainEntry = mainEntry,
        additionalEntryPoints = Nil,
        aliases = Nil,
        preamble = "",
        preambleFile = preambleFile,
        rules = Nil
      )
    }

  private def defineWebpack(scoped: StageTask) =
    webpack in scoped := {
      val cwd = baseDirectory.value
      val logger = streams.value.log
      val config = (webpackConfig in scoped).value
      val configFile = (webpackWriteConfigFile in scoped).value.file
      (webpackWritePreambleFile in scoped).value

      npmInstall.value
      (scoped in (Compile,scoped)).value

      webpackCmd.value.run("--config",configFile.getAbsolutePath)(cwd,logger)
      config.outputFile
    }

  private def defineWebpackWritePreambleFile(scoped: StageTask) =
    webpackWritePreambleFile in scoped := {
      val lastrun = (webpackWritePreambleFile in scoped).previous
      val config = (webpackConfig in scoped).value

      if(lastrun.isEmpty || lastrun.get.needsUpdateComparedToConfig(baseDirectory.value)) {
        IO.write(config.preambleFile,config.preamble)
        FileWithLastrun(config.preambleFile)
      }
      else lastrun.get
    }

  private def defineWebpackWriteConfigFile(scoped: StageTask) =
    webpackWriteConfigFile in scoped := {
      val lastrun = (webpackWriteConfigFile in scoped).previous
      val config = (webpackConfig in scoped).value

      if(lastrun.isEmpty || lastrun.get.needsUpdateComparedToConfig(baseDirectory.value)) {
        writeConfigFile(config)
        FileWithLastrun(config.configFile)
      }
      else lastrun.get
    }

  private def writeConfigFile(config: Config) {
    import config._
    val cnt = "module.exports = " + config.node.toJson + ";"
    IO.write(configFile,cnt)
  }

  import utils.JsonNode._

  case class Config private[webpack] (configFile: File,
                                      outputFile: File,
                                      mainEntry: File,
                                      additionalEntryPoints: Seq[File],
                                      aliases: Seq[ResolveAlias],
                                      preamble: String,
                                      preambleFile: File,
                                      rules: Seq[WebpackRule]) {
    def withOutput(file: File): Config = copy(outputFile = file)
    def withMainEntry(file: File): Config = copy(mainEntry = file)
//    def withEntryPoints(entryPoints: Seq[File]): Config = copy(entryPoints = entryPoints)
    def withAdditionalEntryPoints(entryPoints: File*): Config = copy(additionalEntryPoints = entryPoints)
    def withAliases(aliases: (String,String)*): Config = copy(aliases = aliases)
    def withPreamble(preamble: String): Config = copy(preamble = preamble)
    def withRules(rules: WebpackRule*): Config = copy(rules = rules)
    def withRule(test: String, use: (String,Seq[(String,String)])*): Config =
      copy(rules = rules :+ WebpackRule(test = test, use= use.map(p => WebpackLoader(p._1,p._2))) )

    def entryPoints: Seq[File] = Seq(preambleFile,mainEntry) ++ additionalEntryPoints

    /// Returns all aliases defined explicitly or by on of the config child nodes
    def resolveAliases: Map[String,String] =
      (aliases ++ rules.flatMap(_.aliases)).toMap

    def node = Obj(
      'entry -> Arr(entryPoints.map(f => Str(f.getAbsolutePath))),
      'output -> Obj(
        'filename -> outputFile.getName,
        'path -> outputFile.getParentFile.getAbsolutePath
      ),
      'resolve -> Obj(
        'alias -> Obj(resolveAliases)
      ),
      'module -> Obj(
        'rules -> Arr(rules.map(_.node))
      )
    )
  }

  case class WebpackRule(test: String,
                         use: Seq[WebpackLoader]) {
    def aliases: Seq[ResolveAlias] = use.flatMap(_.aliases)
    def node = Obj(
      'test -> Raw(test),
      'use -> Arr(use.map(_.node))
    )
  }

  case class WebpackLoader(name: String, aliases: Seq[ResolveAlias] = Nil, options: Seq[(String,JsonNode)] = Nil) {
    def node = Obj(
      'loader -> name,
      'options -> Obj(options)
    )
  }

}
