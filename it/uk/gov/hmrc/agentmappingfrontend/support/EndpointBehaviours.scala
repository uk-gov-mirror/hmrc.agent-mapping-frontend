package uk.gov.hmrc.agentmappingfrontend.support

import akka.stream.Materializer
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Request, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.agentmappingfrontend.controllers.routes
import uk.gov.hmrc.agentmappingfrontend.stubs.AuthStubs
import uk.gov.hmrc.agentmappingfrontend.support.SampleUsers._
import uk.gov.hmrc.play.test.UnitSpec

trait EndpointBehaviours extends AuthStubs {
  me: UnitSpec with WireMockSupport with AuditSupport =>
  type PlayRequest = Request[AnyContent] => Result

  protected def fakeRequest(endpointMethod: String, endpointPath: String): FakeRequest[AnyContentAsEmpty.type]
  protected def materializer: Materializer

  implicit lazy val mat = materializer

  protected def anAuthenticatedEndpoint(endpointMethod: String, endpointPath:String, doRequest: Request[AnyContentAsEmpty.type] => Result): Unit = {
    "redirect to the company-auth-frontend sign-in page if the current user is not logged in" in {
      givenUserIsNotAuthenticated()
      val request = fakeRequest(endpointMethod, endpointPath)
      val result = await(doRequest(request))

      result.header.status shouldBe 303
      result.header.headers("Location") should include("/gg/sign-in")
      auditEventShouldNotHaveBeenSent("CheckAgentRefCode")
    }

    "redirect to the start page if the current user is logged in and does not have affinity group Agent" in {
      givenUserIsAuthenticated(individual)
      val request = fakeRequest(endpointMethod,endpointPath)
      val result = await(doRequest(request))

      result.header.status shouldBe 303
      result.header.headers("Location") shouldBe routes.MappingController.notEnrolled().url

      auditEventShouldNotHaveBeenSent("CheckAgentRefCode")
    }
  }

  protected def anEndpointReachableGivenAgentAffinityGroupAndIrSaAgentEnrolment(endpointMethod: String, endpointPath: String,
                                                                                expectCheckAgentRefCodeAudit: Boolean)
                                                                               (doRequest: FakeRequest[AnyContentAsEmpty.type] => Result): Unit = {
    "redirect to the company-auth-frontend sign-in page if the current user is not logged in" in {
      givenUserIsNotAuthenticated()

      val request = fakeRequest(endpointMethod, endpointPath)
      val result = await(doRequest(request))

      result.header.status shouldBe 303
      result.header.headers("Location") should include("/gg/sign-in")
      auditEventShouldNotHaveBeenSent("CheckAgentRefCode")
    }

    "redirect to the start page if the current user is logged in and does not have affinity group Agent" in {
      givenUserIsAuthenticated(individual)
      val request = fakeRequest(endpointMethod,endpointPath)
      val result = await(doRequest(request))

      result.header.status shouldBe 303
      result.header.headers("Location") shouldBe routes.MappingController.notEnrolled().url

      auditEventShouldNotHaveBeenSent("CheckAgentRefCode")
    }

    "redirect to the start page if the current user is logged with affinity group Agent but is not enrolled to IR-SA-AGENT " in {
      givenUserIsAuthenticated(anAgentNotEnrolled)
      val request = fakeRequest(endpointMethod,endpointPath)
      val result = await(doRequest(request))

      result.header.status shouldBe 303
      result.header.headers("Location") shouldBe routes.MappingController.notEnrolled().url

      if (expectCheckAgentRefCodeAudit)
        auditEventShouldHaveBeenSent("CheckAgentRefCode")(
          auditDetail("isEnrolledSAAgent" -> "false")
            and not(auditDetailKey("saAgentRef"))
            and auditDetail("authProviderId" -> "12345-credId")
            and auditDetail("authProviderType" -> "GovernmentGateway")
            and auditTag("transactionName" -> "check-agent-ref-code")
        )
      else auditEventShouldNotHaveBeenSent("CheckAgentRefCode")
    }

    "redirect to the start page if the current user is logged with affinity group Agent but has an inactive enrolment to IR-SA-AGENT " in {
      givenUserIsAuthenticated(anSAEnrolledAgentInactive)
      val request = fakeRequest(endpointMethod,endpointPath)
      val result = await(doRequest(request))

      bodyOf(result) shouldBe ""
      result.header.status shouldBe 303
      result.header.headers("Location") shouldBe routes.MappingController.notEnrolled().url

      if (expectCheckAgentRefCodeAudit)
        auditEventShouldHaveBeenSent("CheckAgentRefCode")(
          auditDetail("isEnrolledSAAgent" -> "false")
            and not(auditDetailKey("saAgentRef"))
            and auditDetail("authProviderId" -> "12345-credId")
            and auditDetail("authProviderType" -> "GovernmentGateway")
            and auditTagsNotEmpty("path", "X-Session-ID", "X-Request-ID", "clientIP", "clientPort")
            and auditTag("transactionName" -> "check-agent-ref-code")
        )
      else auditEventShouldNotHaveBeenSent("CheckAgentRefCode")
    }
  }
}
