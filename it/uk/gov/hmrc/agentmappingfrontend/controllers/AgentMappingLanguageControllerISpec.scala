package uk.gov.hmrc.agentmappingfrontend.controllers

import play.api.test.FakeRequest
import play.api.test.Helpers.redirectLocation
import scala.concurrent.duration._

class AgentMappingLanguageControllerISpec extends BaseControllerISpec {


  implicit val timeout = 2.seconds


  lazy private val controller: AgentMappingLanguageController = app.injector.instanceOf[AgentMappingLanguageController]


  "GET /language/:lang" should {

    val request = FakeRequest("GET", "/language/english")

    "redirect to https://www.gov.uk/fallback when the request header contains no referer" in {

      val result = controller.switchToLanguage("english")(request)
      status(result) shouldBe 303
      redirectLocation(result)(timeout) shouldBe Some("https://www.gov.uk/fallback")


      //TODO test the cookie value



    }

    "redirect to /some-page when the request header contains referer /some-page" in {

      val request = FakeRequest("GET", "/language/english").withHeaders("referer" -> "/some-page")

      val result = controller.switchToLanguage("english")(request)
      status(result) shouldBe 303
      redirectLocation(result)(timeout) shouldBe Some("/some-page")

    }
  }

}
