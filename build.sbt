enablePlugins(ScriptedPlugin)

ThisBuild / organization := "com.glngn"
ThisBuild / organizationName := "glngn"
ThisBuild / organizationHomepage := Some(url("https://dogheadbone.com"))
ThisBuild / description := "Collect all docs into docs"
ThisBuild / licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

sbtPlugin := true
scalaVersion := "2.12.17"

name := """sbt-alldocs"""

// ScalaTest
// libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.15" % "test"
// libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.15" % "test"

Test / scalacOptions ++= Seq("-Yrangepos")

console / initialCommands := """import sbtalldocs._"""

// set up 'scripted; sbt plugin for testing sbt plugins
scriptedLaunchOpts ++= Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
scriptedBufferLog := false
