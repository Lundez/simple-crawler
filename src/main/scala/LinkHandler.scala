import scala.util.matching.Regex

object LinkHandler {
  private val assetRegex: Regex = "(src|href)=\"/?(.*?)\"".r

  private def isNonAsset(asset: String): Boolean =
    asset.startsWith("javascript:void(0)") || asset.startsWith("http") ||
      asset.contains("mailto") || asset.contains("tel") || asset.contains("/cdn")

  private def isDynamicLink(asset: String): Boolean =
    asset.contains('#') || asset.contains('{')

  def isAssetToDownload(asset: String): Boolean =
    !(isNonAsset(asset) || isDynamicLink(asset))

  def toRawAssetLink(asset: String): Option[String] = {
    if (isAssetToDownload(asset)) {
      asset
        .stripPrefix("/")
        .replace("../", "")
        .replace(" ", "%20")
        .split('?')
        .headOption
        .filterNot(_.isBlank)
        .map(cleanAsset => s"${Main.indexUrl}$cleanAsset")
    } else None
  }

  def retrieveAllUniqueAssetLinks(htmlDoc: String): Set[String] = {
    assetRegex
      .findAllMatchIn(htmlDoc)
      .map(_.group(2))
      .flatMap(toRawAssetLink)
      .toSet
  }
}
