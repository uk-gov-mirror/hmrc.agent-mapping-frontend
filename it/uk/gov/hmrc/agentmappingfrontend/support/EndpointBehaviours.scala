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

  protected def anAuthenticatedEndpoint(
    endpointMethod: String,
    endpointPath: String,
    doRequest: Request[AnyContentAsEmpty.type] => Result): Unit = {
    "redirect to the sign-in page if the current user is not logged in" in {
      givenUserIsNotAuthenticated()
      val request = fakeRequest(endpointMethod, endpointPath)
      val result = await(doRequest(request))

      result.header.status shouldBe 303
      result.header.headers("Location") should include("/gg/sign-in")
      auditEventShouldNotHaveBeenSent("CheckAgentRefCode")
    }
  }

  protected def anEndpointReachableIfSignedInWithEligibleEnrolment(
    endpointMethod: String,
    endpointPath: String,
    expectCheckAgentRefCodeAudit: Boolean)(doRequest: Request[AnyContentAsEmpty.type] => Result): Unit = {
    behave like anAuthenticatedEndpoint(endpointMethod, endpointPath, doRequest)

    "redirect to /not-enrolled page if the current user has an ineligible enrolment" in {
      givenUserIsAuthenticated(notEligibleAgent)
      val request = fakeRequest(endpointMethod, endpointPath)
      val result = await(doRequest(request))

      result.header.status shouldBe 303
      result.header.headers("Location") shouldBe routes.MappingController.notEnrolled(id = "someArnRefForMapping").url

      verifyCheckAgentRefCodeAuditEvent(expectCheckAgentRefCodeAudit, false, notEligibleAgent.activeEnrolments)
    }

    "redirect to /incorrect-account page if the current user has an HMRC-AS-AGENT enrolment" in {
      givenUserIsAuthenticated(mtdAsAgent)
      val request = fakeRequest(endpointMethod, endpointPath)
      val result = await(doRequest(request))

      result.header.status shouldBe 303
      result.header.headers("Location") shouldBe routes.MappingController.incorrectAccount(id = "someArnRefForMapping").url

      verifyCheckAgentRefCodeAuditEvent(expectCheckAgentRefCodeAudit, false, mtdAsAgent.activeEnrolments)
    }

    "redirect to /already-linked page if the current user has an HMRC-AGENT-AGENT enrolment" in {
      givenUserIsAuthenticated(mtdAgentAgent)
      val request = fakeRequest(endpointMethod, endpointPath)
      val result = await(doRequest(request))

      result.header.status shouldBe 303
      result.header.headers("Location") shouldBe routes.MappingController.alreadyMapped(id = "someArnRefForMapping").url

      verifyCheckAgentRefCodeAuditEvent(expectCheckAgentRefCodeAudit, false, mtdAgentAgent.activeEnrolments)
    }

    "redirect to /not-enrolled page if the current user has no enrolments" in {
      givenUserIsAuthenticated(agentNotEnrolled)
      val request = fakeRequest(endpointMethod, endpointPath)
      val result = await(doRequest(request))

      result.header.status shouldBe 303
      result.header.headers("Location") shouldBe routes.MappingController.notEnrolled(id = "someArnRefForMapping").url

      verifyCheckAgentRefCodeAuditEvent(expectCheckAgentRefCodeAudit, false, agentNotEnrolled.activeEnrolments)
    }

    "render the /not-enrolled page if the current user has only inactive enrolments" in {
      givenUserIsAuthenticated(saEnrolledAgentInactive)
      val request = fakeRequest(endpointMethod, endpointPath)
      val result = await(doRequest(request))

      result.header.status shouldBe 303
      result.header.headers("Location") shouldBe routes.MappingController.notEnrolled(id = "someArnRefForMapping").url

      verifyCheckAgentRefCodeAuditEvent(expectCheckAgentRefCodeAudit, false)
    }
  }

  def verifyCheckAgentRefCodeAuditEvent(
    expectCheckAgentRefCodeAudit: Boolean = true,
    eligible: Boolean = true,
    activeEnrolments: Set[String] = Set()) =
    if (expectCheckAgentRefCodeAudit)
      auditEventShouldHaveBeenSent("CheckAgentRefCode")(
        auditDetail("authProviderType"       -> "GovernmentGateway")
          and auditDetail("eligible"         -> eligible.toString)
          and auditDetail("activeEnrolments" -> activeEnrolments.mkString(","))
          and auditTagsNotEmpty("path", "X-Session-ID", "X-Request-ID", "clientIP", "clientPort")
          and auditTag("transactionName" -> "check-agent-ref-code")
      )
    else auditEventShouldNotHaveBeenSent("CheckAgentRefCode")
}
