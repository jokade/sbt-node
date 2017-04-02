//     Project: sbt-node
//      Module:
// Description:
package de.surfice.sbtnpm.systemjs

case class SystemJSPackage(main: Option[String] = None,
                          format: Option[String] = None,
                          defaultExtension: Option[String] = None) {
  def toJS(prefix: String = ""): String = {
    val entries = Seq(
      "main" -> main,
      "format" -> format,
      "defaultExtension" -> defaultExtension
    ) collect {
      case (key, Some(value: String)) => s"$prefix$key: '$value'"
    }
    entries.mkString("{\n", ",\n", "\n"+prefix + "}")
  }
}

