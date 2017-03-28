package uk.gov.hmrc.agentmappingfrontend.controllers

import play.api.mvc.Result
import play.api.test.FakeRequest
import uk.gov.hmrc.agentmappingfrontend.stubs.AuthStub.isEnrolled
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

  "add-code" should {

    behave like anAgentAffinityGroupOnlyEndpoint(request => controller.addCode(request))

    "display the start page if the current user is logged in and has legacy agent enrolment" in {
      isEnrolled(subscribingAgent)
      val result: Result = await(controller.addCode(authenticatedRequest()))
      status(result) shouldBe 200
      bodyOf(result) should include("Agent codes")
    }
  }

  "complete" should {

    behave like anAgentAffinityGroupOnlyEndpoint(request => controller.complete(request))

    "display the start page" in {
      isEnrolled(subscribingAgent)
      val result: Result = await(controller.complete(authenticatedRequest()))
      status(result) shouldBe 200
      bodyOf(result) should include("You have successfully added the following codes")
    }
  }
}
