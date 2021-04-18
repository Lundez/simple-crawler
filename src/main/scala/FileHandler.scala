import Main.indexUrl

import java.nio.file.{Files, Path, Paths}

object FileHandler {
  val rootPath: Path = Paths.get(indexUrl.split("/+")(1))

  def saveFile(url: String, response: Array[Byte]): Path = {
    val filename = url.stripPrefix(indexUrl).split('?').head.replace("%20", " ")
    val filenameWithSuffix = if (filename.contains('.')) filename else s"$filename.html"
    val path = rootPath.resolve(filenameWithSuffix)

    Files.createDirectories(path.getParent)
    Files.write(path, response)
  }

}
