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

package uk.gov.hmrc.agentmappingfrontend.auth

import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.passcode.authentication.{PasscodeAuthenticationProvider, PasscodeVerificationConfig}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.test.UnitSpec

class AuthActionsSpec extends UnitSpec with AuthActions {
  "saAgentReference" should {
    "be case insensitive when finding the identifier" in {
      val usualCasedEnrolments = List(Enrolment("IR-SA-AGENT", Seq(Identifier("IRAgentReference", "test agent ref")), "Activated"))
      saAgentReference(usualCasedEnrolments) shouldBe Some(SaAgentReference("test agent ref"))

      val otherCasedEnrolments = List(Enrolment("IR-SA-AGENT", Seq(Identifier("IrAgEnTReference", "test agent ref 2")), "Activated"))
      saAgentReference(otherCasedEnrolments) shouldBe Some(SaAgentReference("test agent ref 2"))
    }
  }

  override protected def authConnector: AuthConnector = ???

  override def config: PasscodeVerificationConfig = ???

  override def passcodeAuthenticationProvider: PasscodeAuthenticationProvider = ???
}
