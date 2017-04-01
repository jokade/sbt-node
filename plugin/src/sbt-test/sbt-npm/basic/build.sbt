name := "basic"

version := "0.1.0-SNAPSHOT"

enablePlugins(NpmPlugin)

TaskKey[Unit]("check") := {
  val logger = streams.value.log
  val json = IO.read(npmPackageJsonFile.value)
  logger.info(s"package.json:\n$json")
  assert(json ==
    """{
      |  'name': 'basic',
      |  'version': '0.1.0-SNAPSHOT',
      |  'dependencies': {},
      |  'devDependencies': {}
      |}""".stripMargin)
}
