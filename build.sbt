name := """sbt-alldocs"""
ThisBuild / organization := "coreyconnor"
ThisBuild / version := "0.1.2"
ThisBuild / description := "Collect all docs into docs"
ThisBuild / licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

sbtPlugin := true

// ScalaTest
libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.1" % "test"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

scalacOptions in Test ++= Seq("-Yrangepos")

bintrayPackageLabels := Seq("sbt","plugin")
bintrayVcsUrl := Some("""git@github.com:glngn/sbt-alldocs.git""")
bintrayOrganization in bintray := None
publishMavenStyle := false

initialCommands in console := """import sbtalldocs._"""

enablePlugins(ScriptedPlugin)
enablePlugins(GitVersioning)

// set up 'scripted; sbt plugin for testing sbt plugins
scriptedLaunchOpts ++=
  Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
