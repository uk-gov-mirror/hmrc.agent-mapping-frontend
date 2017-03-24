package uk.gov.hmrc.agentmappingfrontend.controllers

import play.api.mvc.Result
import play.api.test.FakeRequest

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
    "display the start page" in {
      val result: Result = await(controller.addCode(FakeRequest()))
      status(result) shouldBe 200
      bodyOf(result) should include("Agent codes")
    }
  }

  "complete" should {
    "display the start page" in {
      val result: Result = await(controller.complete(FakeRequest()))
      status(result) shouldBe 200
      bodyOf(result) should include("You have successfully added the following codes")
    }
  }
}
