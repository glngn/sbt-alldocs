import sbt._

package object sbtalldocs {
  sealed trait DocArtifact {
    val name: String
    val src: File
    def copyToIndex(indexDir: File): String
  }

  case class DepDoc private (groupId: String,
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
      DepDoc.UID(groupId, version, name)
  }

  object DepDoc {
    def apply(moduleID: ModuleID, artifact: Artifact, src: File): DepDoc =
      DepDoc(moduleID.organization, moduleID.name, moduleID.revision, src)

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
  type SectionMap = (String, (Int, String))

  type SectionSelector = String => (Int, String)

  type Index = Map[Int, Map[String, List[DocArtifact]]]

  type IndexSection = (String, List[DocArtifact])
}
