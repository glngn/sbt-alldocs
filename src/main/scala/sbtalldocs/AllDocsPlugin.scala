package glngn.sbt

import sbt._
import scala.xml
import scala.util.Try
import scala.collection.mutable

object AllDocsPlugin extends AutoPlugin {
  import autoImport._
  import Keys._

  type SectionMap = (String, (Int, String))

  object autoImport {
    val allDepDocArtifacts = taskKey[Vector[(Artifact, File)]]("all dependency documentation artifacts")
    val allDocsExclusions = settingKey[Set[String]]("exact name, pre rename, of artifact documentation to exclude from index")
    val allDocsRenames = settingKey[Map[String, String]]("mapping of input name to output name. Applied once per artifact.")
    val allDocsSections = settingKey[Seq[SectionMap]]("names matching regex are placed under named section with sort priority")
  }

  override def projectSettings = Seq(
    allDepDocArtifacts := {
      val logger = streams.value.log
      val updateReport = updateClassifiers.value

      updateReport.configurations flatMap (_.modules) flatMap (_.artifacts) filter { case (artifact, file) =>
        val classifier = artifact.classifier
        logger.debug(s"file = ${file}, classifier=${classifier}, extraAttributes=${artifact.extraAttributes}")

        classifier == Some("javadoc")
      }
    },
    allDocsExclusions := Set.empty[String],
    allDocsRenames := Map.empty[String, String],
    allDocsSections := Seq.empty[SectionMap]
  )

  override def globalSettings = Seq(
    commands += allDocsCmd,
    allDocsExclusions := {
      allDocsExclusions.all(ScopeFilter(inAnyProject, inAnyConfiguration)).value.toSet.flatten
    },
    allDocsRenames := {
      allDocsRenames.all(ScopeFilter(inAnyProject, inAnyConfiguration))
        .value
        .foldLeft(Map.empty[String, String])( (acc, renames) => acc ++ renames )
    },
    allDocsSections := {
      allDocsSections.all(ScopeFilter(inAnyProject, inAnyConfiguration)).value.flatten
    }
  )

  override def trigger: PluginTrigger = allRequirements

  sealed trait DocArtifact {
    val name: String
    val src: File
    def copyToIndex(indexDir: File): String
  }

  case class DepDoc private (groupId: String,
                             version: String,
                             name: String,
                             src: File) extends DocArtifact {
    def copyToIndex(indexDir: File): String = {
      val basename = s"${groupId}.${name}"
      val targetDir = indexDir / basename

      val tmpDir = IO.createTemporaryDirectory
      IO.unzip(src, tmpDir)
      IO.copyDirectory(tmpDir,
                       targetDir,
                       overwrite = true,
                       preserveLastModified = true)

      basename
    }

    def uid: DepDoc.UID =
      DepDoc.UID(groupId, version, name)
  }

  object DepDoc {
    def apply(artifact: Artifact, src: File): Option[DepDoc] = for {
      groupId <- artifact.extraAttributes.get("groupId")
      version <- artifact.extraAttributes.get("version")
    } yield DepDoc(groupId, version, artifact.name, src)

    case class UID(groupId: String, version: String, name: String)
  }

  case class ProjectDoc(name: String, src: File) extends DocArtifact {
    def copyToIndex(indexDir: File): String = {
      val targetDir = indexDir / name
      IO.copyDirectory(src,
                       targetDir,
                       overwrite = true,
                       preserveLastModified = true)
      name
    }
  }

  type SectionSelector = String => (Int, String)

  def sectionSelectorForDef(sectionsDef: Seq[SectionMap]): SectionSelector = {
    val regexMap = sectionsDef.map {
      case (r, (p, n)) => (p, ("^" + r + "$").r, n)
    }

    val matchSeq = regexMap.sortBy(_._1)

    { name =>
      matchSeq find { case (_, r, _) =>
        r.findFirstIn(name).nonEmpty
      } map { case (p, _, n) => (p, n)
      } getOrElse (999, "Other")
    }
  }

  type Index = Map[Int, Map[String, List[DocArtifact]]]

  type IndexSection = (String, List[DocArtifact])

  def indexSections(index: Index): List[IndexSection] =
    index.toList.sortBy(_._1).map { case (_, section) =>
      section.toList.sortBy(_._1)
    } flatten

  def allDocsIndexSource(logger: Logger,
                         docsDir: File,
                         exclusions: Set[String],
                         renames: Map[String, String],
                         index: Index): xml.Node = {

    val sectionsHTML = indexSections(index).map { case (sectionName, docArtifacts) =>
      <h2>{ sectionName }</h2>
      <ul>
        { docArtifactsIndex(logger, docsDir, exclusions, renames, docArtifacts) }
      </ul>
    }

    <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
      <head>
        <meta charset="utf-8"/>
        <title>Doc Index</title>
      </head>
      <body>
        { sectionsHTML }
      </body>
    </html>
  }

