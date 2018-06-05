package uk.gov.hmrc.agentmappingfrontend.controllers

import play.api.test.FakeRequest
import play.api.test.Helpers.redirectLocation
import play.api.test.Helpers._

class SignOutControllerISpec extends BaseControllerISpec {
  private lazy val controller: SignedOutController = app.injector.instanceOf[SignedOutController]

  private val fakeRequest = FakeRequest()

  "sign out" should {
    "redirect to /agent-mapping/start" in {
      val result = await(controller.signOut(fakeRequest))

      status(result) shouldBe 303
      redirectLocation(result).get should include("agent-mapping%2Fstart")
    }
  }

  "sign out and redirect" should {
    "redirect to /agent-mapping/enter-account-number" in {
      val result = await(controller.signOutAndRedirect(fakeRequest))

      status(result) shouldBe 303
      redirectLocation(result).get should include("agent-mapping%2Fenter-account-number")
    }
  }
}
