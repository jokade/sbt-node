//     Project: SBT NPM
//      Module:
// Description:
package de.surfice.sbtnpm.test

import de.surfice.sbtnpm.PackageJson
import utest._

object PackageJsonTest extends TestSuite {
  val tests = TestSuite {
    val eut = PackageJson(null,"test","0.1.1",
      dependencies = Map(
        "dep1" -> "^0.0.1",
        "dep2" -> "0.2.1"
      ),
      devDependencies = Map(
        "devDep" -> "1.2.3"
      )
    )
    assert(eut.json.toJson ==
      """{
        |  'name': 'test',
        |  'version': '0.1.1',
        |  'dependencies': {
        |    'dep1': '^0.0.1',
        |    'dep2': '0.2.1'
        |  },
        |  'devDependencies': {
        |    'devDep': '1.2.3'
        |  }
        |}""".stripMargin)
  }
}
