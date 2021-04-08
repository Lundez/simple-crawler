import java.io.OutputStream
import java.net.URI
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpRequest}
import java.nio.file.{Files, OpenOption, Path, Paths}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.util.matching.Regex
import scala.jdk.FutureConverters._

object Main {
  val regexPath: Regex = "(src|href)=\"/?(.*?)\"".r
  val nanoUnit = 1e9

  def main(args: Array[String]): Unit = {
    val indexUrl = "https://tretton37.com/"
    val rootPath = Paths.get("tretton37")
    val httpCode = requests.get(indexUrl)
    saveFile(rootPath.resolve("index.html"), httpCode.bytes)
    val uniqueLinks = getAllUniqueLinks(httpCode.text()).map(asset => s"$indexUrl$asset")
    makeParallellRequests(uniqueLinks)
      .foreach(req => saveFile(Paths.get(req.url.stripPrefix(indexUrl)), req.bytes))
  }

  def makeParallellRequests(urls: Set[String]) = {
    val futures = for (link <- urls) yield Future{
      println(link)
      val resp = requests.get(link)

      resp
    }
    futures.map(Await.result(_, Duration.fromNanos(30 * nanoUnit)))
  }

  def saveFile(path: Path, filedata: Array[Byte]) = {
    Files.createDirectories(path.getParent)
    Files.write(path, filedata)
  }

  def getAllUniqueLinks(htmlDoc: String): Set[String] =
    regexPath.findAllMatchIn(htmlDoc).toSeq
      .map(_.group(2))
      .filterNot(_.startsWith("javascript:void(0)")) // TODO replace by regexp
      .filterNot(_.startsWith("http"))
      .filterNot(_.startsWith("//cdn"))
      .map(_.stripPrefix("/"))
      .toSet
}