//     Project: sbt-node
//      Module:
// Description:
package de.surfice.sbtnpm.utils.test

import com.typesafe.config.ConfigFactory
import utest._

object RichConfigTests extends TestSuite {

  import de.surfice.sbtnpm.utils._

  val tests = TestSuite {
    val config = ConfigFactory.parseString(
      """npm {
        |  dependencies {
        |    core-js = "^2.4.1"
        |    "@angular/http" = "^4.0.0"
        |    "zone.js" = "0.7.4"
        |  }
        |}
        |systemjs {
        |  meta {
        |    "*.html" {
        |      loader = "text_loader"
        |    }
        |  }
        |}
      """.stripMargin)
    'getStringMap-{
//      config.getStringMap("npm.dependencies")
      assert( config.getStringMap("npm.dependencies") == Map(
        "core-js" -> "^2.4.1",
        "@angular/http" -> "^4.0.0",
        "zone.js" -> "0.7.4"
      ))
    }
    'getConfigMap-{
      val meta = config.getConfigMap("systemjs.meta")
      assert( meta("*.html").getString("loader") == "text_loader" )
    }
  }
}
