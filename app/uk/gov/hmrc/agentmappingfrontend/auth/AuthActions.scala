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
import uk.gov.hmrc.agentmappingfrontend.auth.EnrolmentHelper._
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.connectors.AgentSubscriptionConnector
import uk.gov.hmrc.agentmappingfrontend.controllers.routes
import uk.gov.hmrc.agentmappingfrontend.model._
import uk.gov.hmrc.agentmappingfrontend.repository.MappingResult.MappingArnResultId
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{agentCode, allEnrolments, credentials}
import uk.gov.hmrc.domain.AgentCode
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects

import scala.concurrent.{ExecutionContext, Future}

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
        case agentEnrolments ~ Some(Credentials(providerId, _)) =>
          val activeEnrolments = agentEnrolments.enrolments.filter(_.isActivated)

          val eligibleEnrolments: Set[Enrolment] = activeEnrolments.filter(LegacyAgentEnrolmentType.exists)

          if (eligibleEnrolments.nonEmpty) {
            body(providerId)
          } else {
            val redirectRoute = if (userHasAsAgentEnrolment(activeEnrolments)) {
              routes.MappingController.incorrectAccount(idRefToArn)
            } else if (userHasAtedAgentEnrolment(activeEnrolments)) {
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
              .getEnrolment(AsAgentServiceKey)
              .flatMap(_.getIdentifier(ArnEnrolmentKey)
                .map(identifier => Arn(identifier.value))))
      }
      .recoverWith {
        case _: JsResultException      => body(None)
        case _: AuthorisationException => body(None)
      }

}

class Agent(
  private val providerId: AuthProviderId,
  private val maybeAgentCode: Option[AgentCode],
  private val legacyEnrolments: Seq[AgentEnrolment],
  private val maybeSubscriptionJourneyRecord: Option[SubscriptionJourneyRecord]) {
  def authProviderId: AuthProviderId = providerId
  def agentEnrolments: Seq[AgentEnrolment] = legacyEnrolments
  def agentCodeOpt: Option[AgentCode] = maybeAgentCode

  def getMandatorySubscriptionJourneyRecord: SubscriptionJourneyRecord =
    maybeSubscriptionJourneyRecord
      .getOrElse(
        throw new RuntimeException(
          s"mandatory subscription journey record was missing for authProviderID $authProviderId"))
}

trait TaskListAuthActions extends AuthorisedFunctions with AuthRedirects {

  def env: Environment

  def appConfig: AppConfig

  def agentSubscriptionConnector: AgentSubscriptionConnector

  def withBasicAgentAuth[A](body: => Future[Result])(
    implicit request: Request[AnyContent],
    hc: HeaderCarrier,
    ec: ExecutionContext): Future[Result] =
    authorised(AuthProviders(GovernmentGateway) and AffinityGroup.Agent) {
      body
    } recover {
      case _: AuthorisationException => toGGLogin(s"${appConfig.authenticationLoginCallbackUrl}${request.uri}")
    }

  def withSubscribingAgent(id: MappingArnResultId)(body: Agent => Future[Result])(
    implicit request: Request[AnyContent],
    hc: HeaderCarrier,
    ec: ExecutionContext): Future[Result] =
    authorised(AuthProviders(GovernmentGateway) and AffinityGroup.Agent)
      .retrieve(credentials and agentCode and allEnrolments) {
        case Some(Credentials(providerId, _)) ~ agentCodeOpt ~ enrols =>
          val activeEnrolments: Set[Enrolment] = enrols.enrolments.filter(_.isActivated)

          if (userHasAsAgentEnrolment(activeEnrolments)) {
            Future.successful(Redirect(routes.TaskListMappingController.incorrectAccount(id)))
          } else if (userHasAtedAgentEnrolment(activeEnrolments)) {
            Future.successful(Redirect(routes.TaskListMappingController.alreadyMapped(id)))
          } else
            agentSubscriptionConnector.getSubscriptionJourneyRecord(AuthProviderId(providerId)).flatMap { maybeSjr =>
              val eligibleEnrolments: Set[Enrolment] = activeEnrolments.filter(LegacyAgentEnrolmentType.exists)
              body(new Agent(
                providerId = AuthProviderId(providerId),
                maybeAgentCode = agentCodeOpt.flatMap(ac => Some(AgentCode(ac))),
                legacyEnrolments = agentEnrolmentsFromEligibleEnrolments(eligibleEnrolments),
                maybeSjr
              ))
            }
      }
      .recover {
        case _: AuthorisationException => toGGLogin(s"${appConfig.authenticationLoginCallbackUrl}${request.uri}")
      }

  private def agentEnrolmentsFromEligibleEnrolments(eligibleEnrolments: Set[Enrolment]): Seq[AgentEnrolment] =
    eligibleEnrolments
      .map(enrolment =>
        LegacyAgentEnrolmentType.find(enrolment.key) match {
          case Some(legacyEnrolmentType) =>
            AgentEnrolment(legacyEnrolmentType, IdentifierValue(enrolment.identifiers.map(i => i.value).mkString("/")))
          case None => throw new RuntimeException("invalid enrolment type found")
      })
      .toSeq
}
