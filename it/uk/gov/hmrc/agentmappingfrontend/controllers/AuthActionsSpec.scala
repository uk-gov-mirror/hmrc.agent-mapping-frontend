/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.mvc.Results._
import play.api.test.FakeRequest
import uk.gov.hmrc.agentmappingfrontend.auth.AuthActions
import uk.gov.hmrc.agentmappingfrontend.stubs.AuthStubs
import uk.gov.hmrc.auth.core.{AuthConnector, InsufficientEnrolments, MissingBearerToken}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthActionsSpec extends BaseControllerISpec with AuthStubs {

  object TestController extends AuthActions {
    override def authConnector: AuthConnector = app.injector.instanceOf[AuthConnector]
    implicit val hc = HeaderCarrier()
    implicit val request = FakeRequest().withSession(SessionKeys.authToken -> "Bearer XYZ")

    def testWithAuthorisedSAAgent = {
      await(withAuthorisedSAAgent(){request => Future.successful(Ok(request.saAgentReference.value))})
    }
  }

  "withAuthorisedSAAgent" should {

    "check if agent is enrolled for IR-SA-AGENT and extract SaAgentReference" in {
      givenAuthorisedFor(
        "{}",
        s"""{
           |"authorisedEnrolments": [
           |  { "key":"IR-SA-AGENT", "identifiers": [
           |    { "key":"IRAgentReference", "value": "fooSaAgentReference" }
           |  ]}
           |]}""".stripMargin)
      val result = TestController.testWithAuthorisedSAAgent
      status(result) shouldBe 200
      bodyOf(result) shouldBe "fooSaAgentReference"
    }

    "throw an exception if agent is not enrolled for IR-SA-AGENT" in {
      givenAuthorisedFor(
        "{}",
        s"""{
           |"authorisedEnrolments": [
           |  { "key":"IR-FOO-AGENT", "identifiers": [
           |    { "key":"IRAgentReference", "value": "fooSaAgentReference" }
           |  ]}
           |]}""".stripMargin)
      an[InsufficientEnrolments] shouldBe thrownBy {
        TestController.testWithAuthorisedSAAgent
      }
    }

    "throw an exception if agent is not logged in" in {
      givenUnauthorisedWith("MissingBearerToken")
      an[MissingBearerToken] shouldBe thrownBy {
        TestController.testWithAuthorisedSAAgent
      }
    }
  }
}
