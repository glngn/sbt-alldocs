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
  val foundBarSubsection = ".*<h3>.*Bar.*</h3>.*".r
  val foundFooSubsection = ".*<h3>.*Foo.*</h3>.*".r
  val foundCatsEntry = ".*cats-core.*".r
  indexSource.getLines() foreach { line =>
    logger.info(s"$state line $line")

    (state, line) match {
      case (0, foundBarSubsection(_*)) => {
        logger.info("found Bar section")
        state = 1
      }
      case (1, foundCatsEntry(_*)) => {
        logger.info("found cats entry")
        state = 2
      }
      case (2, foundFooSubsection(_*)) => {
        logger.info("found following Foo section")
        state = 3
      }
      case _ => ()
    }
  }

  if (state != 3) sys.error("failed") else ()
}
