/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.Environment
import play.api.libs.json.JsResultException
import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.controllers.routes
import uk.gov.hmrc.agentmappingfrontend.model.Names._
import uk.gov.hmrc.agentmappingfrontend.repository.MappingArnResult.MappingArnResultId
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.Retrievals.{allEnrolments, credentials}
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects

import scala.concurrent.{ExecutionContext, Future}

object Auth {

  val validEnrolments: Set[String] = Set(
    `IR-SA-AGENT`, //IRAgentReference
    `HMCE-VAT-AGNT`, //AgentRefNo
    `HMRC-CHAR-AGENT`, //AGENTCHARID
    `HMRC-GTS-AGNT`, //HMRCGTSAGENTREF
    `HMRC-MGD-AGNT`, //HMRCMGDAGENTREF
    `HMRC-NOVRN-AGNT`, //VATAgentRefNo
    `IR-CT-AGENT`, //IRAgentReference
    `IR-PAYE-AGENT`, //IRAgentReference
    `IR-SDLT-AGENT` //STORN
  )

}

trait AuthActions extends AuthorisedFunctions with AuthRedirects {

  def env: Environment

  def appConfig: AppConfig

  def withBasicAuth[A](body: => Future[Result])(
    implicit request: Request[AnyContent],
    hc: HeaderCarrier,
    ec: ExecutionContext): Future[Result] =
    authorised(AuthProviders(GovernmentGateway)) {
      body
    } recover {
      case _: AuthorisationException => toGGLogin(s"${appConfig.authenticationLoginCallbackUrl}${request.uri}")
    }

  def withAuthorisedAgent(idRefToArn: MappingArnResultId)(body: String => Future[Result])(
    implicit request: Request[AnyContent],
    hc: HeaderCarrier,
    ec: ExecutionContext): Future[Result] =
    authorised(AuthProviders(GovernmentGateway))
      .retrieve(allEnrolments and credentials) {
        case agentEnrolments ~ creds =>
          val activeEnrolments = agentEnrolments.enrolments.filter(_.isActivated).map(_.key)
          val hasEligibleAgentEnrolments = activeEnrolments.intersect(Auth.validEnrolments).nonEmpty

          if (hasEligibleAgentEnrolments) {
            body(creds.providerId)
          } else {
            val redirectRoute = if (activeEnrolments.contains(`HMRC-AS-AGENT`)) {
              routes.MappingController.incorrectAccount(idRefToArn)
            } else if (activeEnrolments.contains(`HMRC-AGENT-AGENT`)) {
              routes.MappingController.alreadyMapped(idRefToArn)
            } else {
              routes.MappingController.notEnrolled(idRefToArn)
            }

            Future.successful(Redirect(redirectRoute))
          }
      }
      .recover {
        case _: AuthorisationException => toGGLogin(s"${appConfig.authenticationLoginCallbackUrl}${request.uri}")
      }

  def withCheckForArn(body: Option[Arn] => Future[Result])(
    implicit request: Request[AnyContent],
    hc: HeaderCarrier,
    ec: ExecutionContext): Future[Result] =
    authorised(AuthProviders(GovernmentGateway) and AffinityGroup.Agent)
      .retrieve(allEnrolments) {
        case agentEnrolments =>
          body(
            agentEnrolments
              .getEnrolment("HMRC-AS-AGENT")
              .flatMap(_.getIdentifier("AgentReferenceNumber")
                .map(identifier => Arn(identifier.value))))
      }
      .recoverWith {
        case _: JsResultException      => body(None)
        case _: AuthorisationException => body(None)
      }
}
