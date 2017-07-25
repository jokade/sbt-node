version in ThisBuild := "0.0.2"

scalaVersion in ThisBuild := "2.10.6"

organization in ThisBuild := "de.surfice"

// default versions of npm packages required by plugins
val Versions = new {
  val liteServer   = "^2.3.0"
  val webpack      = "^3.3.0"
  val css_loader   = "^0.28.4"
  val html_loader  = "^0.4.5"
  val style_loader = "^0.18.2"
}


lazy val sharedSettings = Seq(
  scalacOptions ++= Seq("-deprecation","-unchecked","-feature","-Xlint"),
  libraryDependencies ++= Seq(
    "com.lihaoyi" % "utest_2.10" % "0.4.5"
  ),
  testFrameworks += new TestFramework("utest.runner.Framework")
//  scalacOptions ++= (if (isSnapshot.value) Seq.empty else Seq({
//        val a = baseDirectory.value.toURI.toString.replaceFirst("[^/]+/?$", "")
//        val g = "https://raw.githubusercontent.com/jokade/sbt-node"
//        s"-P:scalajs:mapSourceURI:$a->$g/v${version.value}/"
//      }))
)

lazy val plugin = project
  .settings(sharedSettings ++ scriptedSettings ++ publishingSettings: _*)
  .settings(
    name := "sbt-node",
    sbtPlugin := true,
    addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion),
    sourceGenerators in Compile += Def.task {
      val file = (sourceManaged in Compile).value / "Version.scala"
      IO.write(file,
        s"""package de.surfice.sbtnpm
        |object Versions { 
        |  val liteServer = "${Versions.liteServer}"
        |  val webpack = "${Versions.webpack}"
        |  val style_loader = "${Versions.style_loader}"
        |  val css_loader = "${Versions.css_loader}"
        |  val html_loader = "${Versions.html_loader}"
        |}
        """.stripMargin)
      Seq(file)
    }.taskValue
  )

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
