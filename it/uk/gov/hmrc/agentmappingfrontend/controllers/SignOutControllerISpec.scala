package uk.gov.hmrc.agentmappingfrontend.controllers

import play.api.http.Writeable
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.redirectLocation
import play.api.test.Helpers._

class SignOutControllerISpec extends BaseControllerISpec {
  private lazy val controller: SignedOutController = app.injector.instanceOf[SignedOutController]

  private val fakeRequest = FakeRequest()

  def callEndpointWith[A: Writeable](request: Request[A]): Result = await(play.api.test.Helpers.route(app, request).get)

  "sign out and redirect" should {
    "redirect to /agent-mapping/client-relationships-found while holding arnRef for next mapping iteration" in {
      val result = await(controller.signOutAndRedirect("someIdToRetrieveArnWithToMapAccount")(fakeRequest))

      status(result) shouldBe 303
      redirectLocation(result).get should include("agent-mapping%2Fstart-submit%3Fid%3DsomeIdToRetrieveArnWithToMapAccount")
    }
  }

  "reLog and redirect" should {
    "redirect to /agent-mapping/start" in {
      val result = await(controller.reLogForMappingStart(fakeRequest))

      status(result) shouldBe 303
      redirectLocation(result).get should include("agent-services-account")
    }
  }

  "task list signOutAndRedirect" should {
    "redirect to /agent-subscription/task-list" in {
      val result = await(controller.taskListSignOutAndRedirect("idToReference")(fakeRequest))

      status(result) shouldBe 303
      redirectLocation(result).get should include("agent-mapping%2Ftask-list%2Fstart-submit%3Fid%3DidToReference")
    }
  }

}
