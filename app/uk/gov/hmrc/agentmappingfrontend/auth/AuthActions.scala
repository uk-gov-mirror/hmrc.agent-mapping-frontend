/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.Configuration
import play.api.Environment
import play.api.mvc.Results._
import play.api.mvc._
import sttp.model.Uri.UriContext
import uk.gov.hmrc.agentmappingfrontend.auth.EnrolmentHelper._
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.connectors.AgentSubscriptionConnector
import uk.gov.hmrc.agentmappingfrontend.controllers.routes
import uk.gov.hmrc.agentmappingfrontend.model._
import uk.gov.hmrc.agentmappingfrontend.repository.MappingResult.MappingArnResultId
import uk.gov.hmrc.agentmappingfrontend.model.identifiers.Arn
import uk.gov.hmrc.agentmappingfrontend.util.RequestAwareLogging
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.agentCode
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.allEnrolments
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.credentials
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.domain.AgentCode
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

trait AuthActions
extends AuthorisedFunctions
with RequestAwareLogging {

  def env: Environment

  def appConfig: AppConfig

  def config: Configuration

  def agentSubscriptionConnector: AgentSubscriptionConnector

  def withBasicAuth(
    body: => Future[Result]
  )(implicit
    request: Request[AnyContent],
    ec: ExecutionContext
  ): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    authorised(AuthProviders(GovernmentGateway)) {
      body
    } recover {
      handleException
    }
  }

  def withAuthorisedSaAgent(idRefToArn: MappingArnResultId)(
    body: Enrolment => Future[Result]
  )(implicit
    request: Request[AnyContent],
    ec: ExecutionContext
  ): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    authorised(AuthProviders(GovernmentGateway))
      .retrieve(allEnrolments and credentials) {
        case _ ~ None => Future.successful(Forbidden)
        case agentEnrolments ~ Some(_) =>
          val activeEnrolments = agentEnrolments.enrolments.filter(_.isActivated)
          val saEnrolment = activeEnrolments.find(_.key == IRAgentReference.serviceKey)

          saEnrolment match {
            case Some(enrolment) => body(enrolment)
            case _ if userHasAsAgentEnrolment(activeEnrolments) => Future.successful(Redirect(routes.MappingController.incorrectAccount(idRefToArn)))
            case _ if userHasAtedAgentEnrolment(activeEnrolments) => Future.successful(Redirect(routes.MappingController.alreadyMapped(idRefToArn)))
            case _ => Future.successful(Redirect(routes.MappingController.notEnrolled(idRefToArn)))
          }
      }
      .recover {
        handleException
      }
  }

  def withAuthorisedAgent(idRefToArn: MappingArnResultId)(
    body: String => Future[Result]
  )(implicit
    request: Request[AnyContent],
    ec: ExecutionContext
  ): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    authorised(AuthProviders(GovernmentGateway))
      .retrieve(allEnrolments and credentials) {
        case _ ~ None => Future.successful(Forbidden)
        case agentEnrolments ~ Some(Credentials(providerId, _)) =>
          val activeEnrolments = agentEnrolments.enrolments.filter(_.isActivated)

          val eligibleEnrolments: Set[Enrolment] = activeEnrolments.filter(LegacyAgentEnrolmentType.exists)
          def redirectRoute: Call =
            if (userHasAsAgentEnrolment(activeEnrolments)) {
              routes.MappingController.incorrectAccount(idRefToArn)
            }
            else if (userHasAtedAgentEnrolment(activeEnrolments)) {
              routes.MappingController.alreadyMapped(idRefToArn)
            }
            else {
              routes.MappingController.notEnrolled(idRefToArn)
            }

          if (eligibleEnrolments.nonEmpty) {
            body(providerId)
          }
          else {
            Future.successful(Redirect(redirectRoute))
          }
      }
      .recover {
        handleException
      }
  }

  def withCheckForArn(
    body: Option[Arn] => Future[Result]
  )(implicit
    request: Request[AnyContent],
    ec: ExecutionContext
  ): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    authorised(AuthProviders(GovernmentGateway) and AffinityGroup.Agent)
      .retrieve(allEnrolments) { agentEnrolments =>
        val arn = getArn(agentEnrolments)
        body(arn)
      }
      .recoverWith {
        case _: NoActiveSession => body(None)

        case e => Future.successful(handleException.apply(e))
      }
  }

  def withBasicAgentAuth[A](
    body: => Future[Result]
  )(implicit
    request: Request[AnyContent],
    ec: ExecutionContext
  ): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    authorised(AuthProviders(GovernmentGateway) and AffinityGroup.Agent) {
      body
    } recover {
      handleException
    }
  }

  def withSubscribingAgent(
    id: MappingArnResultId
  )(body: Agent => Future[Result])(implicit
    request: Request[AnyContent],
    ec: ExecutionContext
  ): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    authorised(AuthProviders(GovernmentGateway) and AffinityGroup.Agent)
      .retrieve(credentials and agentCode and allEnrolments) {
        case Some(Credentials(providerId, _)) ~ agentCodeOpt ~ enrols =>
          val activeEnrolments: Set[Enrolment] = enrols.enrolments.filter(_.isActivated)
          val eligibleEnrolments: Set[Enrolment] = activeEnrolments.filter(LegacyAgentEnrolmentType.exists)

          if (eligibleEnrolments.nonEmpty) {
            agentSubscriptionConnector.getSubscriptionJourneyRecord(AuthProviderId(providerId)).flatMap { maybeSjr =>
              body(
                new Agent(
                  providerId = AuthProviderId(providerId),
                  maybeAgentCode = agentCodeOpt.flatMap(ac => Some(AgentCode(ac))),
                  legacyEnrolments = agentEnrolmentsFromEligibleEnrolments(eligibleEnrolments),
                  maybeSjr
                )
              )
            }
          }
          else {
            val redirectRoute =
              if (userHasAsAgentEnrolment(activeEnrolments)) {
                routes.TaskListMappingController.incorrectAccount(id)
              }
              else if (userHasAtedAgentEnrolment(activeEnrolments)) {
                routes.TaskListMappingController.alreadyMapped(id)
              }
              else {
                routes.TaskListMappingController.notEnrolled(id)
              }
            Future.successful(Redirect(redirectRoute))
          }
        case ~(~(None, _), _) => Future.successful(Forbidden)
      }
      .recover {
        handleException
      }
  }

  private def agentEnrolmentsFromEligibleEnrolments(eligibleEnrolments: Set[Enrolment]): Seq[AgentEnrolment] =
    eligibleEnrolments
      .map(enrolment =>
        LegacyAgentEnrolmentType.find(enrolment.key) match {
          case Some(legacyEnrolmentType) => AgentEnrolment(legacyEnrolmentType, IdentifierValue(enrolment.identifiers.map(i => i.value).mkString("/")))
          case None => throw new RuntimeException("invalid enrolment type found")
        }
      )
      .toSeq

  private def getArn(enrolments: Enrolments) =
    for {
      enrolment <- enrolments.getEnrolment(AsAgentServiceKey)
      identifier <- enrolment.getIdentifier(ArnEnrolmentKey)
    } yield Arn(identifier.value)

  private def handleException(implicit request: Request[_]): PartialFunction[Throwable, Result] = {

    case _: UnsupportedAffinityGroup =>
      logger.warn(s"Logged in user does not have the required affinity group")
      Forbidden

    case _: NoActiveSession =>
      val continueUrl = uri"${continueBaseUrl + request.uri}"
      val params = List(
        "continue_url" -> continueUrl,
        "origin" -> appName
      )
      val url = uri"""${basGatewayFrontendExternalUrl + signInUrl}?${params}"""
      Redirect(url.toString)

    case _: InsufficientEnrolments =>
      logger.warn(s"Logged in user does not have required enrolments")
      Forbidden

    case _: UnsupportedAuthProvider =>
      logger.warn("User is not logged in via  GovernmentGateway, signing out and redirecting")
      val continueUrl = uri"${continueBaseUrl + request.uri}"
      val params = List(
        "continue_url" -> continueUrl
      )
      val url = uri"""${basGatewayFrontendExternalUrl + signInUrl}?${params}"""
      Redirect(url.toString)
  }

  private val basGatewayFrontendExternalUrl = getString("microservice.services.bas-gateway-frontend.external-url")
  private val signInUrl = getString("microservice.services.bas-gateway-frontend.sign-in.path")
  private val continueBaseUrl = getString("microservice.services.agent-mapping-frontend.external-url")
  private val appName = getString("appName")

  private def getString(key: String): String = config.underlying.getString(key)

}

class Agent(
  private val providerId: AuthProviderId,
  private val maybeAgentCode: Option[AgentCode],
  private val legacyEnrolments: Seq[AgentEnrolment],
  private val maybeSubscriptionJourneyRecord: Option[SubscriptionJourneyRecord]
) {

  def authProviderId: AuthProviderId = providerId
  def agentEnrolments: Seq[AgentEnrolment] = legacyEnrolments
  def agentCodeOpt: Option[AgentCode] = maybeAgentCode

  def getMandatorySubscriptionJourneyRecord: SubscriptionJourneyRecord = maybeSubscriptionJourneyRecord
    .getOrElse(
      throw new RuntimeException(
        s"mandatory subscription journey record was missing for authProviderID $authProviderId"
      )
    )

}
