ThisBuild / organization := "com.glngn"
ThisBuild / description := "Collect all docs into docs"
ThisBuild / licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))
ThisBuild / crossSbtVersions := Vector("1.3.13", "1.4.9")

lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    nocomma {
      name := """sbt-alldocs"""

      // ScalaTest
      libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.8" % "test"
      libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % "test"

      Test / scalacOptions ++= Seq("-Yrangepos")

      console / initialCommands := """import sbtalldocs._"""

      // set up 'scripted; sbt plugin for testing sbt plugins
      scriptedLaunchOpts ++= Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    }
  )
