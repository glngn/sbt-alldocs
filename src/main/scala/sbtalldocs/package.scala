import sbt._

package object sbtalldocs {
  sealed trait DocArtifact {
    val config: String
    val name: String
    val src: File
    def copyToIndex(indexDir: File): String
  }

  case class DepDoc private (config: String,
                             groupId: String,
                             name: String,
                             version: String,
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
      DepDoc.UID(config, groupId, version, name)
  }

  object DepDoc {
    def apply(config: ConfigRef, moduleID: ModuleID, artifact: Artifact, src: File): DepDoc =
      DepDoc(config.name, moduleID.organization, moduleID.name, moduleID.revision, src)

    case class UID(config: String, groupId: String, version: String, name: String)
  }

  case class ProjectDoc(name: String, src: File) extends DocArtifact {
    final val config: String = "compile"

    def copyToIndex(indexDir: File): String = {
      val targetDir = indexDir / name
      IO.copyDirectory(src,
                       targetDir,
                       overwrite = true,
                       preserveLastModified = true)
      name
    }
  }
  type ViewConfig = String

  type SectionMap = (String, (Int, Section))

  type SectionSelector = String => (Int, String)

  type Section = String

  type Index = Map[(ViewConfig, Int, Section), Vector[DocArtifact]]

  type IndexSections = (Section, Vector[DocArtifact])
}
