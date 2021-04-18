import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.jdk.CollectionConverters._
import scala.util.Try

object Main {
  val indexUrl = "https://tretton37.com/"
  private var numFetches = 0
  private var numCompletedFetches = 0
  private val concurrentMap = new ConcurrentHashMap[String, Boolean]()

  def main(args: Array[String]): Unit = {
    val time = System.currentTimeMillis()

    println("([number of completed fetches] / [total number of fetches])")
    Try(Await.result(makeParallellRequests(Seq(indexUrl)), Duration.Inf))
      .recover(t => s"Fail ${t.getMessage}")
      .foreach(msg => println(s"\nPotential Errors: $msg"))

    println(s"Took ${(System.currentTimeMillis() - time) / 1e3} s")
    println(s"Files exists in ${FileHandler.rootPath.toAbsolutePath} (works best to view through IntelliJ + open in browser)")
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
            case response if response.url == indexUrl =>
              ResponseData("index.html", HtmlCleaner.cleanHtmlDoc(response.text(), link), getAllUniqueLinks(response.text()))
            case response if response.contentType.exists(isHtmlContent) =>
              ResponseData(s"${response.url.stripPrefix(indexUrl)}.html", HtmlCleaner.cleanHtmlDoc(response.text(), link), getAllUniqueLinks(response.text()))
            case response =>
              ResponseData(response.url.stripPrefix(indexUrl), response.bytes, Seq.empty)
          }
          .flatMap { responseData =>
            Future(FileHandler.saveFile(responseData.filename, responseData.cleanedFileData))
              .zip(makeParallellRequests(responseData.internalLinks))
              .map { case (_, units) => units.headOption }
          }
      }
    Future.sequence(allRequests).map(_.flatten)
  }

  private def getAllUniqueLinks(htmlDoc: String): Seq[String] = {
    val paths = LinkHandler.retrieveAllUniqueAssetLinks(htmlDoc)
      .filterNot(concurrentMap.containsKey)

    concurrentMap.putAll(paths.map(_ -> false).toMap.asJava)

    paths.toSeq
  }

  private def printToLog(message: String): Unit = print(s"\r($numCompletedFetches/$numFetches) $message")
}