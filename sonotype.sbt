sonatypeProfileName := "com.glngn"
publishMavenStyle := true

// or if you want to set these fields manually
homepage := Some(url("https://github.com/glngn/sbt-alldocs/"))

scmInfo := Some(
  ScmInfo(
    url("https://github.com/glngn/sbt-alldocs"),
    "scm:git@github.com:glngn/sbt-alldocs.git"
  )
)

developers := List(
  Developer(id="coreyoconnor",
            name="Corey O'Connor",
            email="coreyoconnor@dogheadbone.com",
            url=url("https://dogheadbone.com/")
  )
)

ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"

publishTo := sonatypePublishToBundle.value
