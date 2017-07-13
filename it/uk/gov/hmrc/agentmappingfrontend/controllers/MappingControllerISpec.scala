package uk.gov.hmrc.agentmappingfrontend.controllers

import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmappingfrontend.stubs.AuthStub.isEnrolled
import uk.gov.hmrc.agentmappingfrontend.stubs.MappingStubs.{mappingExists, mappingIsCreated, mappingKnownFactsIssue}
import uk.gov.hmrc.agentmappingfrontend.support.SampleUsers.subscribingAgent
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}
import uk.gov.hmrc.domain.SaAgentReference

class MappingControllerISpec extends BaseControllerISpec {
  private lazy val controller: MappingController = app.injector.instanceOf[MappingController]

  "context root" should {
    "redirect to start page" in {
      val result = await(controller.root(FakeRequest()))

      status(result) shouldBe 303
      redirectLocation(result).head should include("/start")
    }
  }

  "start" should {
    "display the start page" in {
      val result: Result = await(controller.start(FakeRequest()))
      status(result) shouldBe 200
      bodyOf(result) should include("Connect each of your agent Government Gateway IDs so your accounting software will be able to access your Self Assessment client information.")
    }
  }

  "show add-code" should {

    behave like anEndpointAccessableGivenAgentAffinityGroupAndEnrolmentIrSAAgent(expectCheckAgentRefCodeAudit = true)(request => controller.showAddCode(request))

    "display the add code page if the current user is logged in and has legacy agent enrolment" in {
      isEnrolled(subscribingAgent)
      val result: Result = await(controller.showAddCode(authenticatedRequest()))
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

    "display the SA Agent Reference if the current user is logged in and has legacy agent enrolment" in {
      isEnrolled(subscribingAgent)
      val result: Result = await(controller.showAddCode(authenticatedRequest()))
      status(result) shouldBe 200
      auditEventShouldHaveBeenSent("CheckAgentRefCode")(
        auditDetail("isEnrolledSAAgent" -> "true")
          and auditDetail("saAgentRef" -> "HZ1234")
          and auditDetail("authProviderId" -> "12345-credId")
          and auditDetail("authProviderType" -> "GovernmentGateway")
          and auditTag("transactionName" -> "check-agent-ref-code")
      )
    }
  }

  "submit add code" should {
    behave like anEndpointAccessableGivenAgentAffinityGroupAndEnrolmentIrSAAgent(expectCheckAgentRefCodeAudit = false)(request => controller.submitAddCode(request))

    "redirect to complete if the user enters an ARN and UTR that match the known facts" in {
      isEnrolled(subscribingAgent)
      mappingIsCreated(Utr("2000000000"),Arn("TARN0000001"), subscribingAgent.saAgentReference.get)
      val request = authenticatedRequest().withFormUrlEncodedBody("arn.arn" -> "TARN0000001", "utr.value" -> "2000000000")

      val result = await(controller.submitAddCode(request))

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe routes.MappingController.complete(Arn("TARN0000001"),subscribingAgent.saAgentReference.get).url
    }

    "redirect to the already-mapped page if the mapping already exists" in new App {
      isEnrolled(subscribingAgent)
      mappingExists(Utr("2000000000"),Arn("TARN0000001"), subscribingAgent.saAgentReference.get)

      val request = authenticatedRequest().withFormUrlEncodedBody("arn.arn" -> "TARN0000001", "utr.value" -> "2000000000")
      val result = await(controller.submitAddCode(request))

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe routes.MappingController.alreadyMapped(Arn("TARN0000001"),subscribingAgent.saAgentReference.get).url
    }

    "redisplay the form " when {
      "there is no ARN " in {
        isEnrolled(subscribingAgent)
        val request = authenticatedRequest().withFormUrlEncodedBody("utr.value" -> "2000000000")

        val result = await(controller.submitAddCode(request))

        status(result) shouldBe 200
        bodyOf(result) should include("This field is required")
        bodyOf(result) should include("2000000000")
      }

      "the arn is invalid" in {
        isEnrolled(subscribingAgent)
        val request = authenticatedRequest().withFormUrlEncodedBody("arn.arn" -> "ARN0000001", "utr.value" -> "2000000000")

        val result = await(controller.submitAddCode(request))

        status(result) shouldBe 200
        bodyOf(result) should include("Check you have entered a valid account number")
        bodyOf(result) should include("2000000000")
        bodyOf(result) should include("ARN0000001")
      }

      "there is no UTR " in {
        isEnrolled(subscribingAgent)
        val request = authenticatedRequest().withFormUrlEncodedBody("arn.arn" -> "TARN0000001")

        val result = await(controller.submitAddCode(request))

        status(result) shouldBe 200
        bodyOf(result) should include("This field is required")
        bodyOf(result) should include("TARN0000001")
      }

      "the utr is invalid" in {
        isEnrolled(subscribingAgent)
        val request = authenticatedRequest().withFormUrlEncodedBody("arn.arn" -> "TARN0000001", "utr.value" -> "notautr")

        val result = await(controller.submitAddCode(request))

        status(result) shouldBe 200
        bodyOf(result) should include("Check you have entered a valid UTR or tax reference")
        bodyOf(result) should include("notautr")
        bodyOf(result) should include("TARN0000001")
      }

      "the known facts check fails" in {
        isEnrolled(subscribingAgent)
        mappingKnownFactsIssue(Utr("2000000000"),Arn("TARN0000001"), subscribingAgent.saAgentReference.get)

        val request = authenticatedRequest().withFormUrlEncodedBody("arn.arn" -> "TARN0000001", "utr.value" -> "2000000000")
        val result = await(controller.submitAddCode(request))

        status(result) shouldBe 200
        bodyOf(result) should include(htmlEscapedMessage("error.summary.heading"))
      }
    }
  }

  "complete" should {

    behave like anEndpointAccessableGivenAgentAffinityGroupAndEnrolmentIrSAAgent(expectCheckAgentRefCodeAudit = false)(request => controller.complete(Arn("TARN0000001"), subscribingAgent.saAgentReference.get)(request))

    "display the complete page for an arn and ir sa agent reference" in {
      isEnrolled(subscribingAgent)
      val saRef: SaAgentReference = subscribingAgent.saAgentReference.get
      val result: Result = await(controller.complete(Arn("TARN0000001"),saRef)(authenticatedRequest()))
      status(result) shouldBe 200
      bodyOf(result) should include(htmlEscapedMessage("connectionComplete.title"))
      bodyOf(result) shouldNot include("TARN0000001")
      bodyOf(result) shouldNot include(saRef.value)
    }
  }

 "not enrolled " should {
   "contain a message indicating if the user has enrolled for IR-SA-AGENT" in {
     val result: Result = await(controller.notEnrolled(FakeRequest()))
     status(result) shouldBe 200
     bodyOf(result) should include(htmlEscapedMessage("notEnroled.p1"))
   }
 }
}
