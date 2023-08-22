name := "sections"
version := "0.1.1"
scalaVersion := "2.13.6"

libraryDependencies += "org.scala-lang.modules" %% "scala-parallel-collections" % "0.2.0"
libraryDependencies += "org.typelevel" %% "cats-core" % "2.9.0"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.16" % Test

logLevel := Level.Debug

allDocsSections := Seq(
  "cats" -> (0, "Bar"),
  "scalatest" -> (0, "Foo"),
)

TaskKey[Unit]("check", "check") := {
  val logger = streams.value.log

  val indexPath = allDocsTargetDir.value + "/index.html"
  val indexSource = scala.io.Source.fromFile(indexPath, "UTF-8")

  var state = 0
  val foundCompileConfig = ".*<h2>.*compile.*</h2>.*".r
  val foundTestConfig = ".*<h2>.*test.*</h2>.*".r
  val foundCatsEntry = ".*cats-core.*".r
  val foundScalatestEntry = ".*scalatest-core.*".r
  indexSource.getLines() foreach { line =>
    logger.info(s"$state line $line")

    (state, line) match {
      case (0, foundCompileConfig(_*)) => {
        logger.info("found Compile config section")
        state = 1
      }
      case (1, foundCatsEntry(_*)) => {
        logger.info("found cats entry")
        state = 2
      }
      case (2, foundTestConfig(_*)) => {
        logger.info("found following Test config section")
        state = 3
      }
      case (3, foundScalatestEntry(_*)) => {
        logger.info("found Test / scalatest entry")
        state = 4
      }
      case _ => ()
    }
  }

  if (state != 4) sys.error("failed") else ()
}
