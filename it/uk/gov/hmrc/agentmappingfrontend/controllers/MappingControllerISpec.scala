package uk.gov.hmrc.agentmappingfrontend.controllers

import play.api.http.Writeable
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmappingfrontend.model.Identifier
import uk.gov.hmrc.agentmappingfrontend.stubs.AuthStubs
import uk.gov.hmrc.agentmappingfrontend.stubs.MappingStubs.{mappingExists, mappingIsCreated, mappingKnownFactsIssue}
import uk.gov.hmrc.agentmappingfrontend.support.SampleUsers.{anSAEnrolledAgent, anVATEnrolledAgent, anAgentNotEnrolled}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}
import uk.gov.hmrc.domain.SaAgentReference

class MappingControllerISpec extends BaseControllerISpec with AuthStubs {

  def callEndpointWith[A: Writeable](request:Request[A]): Result = await(play.api.test.Helpers.route(app, request).get)

  "context root" should {
    "redirect to start page" in {
      val request = FakeRequest(GET,"/agent-mapping/")
      val result = callEndpointWith(request)
      status(result) shouldBe 303
      redirectLocation(result).head should include("/start")
    }
  }

  "start" should {
    "display the start page" in {
      val request = FakeRequest(GET, "/agent-mapping/start")
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      bodyOf(result) should include("Connect each of your agent Government Gateway IDs so your accounting software will be able to access your Self Assessment or VAT client information.")
    }
  }

  "startSubmit" should {

    behave like anAuthenticatedEndpoint(GET, "/agent-mapping/start-submit", callEndpointWith)

    "redirect to add-code if the current user is logged in and has legacy agent enrolment for SA" in {
      givenUserIsAuthenticated(anSAEnrolledAgent)
      val request = fakeRequest(GET, "/agent-mapping/start-submit")
      val result = callEndpointWith(request)
      status(result) shouldBe 303
      redirectLocation(result).get shouldBe routes.MappingController.showAddCode().url
    }
    "redirect to add-code if the current user is logged in and has legacy agent enrolment for VAT" in {
      givenUserIsAuthenticated(anVATEnrolledAgent)
      val request = fakeRequest(GET, "/agent-mapping/start-submit")
      val result = callEndpointWith(request)
      status(result) shouldBe 303
      redirectLocation(result).get shouldBe routes.MappingController.showAddCode().url
    }
  }

  "show add-code" should {

    val endpoint = "/agent-mapping/add-code"

    behave like anEndpointReachableGivenAgentAffinityGroupAndIrSaAgentEnrolment(GET, endpoint,
      expectCheckAgentRefCodeAudit = true)(callEndpointWith)

    "display the add code page if the current user is logged in and has legacy agent enrolment for SA" in {
      givenUserIsAuthenticated(anSAEnrolledAgent)
      val request = fakeRequest(GET, endpoint)
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      bodyOf(result) should include("Connect to your Agent Services account")
      auditEventShouldHaveBeenSent("CheckAgentRefCode")(
        auditDetail("isEnrolledSAAgent" -> "true")
          and auditDetail("saAgentRef" -> "HZ1234")
          and auditDetail("authProviderId" -> "12345-credId")
          and auditDetail("authProviderType" -> "GovernmentGateway")
          and auditTag("transactionName" -> "check-agent-ref-code")
      )
    }

    "display the add code page if the current user is logged in and has legacy agent enrolment for VAT" in {
      givenUserIsAuthenticated(anVATEnrolledAgent)
      val request = fakeRequest(GET, endpoint)
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      bodyOf(result) should include("Connect to your Agent Services account")
      auditEventShouldHaveBeenSent("CheckAgentRefCode")(
        auditDetail("isEnrolledVATAgent" -> "true")
          and auditDetail("vatAgentRef" -> "HZ1234")
          and auditDetail("authProviderId" -> "12345-credId")
          and auditDetail("authProviderType" -> "GovernmentGateway")
          and auditTag("transactionName" -> "check-agent-ref-code")
      )
    }

    "display the SA Agent Reference if the current user is logged in and has legacy agent enrolment for SA" in {
      givenUserIsAuthenticated(anSAEnrolledAgent)
      val request = fakeRequest(GET, endpoint)
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      auditEventShouldHaveBeenSent("CheckAgentRefCode")(
        auditDetail("isEnrolledSAAgent" -> "true")
          and auditDetail("saAgentRef" -> "HZ1234")
          and auditDetail("authProviderId" -> "12345-credId")
          and auditDetail("authProviderType" -> "GovernmentGateway")
          and auditTag("transactionName" -> "check-agent-ref-code")
      )
    }

    "display the VAT Agent Reference if the current user is logged in and has legacy agent enrolment for VAT" in {
      givenUserIsAuthenticated(anVATEnrolledAgent)
      val request = fakeRequest(GET, endpoint)
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      auditEventShouldHaveBeenSent("CheckAgentRefCode")(
        auditDetail("isEnrolledVATAgent" -> "true")
          and auditDetail("vatAgentRef" -> "HZ1234")
          and auditDetail("authProviderId" -> "12345-credId")
          and auditDetail("authProviderType" -> "GovernmentGateway")
          and auditTag("transactionName" -> "check-agent-ref-code")
      )
    }
  }

