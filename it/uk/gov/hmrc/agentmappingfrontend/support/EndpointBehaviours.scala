package uk.gov.hmrc.agentmappingfrontend.support

import play.api.mvc.{AnyContent, AnyContentAsEmpty, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmappingfrontend.controllers.routes
import uk.gov.hmrc.agentmappingfrontend.stubs.AuthStub
import uk.gov.hmrc.agentmappingfrontend.stubs.AuthStub._
import uk.gov.hmrc.agentmappingfrontend.support.SampleUsers.{individual, subscribingAgent}
import uk.gov.hmrc.play.test.UnitSpec

trait EndpointBehaviours {
  me: UnitSpec with WireMockSupport =>
  type PlayRequest = Request[AnyContent] => Result

  protected def authenticatedRequest(): FakeRequest[AnyContentAsEmpty.type]

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
      result.header.headers("Location") shouldBe routes.MappingController.notEnrolled().url
    }

    "redirect to the start page if the current user is logged with affinity group Agent but is not enrolled to IR-SA-AGENT " in {
      isNotEnrolled(subscribingAgent)

      val sessionKeys = userIsAuthenticated(subscribingAgent)
      val request = FakeRequest().withSession(sessionKeys: _*)
      val result = await(doRequest(request))

      result.header.status shouldBe 303
      result.header.headers("Location") shouldBe routes.MappingController.notEnrolled().url
    }

    "redirect to the start page if the current user is logged with affinity group Agent but has an inactive enrolment to IR-SA-AGENT " in {
      isEnrolled(subscribingAgent, "Inactive")

      val sessionKeys = userIsAuthenticated(subscribingAgent)
      val request = FakeRequest().withSession(sessionKeys: _*)
      val result = await(doRequest(request))

      result.header.status shouldBe 303
      result.header.headers("Location") shouldBe routes.MappingController.notEnrolled().url
    }
  }

  protected def aWhitelistedEndpoint(doRequest: PlayRequest): Unit = {
    "prevent access if passcode authorisation fails" in {
      AuthStub.isNotEnrolled(subscribingAgent)

      AuthStub.passcodeAuthorisationFails()

      val request = authenticatedRequest()
      val result = await(doRequest(request))

      status(result) shouldBe 303
      result.header.headers("Location") should include("verification/otac")
    }

    "allow access if passcode authorisation succeeds" in {
      AuthStub.isNotEnrolled(subscribingAgent)

      val sessionKeys = AuthStub.passcodeAuthorisationSucceeds()

      val request = authenticatedRequest().withSession(sessionKeys: _*)
      val result = await(doRequest(request))

      redirectLocation(result) match {
        case Some(location) => location should not include "verification/otac"
        case None =>
      }
    }
  }
}
