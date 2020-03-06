package uk.gov.hmrc.agentmappingfrontend.controllers

import play.api.test.FakeRequest
import play.api.test.Helpers.{cookies, redirectLocation}

import scala.concurrent.duration._

class AgentMappingLanguageControllerISpec extends BaseControllerISpec {


  implicit val timeout = 2.seconds


  lazy private val controller: AgentMappingLanguageController = app.injector.instanceOf[AgentMappingLanguageController]


  "GET /language/:lang" should {

    val request = FakeRequest("GET", "/language/english")

    "redirect to https://www.tax.service.gov.uk/agent-mapping when the request header contains no referer" in {

      val result = controller.switchToLanguage("english")(request)
      status(result) shouldBe 303
      redirectLocation(result)(timeout) shouldBe Some("https://www.tax.service.gov.uk/agent-mapping")

      cookies(result)(timeout).get("PLAY_LANG").get.value shouldBe "en"
    }

    "redirect to /some-page when the request header contains referer /some-page" in {

      val request = FakeRequest("GET", "/language/english").withHeaders("referer" -> "/some-page")

      val result = controller.switchToLanguage("english")(request)
      status(result) shouldBe 303
      redirectLocation(result)(timeout) shouldBe Some("/some-page")

      cookies(result)(timeout).get("PLAY_LANG").get.value shouldBe "en"
    }

    "redirect to /some-page with lang set to 'cy' when the user has selected Welsh" in {

      val request = FakeRequest("GET", "/language/cymraeg").withHeaders("referer" -> "/some-page")

      val result = controller.switchToLanguage("cymraeg")(request)
      status(result) shouldBe 303
      redirectLocation(result)(timeout) shouldBe Some("/some-page")

      cookies(result)(timeout).get("PLAY_LANG").get.value shouldBe "cy"
    }
  }

}
