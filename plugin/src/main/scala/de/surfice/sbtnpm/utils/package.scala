//     Project: sbt-node
//      Module:
// Description:
package de.surfice.sbtnpm

import sbt._
import Keys._
import Cache._
import com.typesafe.config.{Config, ConfigFactory, ConfigObject}

package object utils {
  def fileWithScalaJSStageSuffix(dir: File, filePrefix: String, stage: Scoped, fileSuffix: String): File =
    dir / (filePrefix + stage.key.toString.dropRight(2).toLowerCase() + fileSuffix)


  implicit final class RichConfig(val config: Config) extends AnyVal {
    import collection.JavaConverters._
    def getStringMap(path: String): Map[String,String] =  config.withOnlyPath(path).entrySet().asScala
      .map(p => keySuffix(path,p.getKey) -> config.getString(p.getKey))
      .toMap

    def getConfigMap(path: String): Map[String,Config] =
      config.getObject(path).asScala.map(p => p._1 -> p._2).map {
        case (key,obj:ConfigObject) => (key,obj.toConfig)
      }.toMap

    private def stripQuotes(key: String): String = key.replaceAll("\"","")
    private def keySuffix(path: String, key: String): String = stripQuotes(key).stripPrefix(path+".")
  }
}
