object LinkValidator {
  private def isNonAsset(asset: String): Boolean =
    asset.startsWith("javascript:void(0)") || asset.startsWith("http") ||
      asset.contains("mailto") || asset.contains("tel") || asset.contains("/cdn")

  private def isDynamicLink(asset: String): Boolean =
    asset.contains('#') || asset.contains('{')

  def isAssetToDownload(asset: String): Boolean =
    !(isNonAsset(asset) || isDynamicLink(asset))
}