  def docArtifactsIndex(logger: Logger,
                        indexDir: File,
                        exclusions: Set[String],
                        renames: Map[String, String],
                        docs: List[DocArtifact]): Seq[xml.Node] = {
    val sortedArtifacts = docs.toSeq.sortBy(_.name)
    val withoutExclusions = sortedArtifacts.filter(exclusions contains _.name unary_!)

    withoutExclusions map { doc =>
      logger.info(s"doc = ${doc}")
      val name: String = doc.name

      val subpath: String = {
        val basename = doc.copyToIndex(indexDir)

        s"${basename}/index.html"
      }

      val displayName = renames.get(name) getOrElse name

      <li>
        <a href={ subpath }>{ displayName }</a>
      </li>
    }
  }

  def determineProjectDocs(projectRef: ProjectRef, state0: State): (State, Seq[ProjectDoc]) = {
    val logger = state0.log

    Project.runTask(projectRef/Compile/doc, state0) match {
      case None => {
        logger.info(s"no project doc result for ${projectRef}")

        (state0, Seq.empty)
      }

      case Some((state1, taskResult)) => taskResult.toEither match {
        case Left(incomplete) => {
          logger.info(s"incomplete project doc task = ${incomplete}")

          (state1, Seq.empty)
        }

        case Right(dir) => {
          (state1, Seq(ProjectDoc(projectRef.project, dir)))
        }
      }
    }
  }

  def determineDepDocs(projectRef: ProjectRef,
                       state0: State,
                       alreadyIncluded: Set[DepDoc.UID]): (State, Seq[DepDoc]) = {
    val logger = state0.log
    val included = mutable.Set(alreadyIncluded.toSeq: _*)

    Project.runTask(allDepDocArtifacts in projectRef, state0) match {
      case None => {
        logger.info(s"no result for ${projectRef}")

        (state0, Seq.empty[DepDoc])
      }

      case Some((state1, taskResult)) => taskResult.toEither match {
        case Left(incomplete) => {
          logger.info(s"incomplete = ${incomplete}")

          (state1, Seq.empty[DepDoc])
        }

        case Right(artifacts) => {

          val depDocs = for {
            (artifact, file) <- artifacts.toSeq
            depDoc <- DepDoc(artifact, file).toSeq if (!included(depDoc.uid))
          } yield {
            included += depDoc.uid
            depDoc
          }

          (state1, depDocs)
        }
      }
    }
  }

  lazy val allDocsCmd = Command.command("allDocs") { state0 =>
    val logger = state0.log
    val ext = Project.extract(state0)
    import ext._

    val index0: Index = Map.empty
    val alreadyIncluded0 = Set.empty[DepDoc.UID]

    val exclusions = allDocsExclusions in Global get structure.data get
    val renames = allDocsRenames in Global get structure.data get
    val sectionsDef = allDocsSections in Global get structure.data get

    val sectionSelector = sectionSelectorForDef(sectionsDef)

    val build0 = (state0, index0, alreadyIncluded0)
    val (state, index, _) = structure.allProjectRefs.foldLeft(build0) {
      case (buildM, projectRef) => {
        logger.info(s"project = ${projectRef}")

        val (stateM, indexM, alreadyIncludedM) = buildM

        val (stateN, thisDepDocs) = determineDepDocs(projectRef, stateM, alreadyIncludedM)

        val (stateO, thisProjectDocs) = determineProjectDocs(projectRef, stateN)

        val thisDocs: List[DocArtifact] = (thisDepDocs ++ thisProjectDocs).toList

        // TODO: depend on a library that includes map monoid. a bit heavy to include right now.
        val indexN = thisDocs.foldLeft(indexM) { (indexAcc: Index, docArtifact: DocArtifact) =>
          val (p, n) = sectionSelector(docArtifact.name)
          indexAcc.get(p) match {
            case Some(section) => {
              val docArtifacts = section.get(n) match {
                case Some(artifacts) => docArtifact :: artifacts
                case None => List(docArtifact)
              }
              indexAcc.updated(p, section.updated(n, docArtifacts))
            }
            case None => {
              indexAcc.updated(p, Map(n -> List(docArtifact)))
            }
          }
        }

        val alreadyIncludedN = alreadyIncludedM ++ thisDepDocs.map(_.uid).toSet
        (stateO, indexN, alreadyIncludedN)
      }
    }

    val docsDir = IO.toFile(structure.root) / "docs"
    IO.createDirectory(docsDir)

    val indexData = allDocsIndexSource(logger,
                                       docsDir,
                                       exclusions,
                                       renames,
                                       index)

    val indexPath = docsDir / "index.html"

    IO.write(indexPath, indexData.toString)

    state
  }
}
