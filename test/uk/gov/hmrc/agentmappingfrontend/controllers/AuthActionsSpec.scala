/*
 * Copyright 2017 HM Revenue & Customs
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

import org.scalatest.mock.MockitoSugar
import play.api.mvc._
import play.api.test.FakeRequest
import uk.gov.hmrc.agentmappingfrontend.audit.{AuditService, NoOpAuditService}
import uk.gov.hmrc.agentmappingfrontend.auth.{AgentRequest, TestAuthActions}
import uk.gov.hmrc.agentmappingfrontend.support.{TestPasscodeAuthenticationProvider, TestPasscodeVerificationConfig}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class AuthActionsSpec extends UnitSpec with MockitoSugar {

  "AuthorisedWithSubscribingAgent" should {
    "call the body when the user is whitelisted" in {
      val passcodeVerificationConfig = new TestPasscodeVerificationConfig(enabled = true)
      val passcodeAuthenticationProvider = new TestPasscodeAuthenticationProvider(passcodeVerificationConfig, whitelisted = true)
      val testAuthActions = new TestAuthActions(passcodeVerificationConfig, passcodeAuthenticationProvider)

      val result: Result = testAuthActions.AuthorisedSAAgent(NoOpAuditService)(okBody)(FakeRequest("GET", "/"))

      status(result) shouldBe 200
    }

    "prevent access when the user is not whitelisted" in {
      val passcodeVerificationConfig = new TestPasscodeVerificationConfig(enabled = true)
      val passcodeAuthenticationProvider = new TestPasscodeAuthenticationProvider(passcodeVerificationConfig, whitelisted = false)
      val testAuthActions = new TestAuthActions(passcodeVerificationConfig, passcodeAuthenticationProvider)

      val result: Result = testAuthActions.AuthorisedSAAgent(NoOpAuditService)(okBody)(FakeRequest("GET", "/"))

      status(result) shouldBe 303
      result.header.headers("Location") should endWith("/otac/login")
    }
  }

  private val okBody: AuthContext => AgentRequest[AnyContent] => Future[Result] = { authContext =>
    request =>
      Future(Results.Ok)
  }
}
