name := """sbt-alldocs"""
ThisBuild / organization := "glngn"
ThisBuild / description := "Collect all docs into docs"
ThisBuild / licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

sbtPlugin := true

// cannot use 2.0 due to https://github.com/glngn/sbt-alldocs/issues/1
addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.3")

// ScalaTest
libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.1" % "test"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

scalacOptions in Test ++= Seq("-Yrangepos")

bintrayPackageLabels := Seq("sbt","plugin")
bintrayVcsUrl := Some("""git@github.com:glngn/sbt-alldocs.git""")
bintrayOrganization := Some("glngn")
bintrayRepository := "sbt-plugins"
publishMavenStyle := false

initialCommands in console := """import sbtalldocs._"""

enablePlugins(ScriptedPlugin)
enablePlugins(GitVersioning)

git.baseVersion := "0.1.5"

// set up 'scripted; sbt plugin for testing sbt plugins
scriptedLaunchOpts ++=
  Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
