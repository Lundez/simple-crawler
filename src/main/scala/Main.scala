import java.nio.file.{Files, Path, Paths}
import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.Try
import scala.util.matching.Regex

object Main {
  val indexUrl = "https://tretton37.com/"
  private var numFetches = 0
  private var numCompletedFetches = 0
  private val regexPath: Regex = "(src|href)=\"/?(.*?)\"".r
  private val rootPath: Path = Paths.get("tretton37")
  private val concurrentMap = new ConcurrentHashMap[String, Boolean]()

  def main(args: Array[String]): Unit = {
    val time = System.currentTimeMillis()

    println("([number of completed fetches] / [total number of fetches])")
    Try(Await.result(makeParallellRequests(Seq(indexUrl)), Duration.Inf))
      .recover(t => s"Fail ${t.getMessage}")
      .foreach(msg => println(s"\nPotential Errors: $msg"))

    println(s"Took ${(System.currentTimeMillis() - time) / 1e3} s")
    println(s"Files exists in $rootPath (works best to view through IntelliJ + open in browser)")
  }

  private def isHtmlContent(contentType: String): Boolean = contentType.contains("html")

  def makeParallellRequests(urls: Seq[String]): Future[Seq[Unit]] = {
    val allRequests = urls
      .map { link =>
        numFetches += 1
        printToLog(s"Fetching $link")
        Future(requests.get(link))
          .map { resp => numCompletedFetches += 1; printToLog(s"Got response from ${resp.url}"); resp }
          .map {
            case response if response.contentType.exists(isHtmlContent) =>
              val url = response.url.stripPrefix(indexUrl)
              ResponseData(s"${if (url.isEmpty) "index" else url}.html", HtmlCleaner.cleanHtmlDoc(response.text(), link), getAllUniqueLinks(response.text()))
            case response =>
              ResponseData(response.url.stripPrefix(indexUrl), response.bytes, Seq.empty)
          }
          .flatMap { responseData =>
            val path = urlToPath(responseData.filename)
            saveFile(path, responseData.cleanedFileData)
            makeParallellRequests(responseData.internalLinks)
          }
      }
    Future.sequence(allRequests).map(_.flatten)
  }

  private def urlToPath(url: String): Path = {
    val filename = url.stripPrefix(indexUrl).split('?').head.replace("%20", " ")
    val filenameWithSuffix = if (filename.contains('.')) filename else s"$filename.html"
    rootPath.resolve(filenameWithSuffix)
  }

  private def saveFile(path: Path, response: Array[Byte]): Path = {
    Files.createDirectories(path.getParent)
    Files.write(path, response)
  }

  private def getFilePath(assetLink: String): Option[String] = {
    if (LinkValidator.isAssetToDownload(assetLink)) {
      Some(assetLink
        .stripPrefix("/")
        .replace("../", "")
        .replace(" ", "%20")
        .split('?').head
      )
    } else None
  }

  private def getAllUniqueLinks(htmlDoc: String): Seq[String] = {
    val paths = regexPath
      .findAllMatchIn(htmlDoc).toSeq
      .map(_.group(2))
      .flatMap(getFilePath)
      .filterNot(_.isBlank)
      .map(asset => s"$indexUrl$asset")
      .filterNot(concurrentMap.containsKey)
      .distinct

    paths.foreach(concurrentMap.put(_, false))

    paths
  }

  private def printToLog(message: String): Unit = print(s"\r($numCompletedFetches/$numFetches) $message")
}