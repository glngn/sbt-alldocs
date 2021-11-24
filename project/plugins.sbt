libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.10")
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.1.2")
addSbtPlugin("com.eed3si9n" % "sbt-nocomma" % "0.1.0")
addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.1.1")
