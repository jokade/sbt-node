version in ThisBuild := "0.0.1-SNAPSHOT"

scalaVersion in ThisBuild := "2.11.11"

organization in ThisBuild := "de.surfice"

lazy val sharedSettings = Seq(
  scalacOptions ++= Seq("-deprecation","-unchecked","-feature","-Xlint"),
  libraryDependencies ++= Seq(
  )
)


lazy val root = project.in(file(".")).
  enablePlugins(WebpackPlugin,LiteServerPlugin).
  settings(sharedSettings ++ dontPublish: _*).
  settings( 
    name := "webpack-test",
    libraryDependencies ++= Seq(
    ),
    npmDependencies ++= Seq(
      "lodash" -> "4.16.6"
    )
  )


lazy val dontPublish = Seq(
    publish := {},
    publishLocal := {},
    com.typesafe.sbt.pgp.PgpKeys.publishSigned := {},
    com.typesafe.sbt.pgp.PgpKeys.publishLocalSigned := {},
    publishArtifact := false,
    publishTo := Some(Resolver.file("Unused transient repository",file("target/unusedrepo")))
  )


