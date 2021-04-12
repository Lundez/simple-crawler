import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class AllTests extends AnyFlatSpec with should.Matchers {
  "LinkValidator" should "validate valid internal links" in {
    LinkValidator.isAssetToDownload("/assets/v/video.mp4") shouldBe true
    LinkValidator.isAssetToDownload("assets/v/video.mp4") shouldBe true
    LinkValidator.isAssetToDownload("assets/i/img.jpg") shouldBe true
    LinkValidator.isAssetToDownload("page") shouldBe true
    LinkValidator.isAssetToDownload("page.html") shouldBe true
  }
  it should "invalidate links that is not to download" in {
    LinkValidator.isAssetToDownload("https://google.com/assets/v/video.mp4") shouldBe false
    LinkValidator.isAssetToDownload("/cdn...") shouldBe false
    LinkValidator.isAssetToDownload("//cdn") shouldBe false
    LinkValidator.isAssetToDownload("page#hello") shouldBe false
    LinkValidator.isAssetToDownload("tel:+46318") shouldBe false
    LinkValidator.isAssetToDownload("mailto:hampus@gmail.com") shouldBe false
    LinkValidator.isAssetToDownload("javascript:void(0)") shouldBe false
    LinkValidator.isAssetToDownload("asset/{filename}") shouldBe false
  }

  "HtmlCleaner" should "relativize file correctly for intellij to serve html" in {
    new String(HtmlCleaner.cleanHtmlDoc("\"/asset/v/video.mp4", Main.indexUrl)) shouldBe "\"asset/v/video.mp4"
    new String(HtmlCleaner.cleanHtmlDoc("\"asset/v/video.mp4", Main.indexUrl)) shouldBe "\"asset/v/video.mp4"

    new String(HtmlCleaner.cleanHtmlDoc("\"/asset/v/video.mp4", s"${Main.indexUrl}assets/")) shouldBe "\"../asset/v/video.mp4"
    new String(HtmlCleaner.cleanHtmlDoc("\"/asset/v/video.mp4", s"${Main.indexUrl}assets/v/")) shouldBe "\"../../asset/v/video.mp4"
  }
  it should "do nothing for random" in {
    new String(HtmlCleaner.cleanHtmlDoc("random", Main.indexUrl)) shouldBe "random"
  }

  it should "add html for web-serving to html files" in {
    new String(HtmlCleaner.cleanHtmlDoc("src=\"/content/hello-world\"", Main.indexUrl)) shouldBe "src=\"content/hello-world.html\""
  }

  it should "remove cache from css etc" in {
    new String(HtmlCleaner.cleanHtmlDoc("\"/asset/main.css?9aa752f9-7025-4a7e-9ddb-70e02e43f836", Main.indexUrl)) shouldBe "\"asset/main.css"
  }
}
