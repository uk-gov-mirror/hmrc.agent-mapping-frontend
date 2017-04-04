package uk.gov.hmrc.agentmappingfrontend.controllers

import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmappingfrontend.model.Arn
import uk.gov.hmrc.agentmappingfrontend.stubs.AuthStub.{isEnrolled, userIsAuthenticated}
import uk.gov.hmrc.agentmappingfrontend.stubs.MappingStubs.{mappingExists, mappingIsCreated}
import uk.gov.hmrc.agentmappingfrontend.support.SampleUsers.subscribingAgent

class MappingControllerISpec extends BaseControllerISpec {
  private lazy val controller: MappingController = app.injector.instanceOf[MappingController]

  "start" should {
    "display the start page" in {
      val result: Result = await(controller.start(FakeRequest()))
      status(result) shouldBe 200
      bodyOf(result) should include("map existing agent codes to your new MTD Subscription")
    }
  }

  "show add-code" should {

    behave like anEndpointAccessableGivenAgentAffinityGroupAndEnrolmentIrSAAgent(request => controller.showAddCode(request))

    "display the add code page if the current user is logged in and has legacy agent enrolment" in {
      isEnrolled(subscribingAgent)
      val result: Result = await(controller.showAddCode(authenticatedRequest()))
      status(result) shouldBe 200
      bodyOf(result) should include("Agent codes")
    }

    "display the SA Agent Reference if the current user is logged in and has legacy agent enrolment" in {
      isEnrolled(subscribingAgent)
      val result: Result = await(controller.showAddCode(authenticatedRequest()))
      status(result) shouldBe 200
      bodyOf(result) should include(">HZ1234")
    }
  }

  "submit add code" should {
    behave like anEndpointAccessableGivenAgentAffinityGroupAndEnrolmentIrSAAgent(request => controller.submitAddCode(request))

    "redirect to complete if the user enters an ARN" in {
      isEnrolled(subscribingAgent)
      mappingIsCreated(Arn("ARN0001"), subscribingAgent.saAgentReference.get)
      val request = authenticatedRequest().withFormUrlEncodedBody("arn.arn" -> "ARN0001")

      val result = await(controller.submitAddCode(request))

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe routes.MappingController.complete().url
    }

    "return 500 if the mapping already exists" in new App {
      isEnrolled(subscribingAgent)
      mappingExists(Arn("ARN0001"), subscribingAgent.saAgentReference.get)

      val sessionKeys = userIsAuthenticated(subscribingAgent)
      val request = FakeRequest("POST", "/agent-mapping/add-code")
                      .withSession(sessionKeys: _*)
                      .withFormUrlEncodedBody("arn.arn" -> "ARN0001")

      val result = await(route(app, request).get)

      status(result) shouldBe 500
    }

    "redisplay the form if there is no ARN " in {
      isEnrolled(subscribingAgent)

      val result = await(controller.submitAddCode(authenticatedRequest()))

      status(result) shouldBe 200
      bodyOf(result) should include("Agent codes")
    }

  }

  "complete" should {

    behave like anEndpointAccessableGivenAgentAffinityGroupAndEnrolmentIrSAAgent(request => controller.complete(request))

    "display the complete page" in {
      isEnrolled(subscribingAgent)
      val result: Result = await(controller.complete(authenticatedRequest()))
      status(result) shouldBe 200
      bodyOf(result) should include("You have successfully added the following codes")
    }
  }

 "not enrolled " should {
   "contain a message indicating if the user has enrolled for IR-SA-AGENT" in {
     val result: Result = await(controller.notEnrolled(FakeRequest()))
     status(result) shouldBe 200
     bodyOf(result) should include("There is no active IR-SA-AGENT enrolment associated with this Government Gateway identifier")
   }
 }
}