  "submit add code" should {

    val endpoint = "/agent-mapping/add-code"

    behave like anEndpointReachableGivenAgentAffinityGroupAndIrSaAgentEnrolment(POST, endpoint,
      expectCheckAgentRefCodeAudit = false)(callEndpointWith)

    "redirect to complete if the user enters an ARN and UTR that match the known facts for SA" in {
      givenUserIsAuthenticated(anSAEnrolledAgent)
      mappingIsCreated(Utr("2000000000"),Arn("TARN0000001"), anSAEnrolledAgent.identifier)
      val request = fakeRequest(POST, endpoint).withFormUrlEncodedBody("arn.arn" -> "TARN0000001", "utr.value" -> "2000000000")
      val result = callEndpointWith(request)

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe routes.MappingController.complete().url
    }

    "redirect to complete if the user enters an ARN and UTR that match the known facts for VAT" in {
      givenUserIsAuthenticated(anVATEnrolledAgent)
      mappingIsCreated(Utr("2000000000"),Arn("TARN0000001"), anVATEnrolledAgent.identifier)
      val request = fakeRequest(POST, endpoint).withFormUrlEncodedBody("arn.arn" -> "TARN0000001", "utr.value" -> "2000000000")
      val result = callEndpointWith(request)

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe routes.MappingController.complete().url
    }

    "redirect to the already-mapped page if the mapping already exists for SA" in new App {
      givenUserIsAuthenticated(anSAEnrolledAgent)
      mappingExists(Utr("2000000000"),Arn("TARN0000001"), anSAEnrolledAgent.identifier)

      val request = fakeRequest(POST, endpoint).withFormUrlEncodedBody("arn.arn" -> "TARN0000001", "utr.value" -> "2000000000")
      val result = callEndpointWith(request)

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe routes.MappingController.alreadyMapped().url
    }

    "redirect to the already-mapped page if the mapping already exists for VAT" in new App {
      givenUserIsAuthenticated(anVATEnrolledAgent)
      mappingExists(Utr("2000000000"),Arn("TARN0000001"), anVATEnrolledAgent.identifier)

      val request = fakeRequest(POST, endpoint).withFormUrlEncodedBody("arn.arn" -> "TARN0000001", "utr.value" -> "2000000000")
      val result = callEndpointWith(request)

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe routes.MappingController.alreadyMapped().url
    }

    "redisplay the form " when {
      "there is no ARN " in {
        givenUserIsAuthenticated(anSAEnrolledAgent)
        val request = fakeRequest(POST, endpoint).withFormUrlEncodedBody("utr.value" -> "2000000000")
        val result = callEndpointWith(request)

        status(result) shouldBe 200
        bodyOf(result) should include("This field is required")
        bodyOf(result) should include("2000000000")
      }

      "the arn is invalid" in {
        givenUserIsAuthenticated(anSAEnrolledAgent)
        val request = fakeRequest(POST, endpoint).withFormUrlEncodedBody("arn.arn" -> "ARN0000001", "utr.value" -> "2000000000")
        val result = callEndpointWith(request)

        status(result) shouldBe 200
        bodyOf(result) should include("Check you have entered a valid account number")
        bodyOf(result) should include("2000000000")
        bodyOf(result) should include("ARN0000001")
      }

      "there is no UTR " in {
        givenUserIsAuthenticated(anSAEnrolledAgent)
        val request = fakeRequest(POST, endpoint).withFormUrlEncodedBody("arn.arn" -> "TARN0000001")
        val result = callEndpointWith(request)

        status(result) shouldBe 200
        bodyOf(result) should include("This field is required")
        bodyOf(result) should include("TARN0000001")
      }

      "the utr is invalid" in {
        givenUserIsAuthenticated(anSAEnrolledAgent)
        val request = fakeRequest(POST, endpoint).withFormUrlEncodedBody("arn.arn" -> "TARN0000001", "utr.value" -> "notautr")
        val result = callEndpointWith(request)

        status(result) shouldBe 200
        bodyOf(result) should include("Check you have entered a valid UTR or tax reference")
        bodyOf(result) should include("notautr")
        bodyOf(result) should include("TARN0000001")
      }

      "the known facts check fails" in {
        givenUserIsAuthenticated(anSAEnrolledAgent)
        mappingKnownFactsIssue(Utr("2000000000"),Arn("TARN0000001"), anSAEnrolledAgent.identifier)

        val request = fakeRequest(POST, endpoint).withFormUrlEncodedBody("arn.arn" -> "TARN0000001", "utr.value" -> "2000000000")
        val result = callEndpointWith(request)

        status(result) shouldBe 200
        bodyOf(result) should include(htmlEscapedMessage("error.summary.heading"))
      }
    }
  }

