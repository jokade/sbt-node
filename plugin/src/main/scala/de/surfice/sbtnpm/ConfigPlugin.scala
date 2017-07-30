//     Project: sbt-node
//      Module:
// Description:
package de.surfice.sbtnpm

import java.io.{File => _, _}
import java.net.JarURLConnection

import sbt.{Def, _}
import Keys._
import com.typesafe.config.{Config, ConfigFactory}
import org.scalajs.sbtplugin.ScalaJSPlugin
import sbt.impl.DependencyBuilders

object ConfigPlugin extends AutoPlugin {

  override val requires: Plugins = ScalaJSPlugin

  object autoImport {

    val npmProjectConfig: TaskKey[Config] =
      taskKey[Config]("Configuration loaded from package.conf files in libraries")

    val npmProjectConfigFile: SettingKey[File] =
      settingKey[File]("Project configuration file")
  }

  import autoImport._


  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    libraryDependencies += DepBuilder.toGroupID("de.surfice") %% "sbt-node-config" % Versions.sbtNode,

    npmProjectConfigFile := baseDirectory.value / "project.conf",

    npmProjectConfig := {
      val configString = loadPackageConfigs((dependencyClasspath in Compile).value, npmProjectConfigFile.value)
        .foldLeft("")( (s,in) => s + IO.readLines(new BufferedReader(new InputStreamReader(in))).mkString("\n") + "\n\n" )
      ConfigFactory.parseString(configString).resolve()
    }

  )
  private def loadPackageConfigs(dependencyClasspath: Classpath, projectConfig: File) =
    loadDepPackageConfigs(dependencyClasspath) ++ loadProjectConfig(projectConfig)

  private def loadProjectConfig(projectConfig: File): Option[InputStream] =
    if(projectConfig.canRead)
      Some(fin(projectConfig))
    else None

  private def loadDepPackageConfigs(cp: Classpath): Seq[InputStream] = {
    val (dirs,jars) = cp.files.partition(_.isDirectory)
    loadJarPackageConfigs(jars) // ++ loadDirPackageConfigs(dirs,log)
  }

  private def loadJarPackageConfigs(jars: Seq[File]): Seq[InputStream] = {
    val files = jars
      .map( f => new URL("jar:" + f.toURI + "!/package.conf").openConnection() )
      .map {
        case c: JarURLConnection => try{
          Some(c.getInputStream)
        } catch {
          case _: FileNotFoundException => None
        }
      }
      .collect{
        case Some(in) => in
      }
    files
  }


  private def fin(file: File): BufferedInputStream = new BufferedInputStream(new FileInputStream(file))

  private object DepBuilder extends DependencyBuilders
}
