version in ThisBuild := "0.0.1-SNAPSHOT"

scalaVersion in ThisBuild := "2.10.6"

organization in ThisBuild := "de.surfice"

lazy val Version = new {
  val scalajs = "0.6.15"
}

lazy val sharedSettings = Seq(
  scalacOptions ++= Seq("-deprecation","-unchecked","-feature","-Xlint"),
  libraryDependencies ++= Seq(
    "com.lihaoyi" % "utest_2.10" % "0.4.5"
  ),
  testFrameworks += new TestFramework("utest.runner.Framework"),
  scalacOptions ++= (if (isSnapshot.value) Seq.empty else Seq({
        val a = baseDirectory.value.toURI.toString.replaceFirst("[^/]+/?$", "")
        val g = "https://raw.githubusercontent.com/jokade/sbt-node"
        s"-P:scalajs:mapSourceURI:$a->$g/v${version.value}/"
      }))
)

lazy val plugin = project
  .settings(sharedSettings ++ scriptedSettings ++ publishingSettings: _*)
  .settings(
    name := "sbt-node",
    sbtPlugin := true,
    addSbtPlugin("org.scala-js" % "sbt-scalajs" % Version.scalajs)
  )

//lazy val sassPlugin = project
//  .dependsOn(plugin)
//  .settings(sharedSettings ++ scriptedSettings ++ publishingSettings: _*)
//  .settings(
//    name := "sbt-node-sass",
//    sbtPlugin := true
//  )

lazy val root = project.in(file("."))
  .aggregate(plugin)
  .settings(sharedSettings ++ dontPublish: _*)
  .settings( 
    name := "sbt-node"
  )


lazy val publishingSettings = Seq(
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  pomExtra := (
    <url>https://github.com/jokade/sbt-node</url>
    <licenses>
      <license>
        <name>MIT License</name>
        <url>http://www.opensource.org/licenses/mit-license.php</url>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:jokade/sbt-node</url>
      <connection>scm:git:git@github.com:jokade/sbt-node.git</connection>
    </scm>
    <developers>
      <developer>
        <id>jokade</id>
        <name>Johannes Kastner</name>
        <email>jokade@karchedon.de</email>
      </developer>
    </developers>
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

lazy val scriptedSettings = ScriptedPlugin.scriptedSettings ++ Seq(
  scriptedLaunchOpts += "-Dplugin.version=" + version.value,
  scriptedBufferLog := false
)
