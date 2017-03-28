package uk.gov.hmrc.agentmappingfrontend.support

import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.agentmappingfrontend.controllers.routes
import uk.gov.hmrc.agentmappingfrontend.stubs.AuthStub.{isNotEnrolled, userIsAuthenticated, userIsNotAuthenticated}
import uk.gov.hmrc.agentmappingfrontend.support.SampleUsers.{individual, subscribingAgent}
import uk.gov.hmrc.play.test.UnitSpec

trait EndpointBehaviours {
  me: UnitSpec with WireMockSupport =>

  protected def anEndpointAccessableGivenAgentAffinityGroupAndEnrolmentIrSAAgent(doRequest: FakeRequest[AnyContentAsEmpty.type] => Result): Unit = {
    "redirect to the company-auth-frontend sign-in page if the current user is not logged in" in {
      userIsNotAuthenticated()

      val request = FakeRequest()
      val result = await(doRequest(request))

      result.header.status shouldBe 303
      result.header.headers("Location") should include("/gg/sign-in")
    }

    "redirect to the start page if the current user is logged in and does not have affinity group Agent" in {
      val sessionKeys = userIsAuthenticated(individual)

      val request = FakeRequest().withSession(sessionKeys: _*)
      val result = await(doRequest(request))

      result.header.status shouldBe 303
      result.header.headers("Location") shouldBe routes.MappingController.start().url
    }

    "redirect to the start page if the current user is logged with affinity group Agent but is not enrolled to IR-PAYE-AGENT " in {
      isNotEnrolled(subscribingAgent)

      val sessionKeys = userIsAuthenticated(subscribingAgent)
      val request = FakeRequest().withSession(sessionKeys: _*)
      val result = await(doRequest(request))

      result.header.status shouldBe 303
      result.header.headers("Location") shouldBe routes.MappingController.start().url
    }
  }
}
