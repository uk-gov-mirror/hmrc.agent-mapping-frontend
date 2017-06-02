package uk.gov.hmrc.agentmappingfrontend.controllers

import play.api.test.FakeRequest
import play.api.test.Helpers.redirectLocation
import play.api.test.Helpers._

class SignOutControllerISpec extends BaseControllerISpec{
  private lazy val controller: SignedOutController = app.injector.instanceOf[SignedOutController]

  private val fakeRequest = FakeRequest()

  "context signed out" should {
    "redirect to gov.uk page" in {
      val result = await(controller.signOut(fakeRequest))

      status(result) shouldBe 303
      redirectLocation(result).head should include("www.gov.uk")
    }
  }
}
