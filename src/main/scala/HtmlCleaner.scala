import scala.util.matching.Regex

object HtmlCleaner {
  private val fileCacheRegex: Regex = raw"(\?[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12})".r
  private val htmlAssetRegex: Regex = "\\b(src|href)=\"([/\\w-]+)\"".r
  private val rootCacheRegex: Regex = "\"/(\\w+)".r

  def cleanHtmlDoc(htmlDoc: String, link: String): Array[Byte] = {
    val parentLevel = "../".repeat(link.replace(Main.indexUrl, "").count(_ == '/'))
    val noCacheHtml = fileCacheRegex.replaceAllIn(htmlDoc, "")
    val fixedHtmlFileEnding = htmlAssetRegex.replaceAllIn(noCacheHtml, "$1=\"$2.html\"")
    val fixedAssetsHtml = rootCacheRegex.replaceAllIn(fixedHtmlFileEnding, "\""+parentLevel+"$1")
    fixedAssetsHtml.replace("\"/.html\"", "\"index.html\"").getBytes()
  }
}
