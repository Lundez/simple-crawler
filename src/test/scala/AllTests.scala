import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class AllTests extends AnyFlatSpec with should.Matchers {
  "LinkHandler" should "validate valid internal links" in {
    LinkHandler.isAssetToDownload("/assets/v/video.mp4") shouldBe true
    LinkHandler.isAssetToDownload("assets/v/video.mp4") shouldBe true
    LinkHandler.isAssetToDownload("assets/i/img.jpg") shouldBe true
    LinkHandler.isAssetToDownload("page") shouldBe true
    LinkHandler.isAssetToDownload("page.html") shouldBe true
  }
  it should "invalidate links that is not to download" in {
    LinkHandler.isAssetToDownload("https://google.com/assets/v/video.mp4") shouldBe false
    LinkHandler.isAssetToDownload("/cdn...") shouldBe false
    LinkHandler.isAssetToDownload("//cdn") shouldBe false
    LinkHandler.isAssetToDownload("page#hello") shouldBe false
    LinkHandler.isAssetToDownload("tel:+46318") shouldBe false
    LinkHandler.isAssetToDownload("mailto:hampus@gmail.com") shouldBe false
    LinkHandler.isAssetToDownload("javascript:void(0)") shouldBe false
    LinkHandler.isAssetToDownload("asset/{filename}") shouldBe false
  }

  it should "correctly make raw link" in {
    LinkHandler.toRawAssetLink("https://londogard.com") shouldBe None
    LinkHandler.toRawAssetLink("random#hej") shouldBe None
    LinkHandler.toRawAssetLink("../hej") shouldBe Some("https://tretton37.com/hej")
    LinkHandler.toRawAssetLink("glad man") shouldBe Some("https://tretton37.com/glad%20man")
    LinkHandler.toRawAssetLink("name.css?039ika") shouldBe Some("https://tretton37.com/name.css")
  }

  it should "fetch all unique links" in {
    LinkHandler.retrieveAllUniqueAssetLinks("hello main.css but src=\"hej.css\" with href=\"global/site/blog\"") shouldBe Set("https://tretton37.com/hej.css", "https://tretton37.com/global/site/blog")
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
