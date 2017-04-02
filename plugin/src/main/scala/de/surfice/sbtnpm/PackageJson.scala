//     Project: SBT NPM
//      Module:
// Description:
package de.surfice.sbtnpm

import de.surfice.sbtnpm.utils.{JsonFile, JsonNode}
import PackageJson._

case class PackageJson(path: sbt.File,
                       name: String,
                       version: String = "0.0.1",
                       description: String = "",
                       dependencies: Dependencies = Nil,
                       devDependencies: Dependencies = Nil
                      ) extends JsonFile {
  override def json: JsonNode = {
    import JsonNode._
    Obj(
      'name -> name,
      'version -> version,
      'description -> description,
      'dependencies -> Obj(dependencies),
      'devDependencies -> Obj(devDependencies)
    )
  }
}

object PackageJson {
  type Dependencies = Iterable[(String,String)]
}
