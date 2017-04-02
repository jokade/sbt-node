name := "basic"

version := "0.1.0-SNAPSHOT"

description := "Basic sbt-npm test"

enablePlugins(NpmPlugin)

npmDependencies ++= Seq(
  "rxjs" -> "5.0.1"
)

TaskKey[Unit]("check") := {
  val logger = streams.value.log
  val json = IO.read(npmPackageJsonFile.value)
  logger.info(s"package.json:\n$json")
  assert(json ==
    """{
      |  "name": "basic",
      |  "version": "0.1.0-SNAPSHOT",
      |  "description": "Basic sbt-npm test",
      |  "dependencies": {
           "rxjs": "5.0.1"
      |  },
      |  "devDependencies": {}
      |}""".stripMargin)
}
