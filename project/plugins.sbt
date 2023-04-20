libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.18")
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.2.1")
addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.1.1")
