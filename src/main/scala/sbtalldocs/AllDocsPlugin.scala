package sbtalldocs

import sbt._
import scala.xml
import scala.util.Try
import scala.collection.mutable

import Keys._

object AllDocs {
  import AllDocsPlugin.autoImport._

  def sectionSelectorForDef(sectionsDef: Seq[SectionMap]): SectionSelector = {
    // anchor only to start
    val regexMap = sectionsDef.map {
      case (r, (p, n)) => (p, ("^" + r).r, n)
    }

    val matchSeq = regexMap.sortBy(_._1)

    { name =>
      matchSeq find { case (_, r, _) =>
        r.findFirstIn(name).nonEmpty
      } map { case (p, _, n) => (p, n)
      } getOrElse (999, "Other")
    }
  }

  def indexViewConfigs(index: Index): List[ViewConfig] = index.keys.map(_._1).toList

  def indexSections(index: Index, viewConfig: ViewConfig): Vector[(Section, Vector[DocArtifact])] =
    index.toVector collect {
      case ((config, priority, section), artifacts) if config == viewConfig => (priority, section, artifacts)
    } sortBy(_._1) map { case (_, section, artifacts) =>
      (section, artifacts.sortBy(_.name))
    }

  def allDocsIndexSource(logger: Logger,
                         docsDir: File,
                         exclusions: Set[String],
                         renames: Map[String, String],
                         index: Index): xml.Node = {

    val viewConfigsHTML = {
      val viewConfigs = indexViewConfigs(index)

      viewConfigs map { viewConfig =>
        val sections = indexSections(index, viewConfig)
        logger.info(s"sections = $sections")

        val sectionsHTML = sections.map { case (sectionName, docArtifacts) =>
          <h3>{ sectionName }</h3>
          <ul>
            { docArtifactsIndex(logger, docsDir, exclusions, renames, docArtifacts) }
          </ul>
        }

        <h2>{ viewConfig }</h2>
        <div class="section">
          { sectionsHTML }
        </div>
      }
    }


    <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
      <head>
        <meta charset="utf-8"/>
        <title>Doc Index</title>
      </head>
      <body>
        { viewConfigsHTML }
      </body>
    </html>
  }

  def docArtifactsIndex(logger: Logger,
                        indexDir: File,
                        exclusions: Set[String],
                        renames: Map[String, String],
                        docs: Vector[DocArtifact]): Seq[xml.Node] = {
    val sortedArtifacts = docs.toSeq.sortBy(_.name)
    val withoutExclusions = sortedArtifacts.filter(exclusions contains _.name unary_!)

    withoutExclusions map { doc =>
      logger.info(s"doc = ${doc}")
      val name: String = doc.name.stripSuffix("_2.12").stripSuffix("_2.13").stripSuffix("_3")

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
          val ext = Project.extract(state0)
          import ext.structure
          val projectName = (projectRef / name) get structure.data getOrElse projectRef.project

          (projectRef / organization) get structure.data match {
            case None => (state1, Seq(ProjectDoc(projectName, dir)))
            case Some(projectOrg) => (state1, Seq(ProjectDoc(s"$projectOrg.$projectName", dir)))
          }
        }
      }
    }
  }

  def determineDepDocs(projectRef: ProjectRef,
                       state0: State,
                       alreadyIncluded: Set[DepDoc.UID]): (State, Seq[DepDoc]) = {
    val logger = state0.log
    val included = mutable.Set(alreadyIncluded.toSeq: _*)

    Project.runTask(projectRef / allDepDocArtifacts, state0) match {
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
            (config, moduleID, artifact, file) <- artifacts.toSeq
            depDoc = DepDoc(config, moduleID, artifact, file) if (!included(depDoc.uid))
          } yield {
            included += depDoc.uid
            depDoc
          }

          (state1, depDocs)
        }
      }
    }
  }

  def command(state0: State): State = {
    val logger = state0.log
    val ext = Project.extract(state0)
    import ext.structure

    val index0: Index = Map.empty
    val alreadyIncluded0 = Set.empty[DepDoc.UID]

    val exclusions = Global / allDocsExclusions get structure.data get
    val renames = Global / allDocsRenames get structure.data get
    val sectionsDef = Global / allDocsSections get structure.data get
    val docsDir = IO.toFile(structure.root) / (Global / allDocsTargetDir get structure.data get)

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
          val config = docArtifact.config
          val (priority, section) = sectionSelector(docArtifact.name)

          indexAcc.get((config, priority, section)) match {
            case None => indexAcc.updated((config, priority, section), Vector(docArtifact))
            case Some(artifacts) => indexAcc.updated((config, priority, section), docArtifact +: artifacts)
          }
        }

        val alreadyIncludedN = alreadyIncludedM ++ thisDepDocs.map(_.uid).toSet
        (stateO, indexN, alreadyIncludedN)
      }
    }

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

