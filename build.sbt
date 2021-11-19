ThisBuild / organization := "glngn"
ThisBuild / description := "Collect all docs into docs"
ThisBuild / licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))
ThisBuild / crossSbtVersions := Vector("1.3.13", "1.4.9")

lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin, GitVersioning)
  .settings(
    nocomma {
      name := """sbt-alldocs"""

      // ScalaTest
      libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.8" % "test"
      libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % "test"

      Test / scalacOptions ++= Seq("-Yrangepos")

      bintrayPackageLabels := Seq("sbt","plugin")
      bintrayVcsUrl := Some("""git@github.com:glngn/sbt-alldocs.git""")
      bintrayOrganization := Some("glngn")
      bintrayRepository := "sbt-plugins"
      publishMavenStyle := false

      console / initialCommands := """import sbtalldocs._"""

      git.baseVersion := "0.1.5"

      // set up 'scripted; sbt plugin for testing sbt plugins
      scriptedLaunchOpts ++= Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    }
  )
