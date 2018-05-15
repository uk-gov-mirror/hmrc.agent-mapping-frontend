package uk.gov.hmrc.agentmappingfrontend

import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.agentmappingfrontend.stubs.AuthStubs
import uk.gov.hmrc.agentmappingfrontend.support.WireMockSupport
import uk.gov.hmrc.play.test.UnitSpec

class MappingISpec extends UnitSpec with GuiceOneServerPerSuite with WireMockSupport with AuthStubs {

  "Mapping application" should {
    "render an error if auth fails with 5xx" in {
      givenAuthorisationFailsWith5xx()
      val response = await(ws.url(s"http://localhost:$port/agent-mapping/start-submit").get())
      response.status shouldBe 200
      response.body should include(htmlEscapedMessage("global.error.500.message"))
    }
  }

  override implicit lazy val app: Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder = {
    new GuiceApplicationBuilder().configure(
      "microservice.services.auth.port" -> wireMockPort,
      "microservice.services.agent-mapping.port" -> wireMockPort
    )
  }

  private lazy val ws = app.injector.instanceOf[WSClient]
  private lazy val messagesApi = app.injector.instanceOf[MessagesApi]
  private implicit lazy val messages: Messages = messagesApi.preferred(Seq.empty[Lang])
  protected def htmlEscapedMessage(key: String): String = HtmlFormat.escape(Messages(key)).toString

}
