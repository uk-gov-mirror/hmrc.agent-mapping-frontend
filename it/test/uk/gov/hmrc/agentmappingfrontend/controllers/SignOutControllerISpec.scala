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
import uk.gov.hmrc.http.StringContextOps

class SignOutControllerISpec
extends BaseControllerISpec {

  private lazy val controller: SignedOutController = app.injector.instanceOf[SignedOutController]

  private val fakeRequest = FakeRequest()

  def callEndpointWith[A: Writeable](request: Request[A]): Result = await(play.api.test.Helpers.route(app, request).get)

  def signOutUrlWithContinue(continue: String): String =
    url"""${"http://localhost:9099/bas-gateway/sign-out-without-state"}?${Map("continue" -> continue)}""".toString

  "sign out and redirect" should {
    "redirect to /agent-mapping/client-relationships-found while holding arnRef for next mapping iteration" in {
      val id = "someIdToRetrieveArnWithToMapAccount"
      val expectedContinue = url"""${"http://localhost:9438/agent-mapping/start-submit"}?${Map("id" -> id)}"""
      val result = await(controller.signOutAndRedirect(id)(fakeRequest))

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe signOutUrlWithContinue(expectedContinue.toString)
    }
  }

  "reLog and redirect" should {
    "redirect to /agent-mapping/start" in {
      val result = await(controller.reLogForMappingStart(fakeRequest))

      status(result) shouldBe 303
      redirectLocation(result).get should include("agent-services-account")
    }
  }

  "signOut" should {
    "redirect to start" in {
      val result = await(controller.signOut()(fakeRequest))
      val expectedContinue = "http://localhost:9438/agent-mapping"

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe signOutUrlWithContinue(expectedContinue)
    }
  }

  "timeOut" should {
    "ensure user is signed out and redirect to the timed out page" in {
      val expectedContinue = "http://localhost:9438/agent-mapping/timed-out"
      val result = controller.timeOut()(fakeRequest).futureValue
      status(result) shouldBe 303
      redirectLocation(result).get shouldBe signOutUrlWithContinue(expectedContinue)
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
