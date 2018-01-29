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
import play.api.{Configuration, Environment}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentmappingfrontend.auth.AuthActions
import uk.gov.hmrc.agentmappingfrontend.stubs.AuthStubs
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthActionsSpec extends BaseControllerISpec with AuthStubs {

  object TestController extends AuthActions {
    override def authConnector: AuthConnector = app.injector.instanceOf[AuthConnector]
    implicit val hc = HeaderCarrier()
    implicit val request = FakeRequest().withSession(SessionKeys.authToken -> "Bearer XYZ")

    val env = app.injector.instanceOf[Environment]
    val config = app.injector.instanceOf[Configuration]

    def testWithAuthorisedSAAgent = {
      await(withAuthorisedSAAgent(){request => Future.successful(Ok(request.saAgentReference.value))})
    }
  }

  "withAuthorisedSAAgent" should {

    "check if an agent is enrolled for IR-SA-AGENT and extract SaAgentReference" in {
      givenAuthorisedFor(
        "{}",
        s"""{
           |  "authorisedEnrolments": [
           |    { "key":"IR-SA-AGENT", "identifiers": [
           |      { "key":"IRAgentReference", "value": "fooSaAgentReference" }
           |    ]}
           |  ],
           |  "credentials": {
           |    "providerId": "12345-credId",
           |    "providerType": "GovernmentGateway"
           |  }}""".stripMargin)
      val result = TestController.testWithAuthorisedSAAgent
      status(result) shouldBe 200
      bodyOf(result) shouldBe "fooSaAgentReference"
    }

    "redirect to not-enrolled if an agent is not enrolled for IR-SA-AGENT" in {
      givenAuthorisedFor(
        "{}",
        s"""{
           |  "authorisedEnrolments": [
           |    { "key":"IR-FOO-AGENT", "identifiers": [
           |     { "key":"IRAgentReference", "value": "fooSaAgentReference" }
           |    ]}
           |  ],
           |  "credentials": {
           |    "providerId": "12345-credId",
           |    "providerType": "GovernmentGateway"
           |  }}""".stripMargin)
      val result = TestController.testWithAuthorisedSAAgent
      status(result) shouldBe 303
      result.header.headers(HeaderNames.LOCATION) shouldBe routes.MappingController.notEnrolled().url
    }

    "redirect to sign-in if an agent is not logged in" in {
      givenUnauthorisedWith("MissingBearerToken")
      val result = TestController.testWithAuthorisedSAAgent
      status(result) shouldBe 303
      result.header.headers(HeaderNames.LOCATION) shouldBe "/gg/sign-in?continue=%2F&origin=agent-mapping-frontend"
    }
  }
}
