package uk.gov.hmrc.agentmappingfrontend.support

import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.agentmappingfrontend.controllers.routes
import uk.gov.hmrc.agentmappingfrontend.stubs.AuthStub._
import uk.gov.hmrc.agentmappingfrontend.support.SampleUsers.{individual, subscribingAgent}
import uk.gov.hmrc.play.test.UnitSpec

trait EndpointBehaviours {
  me: UnitSpec with WireMockSupport with AuditSupport =>

  protected def anEndpointAccessableGivenAgentAffinityGroupAndEnrolmentIrSAAgent(expectCheckAgentRefCodeAudit: Boolean)(doRequest: FakeRequest[AnyContentAsEmpty.type] => Result): Unit = {
    "redirect to the company-auth-frontend sign-in page if the current user is not logged in" in {
      userIsNotAuthenticated()

      val request = FakeRequest()
      val result = await(doRequest(request))

      result.header.status shouldBe 303
      result.header.headers("Location") should include("/gg/sign-in")
      auditEventShouldNotHaveBeenSent("CheckAgentRefCode")
    }

    "redirect to the start page if the current user is logged in and does not have affinity group Agent" in {
      val sessionKeys = userIsAuthenticated(individual)

      val request = FakeRequest().withSession(sessionKeys: _*)
      val result = await(doRequest(request))

      result.header.status shouldBe 303
      result.header.headers("Location") shouldBe routes.MappingController.notEnrolled().url

      auditEventShouldNotHaveBeenSent("CheckAgentRefCode")
    }

    "redirect to the start page if the current user is logged with affinity group Agent but is not enrolled to IR-SA-AGENT " in {
      isNotEnrolled(subscribingAgent)

      val sessionKeys = userIsAuthenticated(subscribingAgent)
      val request = FakeRequest().withSession(sessionKeys: _*)
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
      isEnrolled(subscribingAgent, "Inactive")

      val sessionKeys = userIsAuthenticated(subscribingAgent)
      val request = FakeRequest().withSession(sessionKeys: _*)
      val result = await(doRequest(request))

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
