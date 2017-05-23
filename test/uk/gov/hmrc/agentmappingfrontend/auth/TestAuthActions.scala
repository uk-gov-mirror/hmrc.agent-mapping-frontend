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

import org.scalatest.mock.MockitoSugar
import play.api.mvc._
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.passcode.authentication.{PasscodeAuthenticationProvider, PasscodeVerificationConfig}
import uk.gov.hmrc.play.frontend.auth._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http._

import scala.concurrent.Future


class TestAuthActions(override val config: PasscodeVerificationConfig,
                      override val passcodeAuthenticationProvider: PasscodeAuthenticationProvider)
  extends AuthActions with MockitoSugar {

  override def authConnector: AuthConnector = ???

  override def AuthorisedFor(taxRegime: TaxRegime, pageVisibility: PageVisibilityPredicate): AuthenticatedBy = NoCheckAuthenticatedBy

  object NoCheckAuthenticatedBy extends AuthenticatedBy(null, None, null) {
    override def apply(body: AuthContext => (Request[AnyContent] => Result)): Action[AnyContent] = Action { implicit request =>
      body(authContext)(request)
    }

    override def async(body: AuthContext => (Request[AnyContent] => Future[Result])): Action[AnyContent] = Action.async { implicit request =>
      body(authContext)(request)
    }
  }

  private val authContext: AuthContext = null

  override protected def isAgentAffinityGroup()(implicit authContext: AuthContext, hc: HeaderCarrier): Future[Boolean] =
    Future successful true

  override protected def enrolments(implicit authContext: AuthContext, hc: HeaderCarrier): Future[List[Enrolment]] =
    Future successful List.empty

  override private[auth] def saAgentReference(e: List[Enrolment]): Option[SaAgentReference] =
    Some(SaAgentReference(""))
}