  "complete" should {

    behave like anEndpointReachableGivenAgentAffinityGroupAndIrSaAgentEnrolment(GET, s"/agent-mapping/complete",
      expectCheckAgentRefCodeAudit = false)(callEndpointWith)

    "display the complete page for an arn and ir sa agent reference" in {
      givenUserIsAuthenticated(anSAEnrolledAgent)
      val saRef: Seq[Identifier] = anSAEnrolledAgent.identifier
      val request = fakeRequest(GET, s"/agent-mapping/complete")
      val result = callEndpointWith(request)
      val resultBody: String = bodyOf(result)
      status(result) shouldBe 200
      resultBody should include(htmlEscapedMessage("connectionComplete.title"))
      resultBody should include(htmlEscapedMessage("button.repeatProcess"))
      resultBody should include(htmlEscapedMessage("button.signOut"))
    }

    "display the complete page for an arn and vat agent reference" in {
      givenUserIsAuthenticated(anVATEnrolledAgent)
      val vatRef: Seq[Identifier] = anVATEnrolledAgent.identifier
      val request = fakeRequest(GET, s"/agent-mapping/complete")
      val result = callEndpointWith(request)
      val resultBody: String = bodyOf(result)
      status(result) shouldBe 200
      resultBody should include(htmlEscapedMessage("connectionComplete.title"))
      resultBody should include(htmlEscapedMessage("button.repeatProcess"))
      resultBody should include(htmlEscapedMessage("button.signOut"))
    }
  }

 "not enrolled " should {
   "contain a message indicating if the user has not enrolled for IR-SA-AGENT" in {
     val request = fakeRequest(GET, "/agent-mapping/not-enrolled")
     val result = callEndpointWith(request)
     status(result) shouldBe 200
     bodyOf(result) should include(htmlEscapedMessage("notEnrolled.p1"))
   }
 }
}
