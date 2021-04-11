import java.net.URLEncoder
import java.nio.file.{Files, Path, Paths}
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern
import javax.swing.text.html.HTML
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.Try
import scala.util.matching.Regex

case class ResponseData(filename: String,
                        cleanedFileData: Array[Byte],
                        internalLinks: Set[String])

object Main {
  var numFetches = 1
  var numCompletedFetches = 1
  val regexPath: Regex = "(src|href)=\"/?(.*?)\"".r
  val nanoUnit = 1e9
  val indexUrl = "https://tretton37.com/"
  val rootPath = Paths.get("tretton37")
  val concurrentMap = new ConcurrentHashMap[String, Boolean]()

  def main(args: Array[String]): Unit = {
    val httpCode = requests.get(indexUrl)
    concurrentMap.put("", true)
    println("([number of completed fetches] / [total number of fetches])")
    saveFile(rootPath.resolve("index.html"), cleanHtmlFile(httpCode.text()))

    val uniqueLinks = getAllUniqueLinks(httpCode.text())
    val time = System.currentTimeMillis()
    Try(Await.result(makeParallellRequests(uniqueLinks), Duration.Inf))
      .recover(t => s"Fail ${t.getMessage}")
      .foreach(println)
    println(s"Took ${(System.currentTimeMillis() - time)/1e3} s")
  }

  def urlToPath(url: String): Path = {
    val filename = url.stripPrefix(indexUrl).split('?').head
    val filenameWithSuffix = if (filename.contains('.')) filename else s"$filename.html"
    rootPath.resolve(filenameWithSuffix)
  }

  def makeParallellRequests(urls: Set[String]): Future[Seq[Unit]] = {
    val allRequests = urls
      .map { link =>
        numFetches +=1
        printToLog(s"Fetching $link")
        Future(requests.get(link))
          .map { resp => numCompletedFetches+=1;printToLog(s"Got response from ${resp.url}"); resp}
          .map {
            case response if response.contentType.exists(isHtmlContent) =>
              ResponseData(s"${response.url.stripPrefix(indexUrl)}.html", cleanHtmlFile(response.text()), getAllUniqueLinks(response.text()))
            case response =>
              ResponseData(response.url.stripPrefix(indexUrl), response.bytes, Set.empty)
          }
          .flatMap { responseData =>
            val path = urlToPath(responseData.filename)
            saveFile(path, responseData.cleanedFileData)
            makeParallellRequests(responseData.internalLinks)
          }
      }
    Future.sequence(allRequests).map(_.flatten.toSeq)
  }

  def isHtmlContent(contentType: String): Boolean = contentType.contains("html")
  val fileCacheRegex: Regex = raw"(\?[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12})".r
  val rootCacheRegex: Regex = "\"/(\\w+)".r
  val htmlAssetRegex: Regex = "(src|href)=\"([/\\w-]+)\"".r

  def cleanHtmlFile(content: String): Array[Byte] = {
    val noCacheHtml = fileCacheRegex.replaceAllIn(content, "")
    val fixedAssetsHtml = rootCacheRegex.replaceAllIn(noCacheHtml, "\"$1")
    val fixedHtmlFileEnding = htmlAssetRegex.replaceAllIn(fixedAssetsHtml, "$1=\"$2.html\"")
    fixedHtmlFileEnding.replace("\"/.html\"", "\"index.html\"").getBytes()
  }

  def saveFile(path: Path, response: Array[Byte]): Path = {
    Files.createDirectories(path.getParent)
    Files.write(path, response)
  }

  val filenameRegex: Regex = raw"/?([\w-\\.])".r
  val relativePath: Regex = Pattern.quote("../").r

  def isNonAsset(asset: String): Boolean =
    asset.startsWith("javascript:void(0)") || asset.startsWith("http") ||
      asset.contains("mailto") || asset.contains("tel") || asset.contains("/cdn")

  def isDynamicLink(asset: String): Boolean =
    asset.contains('#') || asset.contains('{')

  def getFilePath(assetLink: String): Option[String] = {
    assetLink match {
      case asset if isNonAsset(asset) => None
      case asset if isDynamicLink(asset) => None
      case asset => Some(asset.stripPrefix("/").replace("../", "").replace(" ", "%20"))
        .map(fileCacheRegex.replaceAllIn(_, ""))
    }
  }

  def getAllUniqueLinks(htmlDoc: String): Set[String] = {
    val paths = regexPath.findAllMatchIn(htmlDoc).toSeq
      .map(_.group(2))
      .flatMap(getFilePath)
      .filterNot(_.isBlank)
      .map(asset => s"$indexUrl$asset")
      .filterNot(concurrentMap.containsKey)
      .toSet
    paths.foreach(concurrentMap.put(_, false))

    paths
  }

  def printToLog(message: String): Unit = {
    print("\r")
    print(s"($numCompletedFetches/$numFetches) $message")
  }
}