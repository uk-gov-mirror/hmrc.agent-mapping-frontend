/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentmappingfrontend.controllers

import play.api.http.Writeable
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._

class SignOutControllerISpec
extends BaseControllerISpec {

  private lazy val controller: SignedOutController = app.injector.instanceOf[SignedOutController]

  private val fakeRequest = FakeRequest()

  def callEndpointWith[A: Writeable](request: Request[A]): Result = await(play.api.test.Helpers.route(app, request).get)

  "sign out and redirect" should {
    "redirect to /agent-mapping/client-relationships-found while holding arnRef for next mapping iteration" in {
      val result = await(controller.signOutAndRedirect("someIdToRetrieveArnWithToMapAccount")(fakeRequest))

      status(result) shouldBe 303
      redirectLocation(result).get should include(
        "agent-mapping%2Fstart-submit%3Fid%3DsomeIdToRetrieveArnWithToMapAccount"
      )
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

  "taskList" should {
    "redirect to the task list" in {
      val result = await(controller.taskList()(fakeRequest))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("http://localhost:9437/agent-subscription/task-list")
    }
  }

  "returnAfterMapping" should {
    "redirect to the return after mapping url" in {
      val result = await(controller.returnAfterMapping()(fakeRequest))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("http://localhost:9437/agent-subscription/return-after-mapping")
    }
  }

  "signOut" should {
    "redirect to start" in {
      val result = await(controller.signOut()(fakeRequest))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/agent-mapping")
    }
  }

  "timedOut" should {
    "return 403" in {
      val result = await(controller.timedOut()(fakeRequest))

      status(result) shouldBe 403
      checkHtmlResultContainsEscapedMsgs(
        result,
        "timed-out.header",
        "timed-out.p2.link",
        "timed-out.p2"
      )
    }
  }

}