object AllDocsPlugin extends AutoPlugin {
  import autoImport._
  import Keys._

  object autoImport {
    val allDepDocArtifacts = taskKey[Vector[(ConfigRef, ModuleID, Artifact, File)]]("all dependency documentation artifacts").withRank(KeyRanks.Invisible)
    val allDocsConfigs = settingKey[Set[ConfigRef]](
      "Configs to include in the index. Each config contains all the allDocsSections sections with at least one package for that config."
    )

    val allDocsConfigRemaps = settingKey[Map[ConfigRef, ConfigRef]](
      "remap from one to config to another prior to placing in a top level section"
    )

    val allDocsExclusions = settingKey[Set[String]](
      "exact name, pre rename, of artifact documentation to exclude from index"
    )

    val allDocsRenames = settingKey[Map[String, String]](
      "mapping of input name to output name. Applied once per artifact."
    )

    val allDocsSections = settingKey[Seq[SectionMap]](
      "names matching regex are placed under named section with sort priority"
    )

    val allDocsTargetDir = settingKey[String](
      "Directory relative to root the documentation should be placed"
    )
  }

  override def projectSettings = Seq(
    allDepDocArtifacts / updateOptions := updateOptions.value.withCachedResolution(false),
    allDepDocArtifacts := {
      val logger = streams.value.log
      val updateReport = updateClassifiers.value

      val configSet = allDocsConfigs.value
      val includeConfig: ConfigRef => Option[ConfigRef] = { configRef =>
        val viewConfig = allDocsConfigRemaps.value.get(configRef) getOrElse configRef
        if (configSet.contains(viewConfig)) Some(viewConfig) else None
      }

      for {
        config <- updateReport.configurations
        viewConfig <- includeConfig(config.configuration).toSeq
        module <- config.modules
        (artifact, file) <- module.artifacts if artifact.classifier == Some("javadoc")
      } yield {
        logger.debug(s"viewConfig = ${viewConfig}, file = ${file}, moduleId=${module.module}")

        (viewConfig, module.module, artifact, file)
      }
    },
    allDocsConfigs := Set(ConfigRef("compile"), ConfigRef("test")),
    allDocsConfigRemaps := Map.empty[ConfigRef, ConfigRef],
    allDocsExclusions := Set.empty[String],
    allDocsRenames := Map.empty[String, String],
    allDocsSections := Seq.empty[SectionMap]
  )

  // too broad
  private val scopeToAggregrate = ScopeFilter(inAnyProject, inAnyConfiguration)

  override def globalSettings = Seq(
    commands += allDocsCmd,
    allDocsConfigs := {
      (allDocsConfigs ?? Set.empty[ConfigRef]).all(scopeToAggregrate).value.toSet.flatten
    },
    allDocsConfigRemaps := {
      (allDocsConfigRemaps ?? Map.empty[ConfigRef, ConfigRef]).all(scopeToAggregrate).value.map(_.toList).flatten.toMap
    },
    allDocsExclusions := {
      (allDocsExclusions ?? Set.empty[String]).all(scopeToAggregrate).value.toSet.flatten
    },
    allDocsRenames := {
      (allDocsRenames ?? Map.empty[String, String]).all(scopeToAggregrate)
        .value
        .foldLeft(Map.empty[String, String])( (acc, renames) => acc ++ renames )
    },
    allDocsSections := {
      (allDocsSections ?? Seq.empty[SectionMap]).all(scopeToAggregrate).value.flatten
    },
    allDocsTargetDir := "alldocs"
  )

  override def trigger: PluginTrigger = allRequirements

  lazy val allDocsCmd = Command.command("allDocs")(AllDocs.command)
}
